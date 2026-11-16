package com.paceah.ghostbound.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid="ghostbound",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ClientKeyBindings {
    private static final KeyMapping ACCEPT = new KeyMapping("key.ghostbound.accept_last",KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_P,"key.categories.ghostbound");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent e) { e.register(ACCEPT); }
    @Mod.EventBusSubscriber(modid="ghostbound",value=Dist.CLIENT)
    public static final class Ticks {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e) {
            if(e.phase!=TickEvent.Phase.END) return;
            var mc=Minecraft.getInstance();
            // Vanilla command transport needs no custom channel and works without a client mod on the server.
            while(ACCEPT.consumeClick()) if(mc.screen==null && mc.player!=null && mc.getConnection()!=null)
                mc.getConnection().sendCommand("spectpaccept");
        }
    }
}
