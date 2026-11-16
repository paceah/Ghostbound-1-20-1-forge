package com.paceah.ghostbound.persistence;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public final class RequestSavedData extends SavedData {
    public enum Status { PENDING, ACCEPTED, DENIED, EXPIRED, INVALID }
    public record Pair(UUID sender, UUID target) {}
    public static final class Request {
        public final UUID id, sender, target;
        public final long created, expires;
        public final String senderName, targetName;
        public Status status = Status.PENDING;
        public Request(UUID id, UUID sender, UUID target, long created, long expires, String sn, String tn) {
            this.id=id; this.sender=sender; this.target=target; this.created=created; this.expires=expires;
            senderName=sn; targetName=tn;
        }
        public Pair pair() { return new Pair(sender, target); }
    }
    public final Map<UUID, Request> pending = new HashMap<>();
    public final Map<Pair, Long> cooldowns = new HashMap<>();
    public final Map<UUID, Long> lastSent = new HashMap<>();
    public long clock;
    public static RequestSavedData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(RequestSavedData::load, RequestSavedData::new, "ghostbound_requests");
    }
    public long now() {
        // Clamp backward wall-clock jumps, including across restarts, to avoid resurrecting deadlines.
        long next = Math.max(clock, System.currentTimeMillis());
        if (clock != next) { clock=next; setDirty(); }
        return clock;
    }
    public static RequestSavedData load(CompoundTag root) {
        var d = new RequestSavedData();
        d.clock = Math.max(0, root.getLong("Clock"));
        for (var tag : root.getList("Pending", 10)) {
            var t=(CompoundTag)tag;
            if (!t.hasUUID("Id") || !t.hasUUID("Sender") || !t.hasUUID("Target")) continue;
            long c=t.getLong("Created"), e=t.getLong("Expires");
            if (c<0 || e<c || e-c>86400000L) continue;
            var r=new Request(t.getUUID("Id"),t.getUUID("Sender"),t.getUUID("Target"),c,e,t.getString("SenderName"),t.getString("TargetName"));
            d.pending.put(r.id,r);
        }
        for(var tag:root.getList("Cooldowns",10)) {
            var t=(CompoundTag)tag;
            if(t.hasUUID("Sender") && t.hasUUID("Target") && t.getLong("Until")>0)
                d.cooldowns.put(new Pair(t.getUUID("Sender"),t.getUUID("Target")),t.getLong("Until"));
        }
        for(var tag:root.getList("LastSent",10)) {
            var t=(CompoundTag)tag;
            if(t.hasUUID("Sender")) d.lastSent.put(t.getUUID("Sender"),t.getLong("At"));
        }
        return d;
    }
    @Override public CompoundTag save(CompoundTag root) {
        root.putInt("Version",1); root.putLong("Clock",clock);
        var requests=new ListTag();
        for(var r:pending.values()) {
            var t=new CompoundTag(); t.putUUID("Id",r.id); t.putUUID("Sender",r.sender); t.putUUID("Target",r.target);
            t.putLong("Created",r.created); t.putLong("Expires",r.expires);
            t.putString("SenderName",r.senderName); t.putString("TargetName",r.targetName); requests.add(t);
        }
        root.put("Pending",requests);
        var penalties=new ListTag();
        cooldowns.forEach((p,until)->{var t=new CompoundTag();t.putUUID("Sender",p.sender());t.putUUID("Target",p.target());t.putLong("Until",until);penalties.add(t);});
        root.put("Cooldowns",penalties);
        var sent=new ListTag();
        lastSent.forEach((id,at)->{var t=new CompoundTag();t.putUUID("Sender",id);t.putLong("At",at);sent.add(t);});
        root.put("LastSent",sent); return root;
    }
}
