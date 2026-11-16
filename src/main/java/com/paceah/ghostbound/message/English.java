package com.paceah.ghostbound.message;
import java.util.Map;
public final class English {
    private static final Map<String,String> TEXT = Map.ofEntries(
        Map.entry("request.incoming", "Spectator %s requests to teleport to you"),
        Map.entry("request.sent", "Teleport request sent to %s"),
        Map.entry("request.accept", "Accept"),
        Map.entry("request.deny", "Deny"),
        Map.entry("request.accepted", "Player %s accepted your spectator teleport request"),
        Map.entry("request.denied", "Player %s denied your spectator teleport request"),
        Map.entry("request.expired", "Request to %s expired"),
        Map.entry("request.cooldown", "You can request %s again in %s"),
        Map.entry("request.pending", "A request to this player is already pending"),
        Map.entry("request.no_pending", "No active requests to accept"),
        Map.entry("request.declined", "Request from %s declined"),
        Map.entry("request.completed", "Spectator %s teleported to you"),
        Map.entry("error.not_spectator", "Only spectators can send teleport requests"),
        Map.entry("error.target_spectator", "You cannot request another spectator"),
        Map.entry("error.target_offline", "Player is offline"),
        Map.entry("error.invalid_request", "This request is no longer valid"),
        Map.entry("error.mode_changed", "Teleport cancelled because a player is no longer in the required game mode"),
        Map.entry("error.request_disabled", "Spectator requests are disabled on this server"),
        Map.entry("error.self", "You cannot request yourself"),
        Map.entry("error.rate_limit", "Too many requests, wait before sending another"),
        Map.entry("error.teleport_failed", "Teleport could not be completed"),
        Map.entry("time", "%sm %ss")
    );
    public static String get(String key) { return TEXT.getOrDefault(key,key); }
    private English() {}
}
