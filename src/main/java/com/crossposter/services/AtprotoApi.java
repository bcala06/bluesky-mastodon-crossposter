package com.crossposter.services;

import com.github.scribejava.core.builder.api.DefaultApi20;

public class AtprotoApi extends DefaultApi20 {
    private String parEndpoint;
    private String authzEndpoint;
    private String tokenEndpoint;

    public AtprotoApi(String parEndpoint, String authzEndpoint, String tokenEndpoint) {
        this.parEndpoint = parEndpoint;
        this.authzEndpoint = authzEndpoint;
        this.tokenEndpoint = tokenEndpoint;
    }

    @Override
    public String getAccessTokenEndpoint() {
        return tokenEndpoint;
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return authzEndpoint;
    }

    // Optionally override to support forcing PKCE, additional parameters, etc.
}
