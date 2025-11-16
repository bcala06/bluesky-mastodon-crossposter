package com.crossposter.services;

import com.crossposter.utils.DPoPUtil;
import com.crossposter.utils.HttpUtil;
import com.crossposter.utils.LocalCallbackServer;
import com.crossposter.utils.PkceUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class BlueskyClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CLIENT_ID = "https://bcala06.github.io/bluesky-mastodon-crossposter/client-metadata.json";
    private static final String REDIRECT_URI = "http://127.0.0.1:8080/callback";
    private static final String SCOPE = "atproto repo:app.bsky.feed.post?action=create";

    private record TokenClaims(String did, String pdsEndpoint) {}

    public AuthSession startAuth(String pdsOrigin) throws Exception {
        String metaUrl = pdsOrigin + "/.well-known/oauth-authorization-server";
        String metaBody = HttpUtil.get(metaUrl, Map.of("Accept", "application/json"));
        Map<String, Object> meta = MAPPER.readValue(metaBody, Map.class);

        String parEndpoint = (String) meta.get("pushed_authorization_request_endpoint");
        String authEndpoint = (String) meta.get("authorization_endpoint");
        String tokenEndpoint = (String) meta.get("token_endpoint");

        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier);
        DPoPUtil.init();
        AuthSession session = new AuthSession(codeVerifier);

        String state = UUID.randomUUID().toString();
        String parBody = String.format(
                "client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s&code_challenge=%s&code_challenge_method=S256",
                urlenc(CLIENT_ID), urlenc(REDIRECT_URI), urlenc(SCOPE), urlenc(state), urlenc(codeChallenge), urlenc("S256")
        );

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        String dpop1 = DPoPUtil.buildDPoP("POST", parEndpoint, session.dpopNonce, null);
        headers.put("DPoP", dpop1);
        HttpResponse<String> parResponse = HttpUtil.postFormWithResponse(parEndpoint, headers, parBody);

        int statusCode = parResponse.statusCode();
        boolean needsRetry = (statusCode == 401) || (statusCode == 400 && parResponse.body().contains("\"use_dpop_nonce\""));

        if (needsRetry) {
            String nonce = HttpUtil.extractDpopNonce(parResponse);
            if (nonce != null && !nonce.isEmpty()) {
                session.dpopNonce = nonce;
                String dpop2 = DPoPUtil.buildDPoP("POST", parEndpoint, session.dpopNonce, null);
                headers.put("DPoP", dpop2);
                parResponse = HttpUtil.postFormWithResponse(parEndpoint, headers, parBody);
            }
        }

        String newNonce = HttpUtil.extractDpopNonce(parResponse);
        if (newNonce != null) {
            session.dpopNonce = newNonce;
        }

        if (parResponse.statusCode() != 200 && parResponse.statusCode() != 201) {
            throw new IOException("PAR failed with status " + parResponse.statusCode() + ": " + parResponse.body());
        }

        Map<String, Object> parJson = MAPPER.readValue(parResponse.body(), Map.class);
        String requestUri = (String) parJson.get("request_uri");
        if (requestUri == null || requestUri.trim().isEmpty()) {
            throw new IOException("PAR failed, no request_uri returned: " + parResponse.body());
        }

        String authUrl = authEndpoint
                + "?client_id=" + urlenc(CLIENT_ID)
                + "&request_uri=" + urlenc(requestUri)
                + "&state=" + urlenc(state);

        LocalCallbackServer callbackServer = new LocalCallbackServer();
        try {
            callbackServer.start();
            waitForLocalServer("127.0.0.1", 8080, 2000);

            // Open system browser. If Desktop fails, print URL so caller can open manually.
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(authUrl));
                } else {
                    System.out.println("Open this URL in your browser: " + authUrl);
                }
            } catch (Exception e) {
                System.out.println("Failed to open system browser: " + e.getMessage());
                System.out.println("Open this URL in your browser: " + authUrl);
            }

            // Wait for callback
            LocalCallbackServer.CallbackResult cb = callbackServer.awaitAuthorizationCode(180);
            if (cb == null) throw new IOException("Timeout waiting for callback");
            if (!state.equals(cb.state())) throw new IOException("State mismatch");
            String code = cb.code();

            if (cb.iss() == null || cb.iss().isBlank()) {
                throw new IOException("Authorization server did not return an issuer URL ('iss').");
            }
            session.issuer = cb.iss();

            // Token exchange
            String tokenBody = String.format(
                    "grant_type=authorization_code&code=%s&redirect_uri=%s&code_verifier=%s&client_id=%s",
                    urlenc(code), urlenc(REDIRECT_URI), urlenc(codeVerifier), urlenc(CLIENT_ID)
            );

            headers.clear();
            headers.put("Content-Type", "application/x-www-form-urlencoded");

            String tokenDpop = DPoPUtil.buildDPoP("POST", tokenEndpoint, session.dpopNonce, null);
            headers.put("DPoP", tokenDpop);
            HttpResponse<String> tokenResponse = HttpUtil.postFormWithResponse(tokenEndpoint, headers, tokenBody);

            newNonce = HttpUtil.extractDpopNonce(tokenResponse);
            if (newNonce != null) {
                session.dpopNonce = newNonce;
            }

            if (tokenResponse.statusCode() != 200) {
                throw new IOException("Token exchange failed: " + tokenResponse.statusCode() + " " + tokenResponse.body());
            }

            Map<String, Object> tokenJson = MAPPER.readValue(tokenResponse.body(), Map.class);
            String accessToken = (String) tokenJson.get("access_token");
            String refreshToken = (String) tokenJson.get("refresh_token");

            // Parse token claims
            TokenClaims claims = parseTokenClaims(accessToken);
            String did = claims.did();
            String pdsEndpoint = claims.pdsEndpoint();

            // Check for errors
            if (accessToken == null) {
                throw new IOException("Token exchange response missing access_token: " + tokenResponse.body());
            }

            // Supply session tokens and handle
            session.accessToken = accessToken;
            session.refreshToken = refreshToken;
            session.did = did;
            session.pdsEndpoint = pdsEndpoint;
            session.handle = getHandle(session);

            return session;
        } finally {
            try {
                callbackServer.stop();
            } catch (Exception ignored) {}
        }
    }

    private void refreshAccessToken(AuthSession session, String pdsOrigin) throws Exception {
        System.out.println("Access token expired. Refreshing...");
        String metaUrl = pdsOrigin + "/.well-known/oauth-authorization-server";
        String metaBody = HttpUtil.get(metaUrl, Map.of("Accept", "application/json"));
        Map<String, Object> meta = MAPPER.readValue(metaBody, Map.class);
        String tokenEndpoint = (String) meta.get("token_endpoint");

        if (session.refreshToken == null) {
            throw new IOException("No refresh token available. Please log in again.");
        }

        String refreshBody = String.format(
                "grant_type=refresh_token&refresh_token=%s&client_id=%s",
                urlenc(session.refreshToken), urlenc(CLIENT_ID)
        );

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");

        String dpop = DPoPUtil.buildDPoP("POST", tokenEndpoint, session.dpopNonce, null);
        headers.put("DPoP", dpop);
        HttpResponse<String> response = HttpUtil.postFormWithResponse(tokenEndpoint, headers, refreshBody);

        boolean needsRetry = (response.statusCode() == 401) || (response.statusCode() == 400 && response.body().contains("use_dpop_nonce"));
        if (needsRetry) {
            String nonce = HttpUtil.extractDpopNonce(response);
            if (nonce != null && !nonce.isEmpty()) {
                session.dpopNonce = nonce;
                String dpop2 = DPoPUtil.buildDPoP("POST", tokenEndpoint, session.dpopNonce, null);
                headers.put("DPoP", dpop2);
                response = HttpUtil.postFormWithResponse(tokenEndpoint, headers, refreshBody);
            }
        }

        String newNonce = HttpUtil.extractDpopNonce(response);
        if (newNonce != null) {
            session.dpopNonce = newNonce;
        }

        if (response.statusCode() != 200) {
            throw new IOException("Token refresh failed: " + response.statusCode() + " " + response.body());
        }

        Map<String, Object> tokenJson = MAPPER.readValue(response.body(), Map.class);
        session.accessToken = (String) tokenJson.get("access_token");
        session.refreshToken = (String) tokenJson.get("refresh_token");

        TokenClaims claims = parseTokenClaims(session.accessToken);
        session.did = claims.did();
        session.pdsEndpoint = claims.pdsEndpoint();
        System.out.println("Token successfully refreshed.");
    }

    public Map<String, Object> createPost(AuthSession session, String pdsOrigin, String text) throws Exception {
        try {
            return attemptToCreatePost(session, pdsOrigin, text);
        } catch (IOException e) {
            String errorMessage = e.getMessage().toLowerCase();
            if (errorMessage.contains("invalidtoken") || errorMessage.contains("status: 401") || errorMessage.contains("ath mismatch")) {
                System.out.println("Initial post failed due to token error. Attempting refresh...");
                refreshAccessToken(session, session.issuer);
                return attemptToCreatePost(session, session.pdsEndpoint, text);
            } else {
                throw e;
            }
        }
    }

    public String getHandle(AuthSession session) throws Exception {
        if (session.accessToken == null) {
            throw new IllegalStateException("Warning: Session access token not found.");
        }
        if (session.did == null || session.did.isBlank()) {
            throw new IllegalStateException("Warning: Session DID not found.");
        }

        String handleEndpoint = "https://public.api.bsky.app/xrpc/app.bsky.actor.getProfile?actor=" + session.did;
        Map<String, String> headers = Map.of("Authorization", "Bearer " + session.accessToken);
        HttpResponse<String> getResponse = HttpUtil.getWithResponse(handleEndpoint, headers);

        if (getResponse.statusCode() != 200)
            throw new IOException("getProfile failed: " + getResponse.statusCode() + " " + getResponse.body());

        Map<String, Object> outer = MAPPER.readValue(getResponse.body(), Map.class);
        String handle = (String) outer.get("handle");

        return handle;
    }

    private Map<String, Object> attemptToCreatePost(AuthSession session, String pdsOrigin, String text) throws Exception {
        if (session.did == null || session.did.isBlank()) {
            throw new IllegalStateException("AuthSession has no DID. Make sure to set it after login.");
        }

        String httpPdsUrl = pdsOrigin.startsWith("did:web:") ? "https://" + pdsOrigin.substring(8) : pdsOrigin;
        String url = httpPdsUrl + "/xrpc/com.atproto.repo.createRecord";

        Map<String, Object> record = Map.of(
                "text", text,
                "$type", "app.bsky.feed.post",
                "createdAt", Instant.now().toString()
        );

        Map<String, Object> body = new HashMap<>();
        body.put("repo", session.did);
        body.put("collection", "app.bsky.feed.post");
        body.put("record", record);

        String jsonBody = MAPPER.writeValueAsString(body);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "DPoP " + session.accessToken);
        headers.put("Content-Type", "application/json");

        String dpop = DPoPUtil.buildDPoP("POST", url, session.dpopNonce, session.accessToken);
        headers.put("DPoP", dpop);
        HttpResponse<String> postResponse = HttpUtil.postFormWithResponse(url, headers, jsonBody);

        boolean needsRetry = (postResponse.statusCode() == 401) || (postResponse.statusCode() == 400 && postResponse.body().contains("use_dpop_nonce"));
        if (needsRetry) {
            System.out.println("DPoP nonce mismatch on createPost. Retrying...");
            String nonce = HttpUtil.extractDpopNonce(postResponse);
            if (nonce != null && !nonce.isEmpty()) {
                session.dpopNonce = nonce;
                String dpop2 = DPoPUtil.buildDPoP("POST", url, session.dpopNonce, session.accessToken);
                headers.put("DPoP", dpop2);
                postResponse = HttpUtil.postFormWithResponse(url, headers, jsonBody);
            }
        }

        String finalNonce = HttpUtil.extractDpopNonce(postResponse);
        if (finalNonce != null) {
            session.dpopNonce = finalNonce;
        }

        if (postResponse.statusCode() != 200) {
            throw new IOException("Failed to create post. Status: " + postResponse.statusCode() + ", Response: " + postResponse.body());
        }

        return MAPPER.readValue(postResponse.body(), Map.class);
    }

    private TokenClaims parseTokenClaims(String accessToken) throws IOException {
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT format");
            }
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = MAPPER.readValue(payload, Map.class);

            String did = (String) claims.get("sub");
            String pdsEndpoint = (String) claims.get("aud");

            if (did == null || !did.startsWith("did:")) {
                throw new IOException("Could not extract DID ('sub') from access token");
            }
            if (pdsEndpoint == null || pdsEndpoint.isBlank()) {
                throw new IOException("Could not extract PDS endpoint ('aud') from access token");
            }

            return new TokenClaims(did, pdsEndpoint);
        } catch (Exception e) {
            throw new IOException("Failed to parse access token claims: " + e.getMessage(), e);
        }
    }

    private static void waitForLocalServer(String host, int port, int maxMillis) {
        long deadline = System.currentTimeMillis() + maxMillis;
        while (System.currentTimeMillis() < deadline) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 250);
                return;
            } catch (IOException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static String urlenc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}