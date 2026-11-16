package com.paceah.ghostbound.mixin;

import com.paceah.ghostbound.config.ServerConfig;
import com.paceah.ghostbound.protection.SpectatorProtection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Redirect(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;setCamera(Lnet/minecraft/world/entity/Entity;)V"))
    private void ghostbound$camera(ServerPlayer p,Entity entity) {
        if(!p.isSpectator() || !ServerConfig.CAMERA.get() || entity==p) p.setCamera(entity);
    }
    @Redirect(method="openMenu",at=@At(value="INVOKE",target="Lnet/minecraft/world/MenuProvider;createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;"))
    private AbstractContainerMenu ghostbound$menu(MenuProvider provider,int id,Inventory inventory,Player player) {
        return SpectatorProtection.create(provider,id,inventory,player);
    }
    @Inject(method="openHorseInventory",at=@At("HEAD"),cancellable=true)
    private void ghostbound$horse(AbstractHorse horse,Container inventory,CallbackInfo ci) {
        if(SpectatorProtection.restricted((ServerPlayer)(Object)this)) ci.cancel();
    }
    @Inject(method="initMenu",at=@At("HEAD"),cancellable=true)
    private void ghostbound$sync(AbstractContainerMenu menu,CallbackInfo ci) {
        if(SpectatorProtection.blocked((ServerPlayer)(Object)this,menu)) ci.cancel();
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void ghostbound$tick(CallbackInfo ci) {
        var p=(ServerPlayer)(Object)this;
        // Close existing menus before vanilla broadcastChanges after a mode/config change.
        if(SpectatorProtection.blocked(p,p.containerMenu)) p.closeContainer();
    }
}
