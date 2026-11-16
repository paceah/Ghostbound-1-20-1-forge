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
        return restricted(p) && menu!=p.inventoryMenu && menu.slots.stream().anyMatch(slot->slot.container!=p.getInventory());
    }
    public static AbstractContainerMenu create(MenuProvider provider,int id,Inventory inventory,Player player) {
        var menu=provider.createMenu(id,inventory,player);
        if(menu!=null && player instanceof ServerPlayer p && blocked(p,menu)) {
            // Balance startOpen and other constructor side effects without attaching any synchronizer.
            menu.removed(player); return null;
        }
        return menu;
    }
    private SpectatorProtection() {}
}
