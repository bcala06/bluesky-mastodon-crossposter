package com.crossposter.services;

public class ServiceRegistry {
    private static final BlueskySessionClient blueskyClient = new BlueskySessionClient();

    public static BlueskySessionClient getBlueskyClient() {
        return blueskyClient;
    }
}
