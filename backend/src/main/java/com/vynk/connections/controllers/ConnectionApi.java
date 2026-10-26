package com.vynk.connections.controllers;

import java.time.Instant;

public final class ConnectionApi {
    private ConnectionApi() {}
    public record Request(long id, String username, Instant createdAt) {}
}
