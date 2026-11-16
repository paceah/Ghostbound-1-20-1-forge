package com.paceah.ghostbound.mixin;

import com.paceah.ghostbound.protection.SpectatorProtection;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value=NetworkHooks.class,remap=false)
public abstract class NetworkHooksMixin {
    @Redirect(method="openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/world/MenuProvider;createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",remap=true))
    private static AbstractContainerMenu ghostbound$menu(MenuProvider provider,int id,Inventory inventory,Player player) {
        return SpectatorProtection.create(provider,id,inventory,player);
    }
}
