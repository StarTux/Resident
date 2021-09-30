package com.cavetale.resident;

import com.cavetale.resident.save.Loc;
import com.cavetale.resident.save.Zone;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@RequiredArgsConstructor
public final class PluginSpawn {
    @NonNull public final Plugin plugin;
    @NonNull public final ZoneType type;
    @NonNull public final Loc loc;
    @Setter protected Consumer<Player> onPlayerClick;
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
        Location location = loc.toLocation();
        if (location == null || !location.isChunkLoaded()) return;
        type.spawn(ResidentPlugin.instance, location, e -> {
                int entityId = e.getEntityId();
                spawned = new Spawned(e, Zone.NULL, -1);
                spawned.pluginSpawn = this;
                ResidentPlugin.instance.spawnedMap.put(entityId, spawned);
            });
    }

    public void despawn() {
        if (spawned == null) return;
        spawned.entity.remove();
        spawned = null;
    }

    public void click(Player player) {
        if (onPlayerClick != null) {
            onPlayerClick.accept(player);
        }
    }

    public boolean isSpawned() {
        return spawned != null;
    }
}
