package com.paceah.ghostbound.protection;

import com.paceah.ghostbound.config.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class SpectatorProtection {
    public static boolean restricted(ServerPlayer p) { return p.isSpectator() && ServerConfig.CONTAINERS.get(); }
    public static boolean blocked(ServerPlayer p, AbstractContainerMenu menu) {
        return restricted(p) && menu!=p.inventoryMenu;
    }
    public static AbstractContainerMenu create(MenuProvider provider,int id,Inventory inventory,Player player) {
        // Reject before constructors can send custom data or change the underlying container.
        if(player instanceof ServerPlayer p && restricted(p)) return null;
        return provider.createMenu(id,inventory,player);
    }
    private SpectatorProtection() {}
}
