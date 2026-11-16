package com.paceah.ghostbound.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.paceah.ghostbound.service.RequestService;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="ghostbound")
public final class SpectatorCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("spectp").requires(s->s.getEntity() instanceof ServerPlayer && s.getEntity()==s.getPlayer())
            .then(Commands.argument("player",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(c.getSource().getOnlinePlayerNames(),b))
                .executes(c->RequestService.create(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"player")))));
        for(boolean accept:new boolean[]{true,false}) e.getDispatcher().register(Commands.literal(accept?"spectpaccept":"spectpdeny")
            .requires(s->s.getEntity() instanceof ServerPlayer)
            .executes(c->RequestService.decide(c.getSource().getPlayerOrException(),null,accept))
            .then(Commands.argument("requestId",UuidArgument.uuid()).executes(c->RequestService.decide(c.getSource().getPlayerOrException(),UuidArgument.getUuid(c,"requestId"),accept))));
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e) {
        if(e.phase==TickEvent.Phase.END && e.getServer().getTickCount()%20==0) RequestService.sweep(e.getServer());
    }
}
