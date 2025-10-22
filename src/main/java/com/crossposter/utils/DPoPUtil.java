package com.crossposter.utils;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public final class DPoPUtil {
    private static ECKey privateECKey;
    private static ECKey publicJWK;
    private DPoPUtil() {}

    public static synchronized void init() {
        if (privateECKey != null) return;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(256); // P-256
            KeyPair kp = kpg.generateKeyPair();

            ECPublicKey pub = (ECPublicKey) kp.getPublic();
            ECPrivateKey priv = (ECPrivateKey) kp.getPrivate();

            privateECKey = new ECKey.Builder(Curve.P_256, pub).privateKey(priv).build();
            publicJWK = privateECKey.toPublicJWK();

            System.out.println("DPoP keypair generated");
        } catch (Exception e) {
            throw new RuntimeException("DPoP init failed", e);
        }
    }

    /**
     * Build a DPoP proof for the given HTTP method and URL.
     * @param method HTTP verb (e.g. POST, GET)
     * @param url exact URL (e.g. https://bsky.social/oauth/par)
     * @param nonce optional nonce (if provided by server on 401)
     * @param accessToken optional access token to bind the proof to (ath claim)
     */
    public static String buildDPoP(String method, String url, String nonce, String accessToken) { // change for bluesky poster
        try {
            if (privateECKey == null) init();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType("dpop+jwt"))
                    .jwk(publicJWK)
                    .build();

            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(Instant.now()))
                    .claim("htm", method.toUpperCase())
                    .claim("htu", new URI(url).toString());

            if (nonce != null && !nonce.isEmpty()) {
                claims.claim("nonce", nonce);
            }

            // change for bluesky poster: Add the 'ath' claim if an access token is provided
            if (accessToken != null && !accessToken.isEmpty()) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(accessToken.getBytes(StandardCharsets.US_ASCII));
                String ath = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
                claims.claim("ath", ath);
            }

            SignedJWT jwt = new SignedJWT(header, claims.build());
            ECDSASigner signer = new ECDSASigner(privateECKey);
            jwt.sign(signer);

            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error creating DPoP proof", e);
        }
    }

    public static ECKey getPublicJWK() {
        if (publicJWK == null) init();
        return publicJWK;
    }
}