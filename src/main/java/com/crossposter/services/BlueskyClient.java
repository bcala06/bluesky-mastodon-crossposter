// BlueskyClient.java

package com.crossposter.services;

import com.crossposter.utils.DPoPUtil;
import com.crossposter.utils.HttpUtil;
import com.crossposter.utils.LocalCallbackServer;
import com.crossposter.utils.PkceUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlueskyClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CLIENT_ID = "https://bcala06.github.io/bluesky-mastodon-crossposter/client-metadata.json";
    private static final String REDIRECT_URI = "http://127.0.0.1:8080/callback";
    private static final String SCOPE = "atproto";

    public AuthSession startAuth(String pdsOrigin) throws Exception {
        // Discover authorization server metadata
        String metaUrl = pdsOrigin + "/.well-known/oauth-authorization-server";
        String metaBody = HttpUtil.get(metaUrl, Map.of("Accept", "application/json"));
        Map<String, Object> meta = MAPPER.readValue(metaBody, Map.class);

        String parEndpoint = (String) meta.get("pushed_authorization_request_endpoint");
        String authEndpoint = (String) meta.get("authorization_endpoint");
        String tokenEndpoint = (String) meta.get("token_endpoint");
        String issuer = (String) meta.get("issuer");

        // PKCE setup
        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier);

        // Init DPoP keypair
        DPoPUtil.init();
        AuthSession session = new AuthSession(codeVerifier);
        session.issuer = issuer;

        // Build PAR request
        String state = UUID.randomUUID().toString();
        Map<String, String> parParams = Map.of(
                "response_type", "code",
                "client_id", CLIENT_ID,
                "redirect_uri", REDIRECT_URI,
                "scope", SCOPE,
                "code_challenge", codeChallenge,
                "code_challenge_method", "S256",
                "state", state,
                "iss", pdsOrigin
        );

        String parBody = HttpUtil.formEncode(parParams);
        String dpop = DPoPUtil.buildDPoP("POST", parEndpoint, session.dpopNonce);
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("DPoP", dpop);
        
        var parResponse = HttpUtil.postFormWithResponse(parEndpoint, headers, parBody);
        
        // Update DPoP nonce if provided in response
        String newNonce = HttpUtil.extractDpopNonce(parResponse);
        if (newNonce != null) {
            session.dpopNonce = newNonce;
        }

        Map<String, Object> parJson = MAPPER.readValue(parResponse.body(), Map.class);
        String requestUri = (String) parJson.get("request_uri");
        if (requestUri == null) throw new IOException("PAR failed, no request_uri");

        // Open system browser to authorization URL
        String authUrl = authEndpoint
                + "?client_id=" + urlenc(CLIENT_ID)
                + "&request_uri=" + urlenc(requestUri)
                + "&state=" + urlenc(state);

        LocalCallbackServer callbackServer = new LocalCallbackServer();
        callbackServer.start();
        Desktop.getDesktop().browse(URI.create(authUrl));

        LocalCallbackServer.CallbackResult cb = callbackServer.awaitAuthorizationCode(180);
        callbackServer.stop(); // Clean up the server
        if (cb == null) throw new IOException("Timeout waiting for callback");
        if (!state.equals(cb.state())) throw new IOException("State mismatch");
        String code = cb.code();

        // Token exchange
        Map<String, String> tokenParams = Map.of(
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", REDIRECT_URI,
                "code_verifier", codeVerifier,
                "client_id", CLIENT_ID
        );
        String tokenBody = HttpUtil.formEncode(tokenParams);
        String tokenDpop = DPoPUtil.buildDPoP("POST", tokenEndpoint, session.dpopNonce);

        headers.clear();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("DPoP", tokenDpop);

        var tokenResponse = HttpUtil.postFormWithResponse(tokenEndpoint, headers, tokenBody);
        
        // Update DPoP nonce if provided in response
        newNonce = HttpUtil.extractDpopNonce(tokenResponse);
        if (newNonce != null) {
            session.dpopNonce = newNonce;
        }

        Map<String, Object> tokenJson = MAPPER.readValue(tokenResponse.body(), Map.class);
        session.accessToken = (String) tokenJson.get("access_token");
        session.refreshToken = (String) tokenJson.get("refresh_token");

        // Get the user's DID using the session information
        session.did = getSession(session, pdsOrigin).get("did").toString();

        return session;
    }

    private Map<String, Object> getSession(AuthSession session, String pdsOrigin) throws Exception {
        String url = pdsOrigin + "/xrpc/com.atproto.session.get";
        
        String dpop = DPoPUtil.buildDPoP("GET", url, session.dpopNonce);

        Map<String, String> headers = Map.of(
                "Authorization", "DPoP " + session.accessToken,
                "DPoP", dpop
        );

        var response = HttpUtil.getWithResponse(url, headers);
        
        // Update DPoP nonce if provided in response
        String newNonce = HttpUtil.extractDpopNonce(response);
        if (newNonce != null) {
            // You might want to update the session nonce here if needed
            // session.dpopNonce = newNonce;
        }
        
        return MAPPER.readValue(response.body(), Map.class);
    }

    private static String urlenc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    public Map<String, Object> createPost(AuthSession session, String pdsOrigin, String text) throws Exception {
        if (session.did == null || session.did.isBlank()) {
            throw new IllegalStateException("AuthSession has no DID. Make sure to set it after login.");
        }

        String url = pdsOrigin + "/xrpc/com.atproto.repo.createRecord";

        // Build record payload
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

        // DPoP proof for this request
        String dpop = DPoPUtil.buildDPoP("POST", url, session.dpopNonce);

        Map<String, String> headers = Map.of(
                "Authorization", "DPoP " + session.accessToken,
                "DPoP", dpop,
                "Content-Type", "application/json"
        );

        var response = HttpUtil.getWithResponse(url, headers); // This should be postFormWithResponse
        
        // Actually, let's fix this - it should be a POST request
        var postResponse = HttpUtil.postFormWithResponse(url, headers, jsonBody);
        
        // Update DPoP nonce if provided in response
        String newNonce = HttpUtil.extractDpopNonce(postResponse);
        if (newNonce != null) {
            session.dpopNonce = newNonce;
        }

        return MAPPER.readValue(postResponse.body(), Map.class);
    }
}