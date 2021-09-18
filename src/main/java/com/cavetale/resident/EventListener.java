package com.cavetale.resident;

import com.cavetale.core.event.entity.PlayerEntityAbilityQuery;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.event.entity.EntityMoveEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final ResidentPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
        Spawned spawned = plugin.spawnedMap.remove(event.getEntity().getEntityId());
    }

    @EventHandler
    private void onEntityMove(EntityMoveEvent event) {
        Spawned spawned = plugin.spawnedMap.get(event.getEntity().getEntityId());
        if (spawned == null) return;
        spawned.lastMoved = System.currentTimeMillis();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onEntityDamage(EntityDamageEvent event) {
        Spawned spawned = plugin.spawnedMap.get(event.getEntity().getEntityId());
        if (spawned == null) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onEntityCombust(EntityCombustEvent event) {
        Spawned spawned = plugin.spawnedMap.get(event.getEntity().getEntityId());
        if (spawned == null) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Spawned spawned = plugin.spawnedMap.get(event.getRightClicked().getEntityId());
        if (spawned == null) return;
        event.setCancelled(true);
        Zoned zoned = plugin.zonedMap.get(spawned.zone.getName());
        if (zoned == null) return;
        if (spawned.messageIndex < 0 || spawned.messageIndex >= zoned.messageList.size()) return;
        event.getPlayer().sendMessage(TextComponent.ofChildren(new Component[] {
                    Component.text("Villager: ", NamedTextColor.WHITE),
                    Component.text(zoned.messageList.get(spawned.messageIndex), NamedTextColor.GRAY),
                }));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onEntityPortal(EntityPortalEvent event) {
        Spawned spawned = plugin.spawnedMap.get(event.getEntity().getEntityId());
        if (spawned == null) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    private void onPlayerEntityAbility(PlayerEntityAbilityQuery query) {
        Spawned spawned = plugin.spawnedMap.get(query.getEntity().getEntityId());
        if (spawned == null) return;
        query.setCancelled(true);
    }
}
