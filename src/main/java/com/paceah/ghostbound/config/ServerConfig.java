package com.paceah.ghostbound.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue CONTAINERS, VANILLA_TP, CAMERA, REQUESTS, INCOMING;
    public static final ForgeConfigSpec.IntValue TIMEOUT, COOLDOWN, GAP, MAX_PENDING;
    static {
        var b = new ForgeConfigSpec.Builder();
        b.comment("Ghostbound - Server Configuration", "All rules are enforced on the logical server").push("restrictions");
        CONTAINERS = b.comment("Block all external menus, including slotless menus, before their creation through standard menu providers").define("blockContainerViewing", true);
        VANILLA_TP = b.comment("Block vanilla spectator teleport packets, not administrator commands").define("blockVanillaSpectatorTeleport", true);
        CAMERA = b.comment("Block spectator attack camera possession, not administrator commands").define("blockEntitySpectating", true);
        b.pop().push("teleport");
        REQUESTS = b.comment("Enable consent teleport requests").define("enableTeleportRequests", true);
        b.push("requests");
        TIMEOUT = b.comment("Real seconds before expiry, minimum 1, maximum 86400").defineInRange("requestTimeoutSeconds", 60, 1, 86400);
        COOLDOWN = b.comment("Pair cooldown after denial or expiry, 0 disables, maximum one year").defineInRange("rejectedRequestCooldownSeconds", 600, 0, 31536000);
        GAP = b.comment("Minimum real seconds between new requests from one sender, 0 disables").defineInRange("minimumSecondsBetweenNewRequests", 3, 0, 86400);
        MAX_PENDING = b.comment("Maximum pending requests per sender").defineInRange("maxPendingRequestsPerSender", 3, 1, 100);
        b.pop().pop().push("notifications");
        INCOMING = b.comment("Send incoming clickable chat notification").define("sendIncomingRequestMessage", true);
        b.pop();
        SPEC = b.build();
    }
    private ServerConfig() {}
}
