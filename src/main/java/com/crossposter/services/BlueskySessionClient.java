package com.crossposter.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class BlueskySessionClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private String accessJwt;
    private String refreshJwt;
    private String did;

    public boolean login(String identifier, String appPassword) {
        try {
            String body = """
                {
                  "identifier": "%s",
                  "password": "%s"
                }
                """.formatted(identifier, appPassword);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://bsky.social/xrpc/com.atproto.server.createSession"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                System.err.println("Login failed: " + resp.body());
                return false;
            }

            JSONObject json = new JSONObject(resp.body());
            this.accessJwt = json.getString("accessJwt");
            this.refreshJwt = json.getString("refreshJwt");
            this.did = json.getString("did");

            System.out.println("Logged in as DID: " + did);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getProfile() throws Exception {
        if (accessJwt == null) throw new IllegalStateException("Not authenticated");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://bsky.social/xrpc/app.bsky.actor.getProfile?actor=" + did))
                .header("Authorization", "Bearer " + accessJwt)
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    public boolean createPost(String text) {
        try {
            if (accessJwt == null) throw new IllegalStateException("Not authenticated");

            long now = System.currentTimeMillis();
            String body = """
                {
                  "collection": "app.bsky.feed.post",
                  "repo": "%s",
                  "record": {
                    "text": "%s",
                    "createdAt": "%s"
                  }
                }
                """.formatted(did, text, java.time.Instant.ofEpochMilli(now));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://bsky.social/xrpc/com.atproto.repo.createRecord"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessJwt)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                System.out.println("Post created: " + resp.body());
                return true;
            } else {
                System.err.println("Post failed: " + resp.body());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getDid() {
        return did;
    }
}
