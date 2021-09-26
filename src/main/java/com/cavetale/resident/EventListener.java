package com.cavetale.resident;

import com.cavetale.core.event.entity.PlayerEntityAbilityQuery;
import com.destroystokyo.paper.event.entity.EntityPathfindEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
    }

    @EventHandler
    private void onEntityMove(EntityMoveEvent event) {
        Spawned spawned = handleEventEntity(event.getEntity(), null); // do not cancel!
        if (spawned == null) return;
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
        Zoned zoned = plugin.zonedMap.get(spawned.zone.getName());
        if (zoned == null) return;
        zoned.talkTo(spawned, event.getPlayer());
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Spawned spawned = handleEventEntity(event.getEntity(), event);
        if (spawned == null) return;
        if (!(event.getDamager() instanceof Player)) return;
        Zoned zoned = plugin.zonedMap.get(spawned.zone.getName());
        if (zoned == null) return;
        zoned.talkTo(spawned, (Player) event.getDamager());
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

}
