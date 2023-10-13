package com.cavetale.resident;

import com.cavetale.core.event.entity.PlayerEntityAbilityQuery;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final ResidentPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private Spawned handleEventEntity(Entity entity, Cancellable event) {
        Spawned spawned = plugin.spawnedMap.get(entity.getEntityId());
        if (spawned == null) return null;
        if (event != null) event.setCancelled(true);
        return spawned;
    }

    @EventHandler
    private void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        Spawned spawned = plugin.spawnedMap.remove(event.getEntity().getEntityId());
        if (spawned == null) return;
        if (spawned.pluginSpawn != null) {
            spawned.pluginSpawn.spawned = null;
        }
        Bukkit.getScheduler().runTask(plugin, spawned::remove);
    }

    @EventHandler(ignoreCancelled = true)
    private void onEntityMove(EntityMoveEvent event) {
        Spawned spawned = handleEventEntity(event.getEntity(), null); // do not cancel!
        if (spawned == null) return;
        Vec3i vec = Vec3i.of(event.getTo());
        if (spawned.lastMovedVec != null
            && vec.maxHorizontalDistance(spawned.lastMovedVec) == 0) return;
        spawned.lastMovedVec = vec;
        spawned.lastMoved = System.currentTimeMillis();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onEntityDamage(EntityDamageEvent event) {
        handleEventEntity(event.getEntity(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onEntityCombust(EntityCombustEvent event) {
        handleEventEntity(event.getEntity(), event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Spawned spawned = handleEventEntity(event.getRightClicked(), event);
        if (spawned == null) return;
        final Player player = event.getPlayer();
        if (!spawned.zone.isNull()) {
            Zoned zoned = plugin.zonedMap.get(spawned.zone.getName());
            if (zoned == null) return;
            zoned.talkTo(spawned, player);
            return;
        }
        if (spawned.pluginSpawn != null) {
            InventoryType it = player.getOpenInventory().getType();
            if (it == InventoryType.CRAFTING || it == InventoryType.CREATIVE) {
                spawned.pluginSpawn.click(player);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        switch (event.getCause()) {
        case ENTITY_ATTACK: break;
        default: return;
        }
        Spawned spawned = handleEventEntity(event.getEntity(), event);
        if (spawned == null) return;
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        if (!spawned.zone.isNull()) {
            Zoned zoned = plugin.zonedMap.get(spawned.zone.getName());
            if (zoned == null) return;
            zoned.talkTo(spawned, player);
            return;
        }
        if (spawned.pluginSpawn != null && player.getOpenInventory().getType() == InventoryType.CRAFTING) {
            spawned.pluginSpawn.click(player);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onEntityPortal(EntityPortalEvent event) {
        handleEventEntity(event.getEntity(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onVillagerReplenishTrade(VillagerReplenishTradeEvent event) {
        handleEventEntity(event.getEntity(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onVillagerAcquireTrade(VillagerAcquireTradeEvent event) {
        handleEventEntity(event.getEntity(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onVillagerCareerChange(VillagerCareerChangeEvent event) {
        handleEventEntity(event.getEntity(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onEntityTarget(EntityTargetEvent event) {
        handleEventEntity(event.getEntity(), event);
        if (event.getTarget() != null) {
            handleEventEntity(event.getTarget(), event);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onEntityPathfind(EntityPathfindEvent event) {
        Spawned spawned = handleEventEntity(event.getEntity(), null); // do not cancel yet
        if (spawned == null) return;
        if (spawned.pathing) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerEntityAbility(PlayerEntityAbilityQuery query) {
        handleEventEntity(query.getEntity(), query);
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        plugin.sessions.remove(event.getPlayer().getUniqueId());
    }


    @EventHandler
    void onPluginDisable(PluginDisableEvent event) {
        plugin.clearPluginSpawns(event.getPlugin());
    }

    @EventHandler
    void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        Vec2i chunkVector = Vec2i.of(chunk.getX(), chunk.getZ());
        String chunkWorld = chunk.getWorld().getName();
        for (Zoned zoned : plugin.zonedMap.values()) {
            zoned.onChunkLoad(chunkWorld, chunkVector, true);
        }
    }

    @EventHandler
    void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        Vec2i chunkVector = Vec2i.of(chunk.getX(), chunk.getZ());
        String chunkWorld = chunk.getWorld().getName();
        for (Zoned zoned : plugin.zonedMap.values()) {
            zoned.onChunkLoad(chunkWorld, chunkVector, false);
        }
    }
}
