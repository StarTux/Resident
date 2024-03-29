package com.cavetale.resident;

import com.cavetale.core.struct.Vec2i;
import com.cavetale.resident.save.Loc;
import com.cavetale.resident.save.Zone;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@RequiredArgsConstructor
public final class PluginSpawn {
    @NonNull public final Plugin plugin;
    @NonNull public final ZoneType type;
    @NonNull public final Loc loc;
    @Setter protected Consumer<Player> onPlayerClick;
    @Setter protected Consumer<Mob> onMobSpawning;
    protected Spawned spawned;

    public static void clear(Plugin thePlugin) {
        ResidentPlugin.instance.clearPluginSpawns(thePlugin);
    }

    public void register() {
        ResidentPlugin.instance.pluginSpawns.add(this);
    }

    public static PluginSpawn register(final Plugin plugin, final ZoneType type, final Loc loc) {
        PluginSpawn result = new PluginSpawn(plugin, type, loc);
        result.register();
        return result;
    }

    public void unregister() {
        despawn();
        ResidentPlugin.instance.pluginSpawns.remove(this);
    }

    public void spawn() {
        if (spawned != null) return;
        World w = loc.getWorld();
        if (w == null) return;
        Vec2i chunk = loc.toChunk();
        if (!w.isChunkLoaded(chunk.x, chunk.z)) return;
        boolean nearby = false;
        final int viewDistance = w.getViewDistance();
        for (Player player : w.getPlayers()) {
            Location ploc = player.getLocation();
            int dx = Math.abs(chunk.x - (ploc.getBlockX() >> 4));
            if (dx > viewDistance) continue;
            int dz = Math.abs(chunk.z - (ploc.getBlockZ() >> 4));
            if (dz > viewDistance) continue;
            nearby = true;
            break;
        }
        if (!nearby) return;
        Location location = loc.toLocation(w);
        type.spawn(ResidentPlugin.instance, location, e -> {
                int entityId = e.getEntityId();
                spawned = new Spawned(e, Zone.NULL, null);
                spawned.pluginSpawn = this;
                ResidentPlugin.instance.spawnedMap.put(entityId, spawned);
                if (onMobSpawning != null) {
                    onMobSpawning.accept(e);
                }
            });
    }

    public void despawn() {
        if (spawned == null) return;
        spawned.remove();
        spawned = null;
    }

    public void click(Player player) {
        if (onPlayerClick != null) {
            player.closeInventory();
            onPlayerClick.accept(player);
        }
    }

    public boolean isSpawned() {
        return spawned != null;
    }

    public Entity getEntity() {
        return spawned != null ? spawned.entity : null;
    }

    public boolean pathing(Consumer<Mob> consumer) {
        if (spawned == null) return false;
        spawned.pathing = true;
        consumer.accept(spawned.entity);
        spawned.pathing = false;
        return true;
    }

    public boolean teleport(Location location) {
        if (spawned == null) return false;
        return spawned.entity.teleport(location);
    }

    public boolean teleportHome() {
        return teleport(loc.toLocation());
    }
}
