package com.brandonitaly.bedrockskins.client;

public final class BedrockSkinsLog {
    private BedrockSkinsLog() {}

    public static void error(String message, Throwable throwable) {
        System.out.println(message + ": " + throwable);
    }
}
