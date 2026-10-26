package com.vynk.notifications.controllers;

import java.time.Instant;

public final class NotificationApi {
    private NotificationApi() {}
    public record Item(long id, String type, String actorUsername, Instant createdAt, Instant readAt) {}
}
