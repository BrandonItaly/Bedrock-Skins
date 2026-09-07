package io.github.brandonitaly.bedrockskins.client;

import java.net.http.HttpClient;

/** Shared HTTP client for Bedrock Skins' account and remote texture requests. */
public final class ContentManager {
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private ContentManager() {}
}
