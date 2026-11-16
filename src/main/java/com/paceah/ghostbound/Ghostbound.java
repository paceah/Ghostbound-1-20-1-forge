package com.paceah.ghostbound;

import com.paceah.ghostbound.config.ServerConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Ghostbound.MOD_ID)
public final class Ghostbound {
    public static final String MOD_ID = "ghostbound";
    public Ghostbound() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }
}
