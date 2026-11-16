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
    private long lastNanos = System.nanoTime();
    private long remainderNanos;
    public static RequestSavedData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(RequestSavedData::load, RequestSavedData::new, "ghostbound_requests");
    }
    public long now() {
        // Elapsed runtime must advance even when the administrator moves the wall clock back.
        long nanos = System.nanoTime();
        long elapsed = Math.max(0, nanos-lastNanos) + remainderNanos;
        lastNanos = nanos;
        remainderNanos = elapsed % 1_000_000L;
        long next = clock == 0 ? System.currentTimeMillis() : clock + elapsed / 1_000_000L;
        if (clock != next) { clock=next; setDirty(); }
        return clock;
    }
    public static RequestSavedData load(CompoundTag root) {
        var d = new RequestSavedData();
        // Count forward offline time, but never restore requests from an earlier session.
        long wall = System.currentTimeMillis();
        d.clock = Math.max(wall, Math.min(root.getLong("Clock"), wall + 31536000000L));
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
        root.putInt("Version",2); root.putLong("Clock",now());
        root.remove("Pending");
        var penalties=new ListTag();
        cooldowns.forEach((p,until)->{var t=new CompoundTag();t.putUUID("Sender",p.sender());t.putUUID("Target",p.target());t.putLong("Until",until);penalties.add(t);});
        root.put("Cooldowns",penalties);
        var sent=new ListTag();
        lastSent.forEach((id,at)->{var t=new CompoundTag();t.putUUID("Sender",id);t.putLong("At",at);sent.add(t);});
        root.put("LastSent",sent); return root;
    }
}
