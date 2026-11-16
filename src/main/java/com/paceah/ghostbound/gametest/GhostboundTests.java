package com.paceah.ghostbound.gametest;

import com.mojang.authlib.GameProfile;
import com.paceah.ghostbound.config.ServerConfig;
import com.paceah.ghostbound.persistence.RequestSavedData;
import com.paceah.ghostbound.service.RequestService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("ghostbound")
@PrefixGameTestTemplate(false)
public final class GhostboundTests {
    @GameTest(template="empty",timeoutTicks=100)
    public static void clockAdvancesAheadOfWallTime(GameTestHelper h) {
        var d=new RequestSavedData();
        long future=System.currentTimeMillis()+3_600_000L;
        d.clock=future;
        d.now();
        h.runAfterDelay(5,()->{
            h.assertTrue(d.now()>future,"Timer froze while logical clock was ahead of wall time");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void consentAndProtection(GameTestHelper h) {
        var s=h.getLevel().getServer();
        var existing=RequestSavedData.get(s);
        var pendingBackup=new java.util.HashMap<>(existing.pending);
        var cooldownBackup=new java.util.HashMap<>(existing.cooldowns);
        var sentBackup=new java.util.HashMap<>(existing.lastSent);
        int gap=ServerConfig.GAP.get();
        int timeout=ServerConfig.TIMEOUT.get();
        int cooldown=ServerConfig.COOLDOWN.get();
        boolean vanillaTp=ServerConfig.VANILLA_TP.get(), camera=ServerConfig.CAMERA.get();
        boolean containers=ServerConfig.CONTAINERS.get(), requests=ServerConfig.REQUESTS.get();
        var a=new ServerPlayer(s,h.getLevel(),new GameProfile(UUID.randomUUID(),"GBSender"));
        var b=new ServerPlayer(s,h.getLevel(),new GameProfile(UUID.randomUUID(),"GBTarget"));
        var ca=new Connection(PacketFlow.SERVERBOUND);
        var cb=new Connection(PacketFlow.SERVERBOUND);
        var channelA=new io.netty.channel.embedded.EmbeddedChannel(ca);
        var channelB=new io.netty.channel.embedded.EmbeddedChannel(cb);
        s.getPlayerList().placeNewPlayer(ca,a);
        s.getPlayerList().placeNewPlayer(cb,b);
        var d=RequestSavedData.get(s);
        long originalClock=d.clock;
        try {
            // Test only its own requests and restore the host world's state afterwards.
            d.pending.clear(); d.cooldowns.clear(); d.lastSent.clear();
            ServerConfig.TIMEOUT.set(60); ServerConfig.COOLDOWN.set(600);
            ServerConfig.REQUESTS.set(true);
            ServerConfig.VANILLA_TP.set(true); ServerConfig.CAMERA.set(true); ServerConfig.CONTAINERS.set(true);
            ServerConfig.GAP.set(0);
            b.setGameMode(GameType.SURVIVAL);
            for(var mode:new GameType[]{GameType.SURVIVAL,GameType.ADVENTURE,GameType.CREATIVE}) {
                a.setGameMode(mode);
                h.assertTrue(RequestService.create(a,"GBTarget")==0,"Non-spectator request accepted");
            }
            a.setGameMode(GameType.SPECTATOR);
            h.assertTrue(RequestService.create(a,"GBSender")==0,"Self request accepted");
            h.assertTrue(RequestService.create(a,"Missing")==0,"Offline request accepted");
            h.assertTrue(RequestService.create(a,"GBTarget")==1,"Request not created");
            var r=d.pending.values().iterator().next();
            h.assertTrue(r.expires-r.created==60000L,"Default timeout is not 60 seconds");
            h.assertTrue(RequestService.create(a,"GBTarget")==0,"Duplicate accepted");
            h.assertTrue(RequestService.decide(a,r.id,true)==0,"Unauthorized acceptance");
            h.assertTrue(d.pending.containsKey(r.id),"Unauthorized acceptance destroyed request");
            a.setGameMode(GameType.CREATIVE);
            h.assertTrue(!d.pending.containsKey(r.id),"Mode change did not immediately invalidate request");
            a.setGameMode(GameType.SPECTATOR);
            h.assertTrue(RequestService.decide(b,r.id,true)==0,"Mode-change bypass");
            a.setGameMode(GameType.SPECTATOR);
            h.assertTrue(RequestService.create(a,"GBTarget")==1,"Second request not created");
            r=d.pending.values().iterator().next();
            h.assertTrue(RequestService.decide(b,r.id,false)==1,"Denial failed");
            h.assertTrue(d.cooldowns.get(r.pair())-d.clock<=600000L && d.cooldowns.get(r.pair())-d.clock>599000L,"Default cooldown is not 600 seconds");
            var loaded=RequestSavedData.load(d.save(new CompoundTag()));
            h.assertTrue(loaded.cooldowns.equals(d.cooldowns),"Cooldown did not roundtrip NBT");
            h.assertTrue(loaded.pending.isEmpty(),"Pending requests survived reload");
            h.assertTrue(RequestService.create(a,"GBTarget")==0,"Cooldown bypass");
            d.cooldowns.clear();
            h.assertTrue(RequestService.create(a,"GBTarget")==1,"Third request not created");
            r=d.pending.values().iterator().next();
            h.assertTrue(RequestSavedData.load(d.save(new CompoundTag())).pending.isEmpty(),"Live pending request survived reload");
            b.teleportTo(h.getLevel(),a.getX()+10,a.getY(),a.getZ(),0,0);
            var before=a.position();
            a.connection.handleTeleportToEntityPacket(new ServerboundTeleportToEntityPacket(b.getUUID()));
            h.assertTrue(a.position().equals(before),"Vanilla teleport packet bypass");
            a.attack(b);
            h.assertTrue(a.getCamera()==a,"Camera attack bypass");
            var pig=net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());
            a.attack(pig);
            h.assertTrue(a.getCamera()==a,"Mob camera attack bypass");
            channelA.outboundMessages().clear();
            h.assertTrue(a.openMenu(new SimpleMenuProvider((id,inv,p)->ChestMenu.threeRows(id,inv,new SimpleContainer(27)),Component.literal("QA"))).isEmpty(),"External menu opened");
            net.minecraftforge.network.NetworkHooks.openScreen(a,new SimpleMenuProvider((id,inv,p)->ChestMenu.threeRows(id,inv,new SimpleContainer(27)),Component.literal("Forge QA")));
            h.assertTrue(a.containerMenu==a.inventoryMenu,"Forge external menu opened");
            var forbidden=new SimpleMenuProvider((id,inv,p)->{
                throw new AssertionError("Blocked menu constructor was called");
            },Component.literal("QA"));
            h.assertTrue(a.openMenu(forbidden).isEmpty(),"Constructor guard failed");
            net.minecraftforge.network.NetworkHooks.openScreen(a,forbidden,buf->{
                throw new AssertionError("Blocked menu data writer was called");
            });
            h.assertTrue(channelA.outboundMessages().stream().noneMatch(packet->
                packet instanceof net.minecraft.network.protocol.game.ClientboundOpenScreenPacket ||
                packet instanceof net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket ||
                packet instanceof net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket),"Blocked menu sent inventory data");
            h.assertTrue(RequestService.decide(b,r.id,true)==1,"Consent teleport failed");
            h.assertTrue(a.position().distanceToSqr(b.position())<0.01,"Wrong destination");
            h.assertTrue(RequestService.decide(b,r.id,true)==0,"Request replay accepted");
            h.assertTrue(!d.cooldowns.containsKey(r.pair()),"Acceptance penalized sender");
            // Exercise each restriction independently in both directions.
            ServerConfig.VANILLA_TP.set(false);
            b.teleportTo(h.getLevel(),a.getX()+10,a.getY(),a.getZ(),0,0);
            a.connection.handleTeleportToEntityPacket(new ServerboundTeleportToEntityPacket(b.getUUID()));
            h.assertTrue(a.position().distanceToSqr(b.position())<0.01,"Vanilla TP toggle did not restore behavior");
            ServerConfig.CAMERA.set(false);
            a.attack(b);
            h.assertTrue(a.getCamera()==b,"Camera toggle did not restore behavior");
            ServerConfig.CAMERA.set(true);
            a.tick();
            h.assertTrue(a.getCamera()==a,"Enabling camera restriction did not detach existing camera");
            ServerConfig.CAMERA.set(false);
            a.setCamera(a);
            ServerConfig.CONTAINERS.set(false);
            h.assertTrue(a.openMenu(new SimpleMenuProvider((id,inv,p)->ChestMenu.threeRows(id,inv,new SimpleContainer(27)),Component.literal("QA"))).isPresent(),"Container toggle did not restore behavior");
            a.closeContainer();
            h.assertTrue(RequestService.create(a,"GBTarget")==1,"Request blocked with restrictions off");
            r=d.pending.values().iterator().next();
            // Advance only the persisted clock in this isolated test world, not the host clock.
            d.clock=r.expires-100;
            RequestService.sweep(s);
            h.assertTrue(d.pending.containsKey(r.id),"Request expired early");
            d.clock=r.expires;
            RequestService.sweep(s);
            h.assertTrue(!d.pending.containsKey(r.id),"Request valid at expiry boundary");
            h.assertTrue(d.cooldowns.get(r.pair())==r.expires+600000L,"Expiry penalty starts at wrong time");
            h.assertTrue(RequestService.decide(b,r.id,true)==0,"Expired request accepted");
            d.clock=r.expires+599900L;
            h.assertTrue(RequestService.create(a,"GBTarget")==0,"Penalty ended early");
            d.clock=r.expires+600000L;
            h.assertTrue(RequestService.create(a,"GBTarget")==1,"Penalty did not end at boundary");
            h.succeed();
        } finally {
            ServerConfig.GAP.set(gap);
            ServerConfig.TIMEOUT.set(timeout); ServerConfig.COOLDOWN.set(cooldown);
            ServerConfig.REQUESTS.set(requests);
            ServerConfig.VANILLA_TP.set(vanillaTp);ServerConfig.CAMERA.set(camera);ServerConfig.CONTAINERS.set(containers);
            d.clock=originalClock;
            d.pending.clear(); d.cooldowns.clear(); d.lastSent.clear();d.setDirty();
            d.pending.putAll(pendingBackup);d.cooldowns.putAll(cooldownBackup);d.lastSent.putAll(sentBackup);
            s.getPlayerList().remove(a);s.getPlayerList().remove(b);
            channelA.finishAndReleaseAll();channelB.finishAndReleaseAll();
        }
    }
}
