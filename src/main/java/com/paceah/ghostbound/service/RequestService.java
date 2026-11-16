package com.paceah.ghostbound.service;

import com.paceah.ghostbound.config.ServerConfig;
import com.paceah.ghostbound.message.Messages;
import com.paceah.ghostbound.persistence.RequestSavedData;
import com.paceah.ghostbound.persistence.RequestSavedData.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import static net.minecraft.ChatFormatting.*;

public final class RequestService {
    private static void error(ServerPlayer p,String key,Object... args) { Messages.send(p,key,RED,args); }
    public static void sweep(MinecraftServer s) {
        var d=RequestSavedData.get(s); long now=d.now();
        for(var r:new ArrayList<>(d.pending.values())) {
            if(now>=r.expires) {
                finish(d,r,Status.EXPIRED,r.expires);
                var sender=s.getPlayerList().getPlayer(r.sender);
                Messages.send(sender,"request.expired",YELLOW,r.targetName);
                cooldownMessage(sender,r.targetName,d.cooldowns.getOrDefault(r.pair(),now)-now);
            } else {
                var sender=s.getPlayerList().getPlayer(r.sender); var target=s.getPlayerList().getPlayer(r.target);
                // Offline requests remain until expiry; mode changes invalidate even before a later reconnect.
                if(sender!=null && !sender.isSpectator() || target!=null && target.isSpectator()) {
                    finish(d,r,Status.INVALID,now);
                    error(sender,"error.mode_changed"); error(target,"error.invalid_request");
                }
            }
        }
        if(d.cooldowns.entrySet().removeIf(e->e.getValue()<=now) |
           d.lastSent.entrySet().removeIf(e->now-e.getValue()>86400000L)) d.setDirty();
    }
    private static void finish(RequestSavedData d,Request r,Status status,long at) {
        r.status=status; d.pending.remove(r.id);
        if(status==Status.DENIED || status==Status.EXPIRED) {
            long seconds=ServerConfig.COOLDOWN.get();
            if(seconds>0) d.cooldowns.put(r.pair(),Math.addExact(at,seconds*1000L));
        }
        d.setDirty();
    }
    private static void cooldownMessage(ServerPlayer p,String name,long remaining) {
        if(remaining>0) {
            long seconds=(remaining+999)/1000;
            error(p,"request.cooldown",name,Messages.text("time",seconds/60,seconds%60));
        }
    }
    public static int create(ServerPlayer sender, String targetName) {
        var s=sender.server; sweep(s); var d=RequestSavedData.get(s); long now=d.now();
        if(!ServerConfig.REQUESTS.get()) {error(sender,"error.request_disabled");return 0;}
        if(!sender.isSpectator()) {error(sender,"error.not_spectator");return 0;}
        var target=s.getPlayerList().getPlayerByName(targetName);
        if(target==null) {error(sender,"error.target_offline");return 0;}
        if(target==sender) {error(sender,"error.self");return 0;}
        if(target.isSpectator()) {error(sender,"error.target_spectator");return 0;}
        if(!sender.isAlive() || !target.isAlive()) {error(sender,"error.invalid_request");return 0;}
        var pair=new Pair(sender.getUUID(),target.getUUID());
        if(d.pending.values().stream().anyMatch(r->r.pair().equals(pair))) {error(sender,"request.pending");return 0;}
        long remaining=d.cooldowns.getOrDefault(pair,now)-now;
        if(remaining>0) {cooldownMessage(sender,targetName,remaining);return 0;}
        if(now-d.lastSent.getOrDefault(sender.getUUID(),0L)<ServerConfig.GAP.get()*1000L ||
            d.pending.values().stream().filter(r->r.sender.equals(sender.getUUID())).count()>=ServerConfig.MAX_PENDING.get()) {
            error(sender,"error.rate_limit");return 0;
        }
        var r=new Request(UUID.randomUUID(),sender.getUUID(),target.getUUID(),now,now+ServerConfig.TIMEOUT.get()*1000L,sender.getGameProfile().getName(),target.getGameProfile().getName());
        d.pending.put(r.id,r); d.lastSent.put(r.sender,now); d.setDirty();
        Messages.send(sender,"request.sent",YELLOW,r.targetName);
        if(ServerConfig.INCOMING.get()) target.sendSystemMessage(Messages.text("request.incoming",r.senderName)
            .withStyle(YELLOW).append(" ").append(button(r.id,true)).append(" ").append(button(r.id,false)));
        return 1;
    }
    private static MutableComponent button(UUID id,boolean accept) {
        return Component.literal(accept?"[✔]":"[✘]").withStyle(style->style.withColor(accept?GREEN:RED)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,(accept?"/spectpaccept ":"/spectpdeny ")+id))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,Messages.text(accept?"request.accept":"request.deny"))));
    }
    public static int decide(ServerPlayer target,UUID id,boolean accept) {
        var s=target.server; sweep(s); var d=RequestSavedData.get(s); long now=d.now();
        if(!ServerConfig.REQUESTS.get()) {error(target,"error.request_disabled");return 0;}
        Request r=id==null?d.pending.values().stream().filter(q->q.target.equals(target.getUUID()))
            .max(Comparator.comparingLong((Request q)->q.created).thenComparing(q->q.id)).orElse(null):d.pending.get(id);
        if(r==null || !r.target.equals(target.getUUID()) || r.status!=Status.PENDING || now>=r.expires) {
            error(target,id==null?"request.no_pending":"error.invalid_request");return 0;
        }
        var sender=s.getPlayerList().getPlayer(r.sender);
        if(sender==null || sender.hasDisconnected() || target.hasDisconnected() || !sender.isSpectator() || target.isSpectator() ||
            !sender.isAlive() || !target.isAlive() || s.getLevel(target.serverLevel().dimension())!=target.serverLevel()) {
            finish(d,r,Status.INVALID,now); error(target,"error.invalid_request");error(sender,"error.mode_changed");return 0;
        }
        if(!accept) {
            finish(d,r,Status.DENIED,now);Messages.send(sender,"request.denied",RED,r.targetName);
            Messages.send(target,"request.declined",YELLOW,r.senderName);return 1;
        }
        // All callers execute on the server thread. Consume before any reentrant teleport events.
        d.pending.remove(r.id); d.setDirty();
        try {
            sender.teleportTo(target.serverLevel(),target.getX(),target.getY(),target.getZ(),target.getYRot(),target.getXRot());
            if(sender.serverLevel()!=target.serverLevel() || sender.position().distanceToSqr(target.position())>0.01) {
                r.status=Status.INVALID;error(target,"error.teleport_failed");error(sender,"error.teleport_failed");return 0;
            }
            r.status=Status.ACCEPTED;
            Messages.send(sender,"request.accepted",GREEN,r.targetName); Messages.send(target,"request.completed",GREEN,r.senderName);
            return 1;
        } catch(RuntimeException ex) {
            r.status=Status.INVALID;
            org.slf4j.LoggerFactory.getLogger("ghostbound").error("Consent teleport failed for request {}",r.id,ex);
            error(target,"error.teleport_failed");error(sender,"error.teleport_failed");return 0;
        }
    }
    private RequestService() {}
}
