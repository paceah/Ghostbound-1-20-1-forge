package com.paceah.ghostbound.message;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class Messages {
    // The fallback is embedded in vanilla chat components so unmodded clients can read them.
    public static MutableComponent text(String key, Object... args) {
        return Component.translatableWithFallback("ghostbound." + key, English.get(key), args);
    }
    public static void send(ServerPlayer p, String key, ChatFormatting color, Object... args) {
        if (p != null) p.sendSystemMessage(text(key, args).withStyle(color));
    }
    private Messages() {}
}
