package com.crossposter.services;

public class ServiceRegistry {
    private static BlueskyClient blueskyClient = new BlueskyClient();
    private static AuthSession blueskySession;
    private static String blueskyPdsOrigin;

    public static BlueskyClient getBlueskyClient() {
        return blueskyClient;
    }

    public static AuthSession getBlueskySession() {
        return blueskySession;
    }

    public static void setBlueskySession(AuthSession session) {
        blueskySession = session;
    }

    public static String getBlueskyPdsOrigin() {
        return blueskyPdsOrigin;
    }

    public static void setBlueskyPdsOrigin(String pdsOrigin) {
        blueskyPdsOrigin = pdsOrigin;
    }
}