package com.cavetale.resident;

import com.cavetale.resident.save.Cuboid;
import com.cavetale.resident.save.Vec2i;
import com.cavetale.resident.save.Vec3i;
import com.cavetale.resident.save.Zone;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

/**
 * This class represents the runtime object of a Zone.
 */
@RequiredArgsConstructor
public final class Zoned {
    protected static final int SPAWN_DISTANCE = 16;
    protected final ResidentPlugin plugin;
    protected final Zone zone;
    protected final List<String> messageList;
    protected final Set<Vec3i> spawnBlocks = new HashSet<>();
    protected final Map<Vec2i, Set<Vec3i>> chunkBlockMap = new HashMap<>();
    protected int updateId = 0;
    protected int total = 0;
    protected boolean disabled;

    public World getWorld() {
        return Bukkit.getWorld(zone.getWorld());
    }

    protected void disable() {
        disabled = true;
    }

    protected void updateSpawnBlocks() {
        updateId += 1;
        spawnBlocks.clear();
        chunkBlockMap.clear();
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;
        Set<Vec3i> vectorSet = new HashSet<>();
        for (Cuboid cuboid : zone.getRegions()) {
            vectorSet.addAll(cuboid.all());
        }
        for (Vec3i vector : vectorSet) {
            chunkBlockMap.computeIfAbsent(vector.toChunk(), u -> new HashSet<>()).add(vector);
        }
        for (Map.Entry<Vec2i, Set<Vec3i>> entry : new ArrayList<>(chunkBlockMap.entrySet())) {
            Vec2i chunkVector = entry.getKey();
            Set<Vec3i> chunkVectorSet = entry.getValue();
            computeChunkSpawnBlocks(world, entry.getKey(), entry.getValue(), updateId);
        }
    }

    protected boolean canSpawnOnBlock(Block block) {
        if (block.isPassable() || block.isLiquid()) return false;
        Block above = block.getRelative(0, 1, 0);
        if (!above.isPassable() || above.isLiquid()) return false;
        Block above2 = block.getRelative(0, 2, 0);
        if (!above2.isPassable() || above2.isLiquid()) return false;
        return true;
    }

    private void computeChunkSpawnBlocks(World world, Vec2i chunkVector, Set<Vec3i> vectorSet, final int id) {
        world.getChunkAtAsync(chunkVector.x, chunkVector.y, (Consumer<Chunk>) c -> {
                if (id != this.updateId || disabled) return;
                for (Iterator<Vec3i> iter = vectorSet.iterator(); iter.hasNext();) {
                    Vec3i vector = iter.next();
                    Block block = vector.toBlock(world);
                    if (!canSpawnOnBlock(block)) {
                        iter.remove();
                        continue;
                    }
                    spawnBlocks.add(vector);
                    if (spawnBlocks.size() > 100000) return; // Magic number
                }
            });
    }

    private Set<Vec3i> computeLoadedBlockSet(World world) {
        Set<Vec3i> result = new HashSet<>();
        for (Map.Entry<Vec2i, Set<Vec3i>> entry : chunkBlockMap.entrySet()) {
            Vec2i chunkVector = entry.getKey();
            if (!world.isChunkLoaded(chunkVector.x, chunkVector.y)) continue;
            result.addAll(entry.getValue());
        }
        return result;
    }

    private Set<Vec3i> computePlayerVectorSet(World world) {
        Set<Vec3i> result = new HashSet<>();
        for (Player player : world.getPlayers()) {
            result.add(Vec3i.of(player.getLocation()));
        }
        return result;
    }

    protected void spawn() {
        World world = getWorld();
        if (world == null) {
            total = 0;
            return;
        }
        int count = plugin.countSpawned(zone);
        if (count >= zone.getMaxResidents()) return;
        List<Vec3i> loadedBlockList = new ArrayList<>(computeLoadedBlockSet(world));
        if (loadedBlockList.isEmpty()) {
            total = 0;
            return;
        }
        // Total
        total = (int) Math.ceil((double) zone.getMaxResidents()
                                * (((double) loadedBlockList.size())
                                   / ((double) spawnBlocks.size())));
        int difference = total - count;
        if (difference < 1) return;
        // Do not spawn near players
        Set<Vec3i> playerVectorSet = computePlayerVectorSet(world);
        loadedBlockList.removeIf(it -> {
                for (Vec3i playerVector : playerVectorSet) {
                    if (it.maxDistance(playerVector) < 16) return true;
                }
                return false;
            });
        if (loadedBlockList.isEmpty()) return;
        // Do not spawn near others
        List<Spawned> existingList = plugin.findSpawned(zone);
        loadedBlockList.removeIf(it -> {
                for (Spawned existing : existingList) {
                    if (existing.movingTo == null) continue;
                    if (existing.movingTo.maxDistance(it) < SPAWN_DISTANCE) return true;
                }
                return false;
            });
        if (loadedBlockList.isEmpty()) return;
        // Spawn!
        for (int i = 0; i < difference; i += 1) {
            Vec3i blockVector = loadedBlockList.remove(plugin.random.nextInt(loadedBlockList.size()));
            Block block = blockVector.toBlock(world);
            if (!canSpawnOnBlock(block)) continue;
            spawn(block.getLocation().add(0.5, 1.0, 0.5));
            loadedBlockList.removeIf(it -> blockVector.maxDistance(it) < SPAWN_DISTANCE);
            if (loadedBlockList.isEmpty()) break;
        }
    }

    protected void spawn(Location location) {
        if (zone.getType() == null) return;
        final int messageIndex;
        if (!messageList.isEmpty()) {
            int[] usedIndexes = new int[messageList.size()];
            for (Spawned existing : plugin.findSpawned(zone)) {
                if (existing.messageIndex >= 0 && existing.messageIndex < usedIndexes.length) {
                    usedIndexes[existing.messageIndex] += 1;
                }
            }
            int minValue = usedIndexes[0];
            int lowestIndex = 0;
            for (int i = 1; i < usedIndexes.length; i += 1) {
                if (usedIndexes[i] < minValue) {
                    minValue = usedIndexes[i];
                    lowestIndex = i;
                }
            }
            messageIndex = lowestIndex;
        } else {
            messageIndex = -1;
        }
        Mob entity = zone.getType().spawn(plugin, location, e -> {
                int entityId = e.getEntityId();
                Spawned spawned = new Spawned(e, zone, messageIndex);
                plugin.spawnedMap.put(entityId, spawned);
                spawned.movingTo = Vec3i.of(location);
            });
    }

    protected void move() {
        long now = System.currentTimeMillis();
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;
        long then = now - 2000L;
        List<Spawned> spawnedList = plugin.findSpawned(zone);
        for (Spawned spawned : spawnedList) {
            if (spawned.lastMoved > then) continue;
            if (spawned.moveCooldown > now) continue;
            spawned.moveCooldown = now + 5000L;
            move(spawned, world);
        }
    }

    private void move(Spawned spawned, World world) {
        Vec3i mobVector = Vec3i.of(spawned.entity.getLocation());
        List<Vec3i> loadedBlockList = new ArrayList<>(computeLoadedBlockSet(world));
        loadedBlockList.removeIf(blockVector -> {
                int distance = blockVector.maxDistance(mobVector);
                return distance < 8 || distance > 32;
            });
        if (loadedBlockList.isEmpty()) {
            spawned.entity.remove(); // modifies spawnedMap?
            return;
        }
        // Move away from others
        List<Spawned> spawnedList = plugin.findSpawned(zone);
        List<Vec3i> spawnedVectorList = new ArrayList<>(spawnedList.size());
        for (Spawned other : spawnedList) {
            if (other == spawned) continue;
            if (other.movingTo != null) spawnedVectorList.add(other.movingTo);
        }
        if (!spawnedVectorList.isEmpty()) {
            // Remove vectors too close to other spawneds!
            loadedBlockList.removeIf(it -> {
                    for (Vec3i otherVector : spawnedVectorList) {
                        if (otherVector.maxDistance(it) < 8) return true;
                    }
                    return false;
                });
            if (loadedBlockList.isEmpty()) return;
        }
        // Move!
        Vec3i targetVector = loadedBlockList.get(plugin.random.nextInt(loadedBlockList.size()));
        Block block = targetVector.toBlock(world);
        if (!canSpawnOnBlock(block)) return;
        spawned.movingTo = targetVector;
        Location location = block.getLocation().add(0.5, 1.0, 0.5);
        spawned.pathing = true;
        spawned.entity.getPathfinder().moveTo(location, 1.0);
        spawned.pathing = false;
    }

    public boolean contains(Location location) {
        return chunkBlockMap.containsKey(Vec2i.of(location.getBlockX() >> 4, location.getBlockZ() >> 4));
    }

    protected void talkTo(Spawned spawned, Player player) {
        spawned.entity.getPathfinder().stopPathfinding();
        spawned.moveCooldown = System.currentTimeMillis() + 5000L;
        spawned.entity.lookAt(player);
        if (spawned.messageIndex < 0 || spawned.messageIndex >= messageList.size()) return;
        String message = messageList.get(spawned.messageIndex);
        player.sendMessage(TextComponent.ofChildren(new Component[] {
                    Component.text("Villager: ", NamedTextColor.WHITE),
                    Component.text(message, NamedTextColor.GRAY),
                }));
    }
}
