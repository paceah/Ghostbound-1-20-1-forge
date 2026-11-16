package com.paceah.ghostbound.mixin;

import com.paceah.ghostbound.config.ServerConfig;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SpectatorPacketMixin {
    @Inject(method="handleTeleportToEntityPacket",at=@At("HEAD"),cancellable=true)
    private void ghostbound$teleport(ServerboundTeleportToEntityPacket packet,CallbackInfo ci) {
        ServerPlayer player=((ServerGamePacketListenerImpl)(Object)this).player;
        // HEAD runs on Netty first; enforce vanilla's scheduling before reading gameplay/config state.
        PacketUtils.ensureRunningOnSameThread(packet,(ServerGamePacketListenerImpl)(Object)this,player.serverLevel());
        if(player.isSpectator() && ServerConfig.VANILLA_TP.get()) ci.cancel();
    }
}
