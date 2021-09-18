package com.cavetale.resident;

import com.cavetale.resident.save.Cuboid;
import com.cavetale.resident.save.Vec2i;
import com.cavetale.resident.save.Vec3i;
import com.cavetale.resident.save.Zone;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;

/**
 * This class represents the runtime object of a Zone.
 */
@RequiredArgsConstructor
public final class Zoned {
    protected final ResidentPlugin plugin;
    protected final Zone zone;
    protected final List<String> messageList;
    protected final Set<Vec3i> spawnBlocks = new HashSet<>();
    protected final Map<Vec2i, Set<Vec3i>> chunkBlockMap = new HashMap<>();
    protected int updateId = 0;
    protected boolean disabled;

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
                for (Vec3i vector : vectorSet) {
                    Block block = vector.toBlock(world);
                    if (canSpawnOnBlock(block)) {
                        spawnBlocks.add(vector);
                        if (spawnBlocks.size() > 100000) return; // Magic number
                    }
                }
            });
    }

    private List<Vec3i> computeLoadedBlockList(World world) {
        List<Vec3i> loadedBlockList = new ArrayList<>();
        for (Map.Entry<Vec2i, Set<Vec3i>> entry : chunkBlockMap.entrySet()) {
            Vec2i chunkVector = entry.getKey();
            if (!world.isChunkLoaded(chunkVector.x, chunkVector.y)) continue;
            loadedBlockList.addAll(entry.getValue());
        }
        return loadedBlockList;
    }

    protected void spawn() {
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;
        int count = plugin.countSpawned(zone);
        if (count >= Math.min(messageList.size(), zone.getMaxResidents())) return;
        List<Vec3i> loadedBlockList = computeLoadedBlockList(world);
        if (loadedBlockList.isEmpty()) return;
        Vec3i blockVector = loadedBlockList.get(plugin.random.nextInt(loadedBlockList.size()));
        Block block = blockVector.toBlock(world);
        if (!canSpawnOnBlock(block)) return;
        spawn(block.getLocation().add(0.5, 1.0, 0.5));
    }

    protected void spawn(Location location) {
        if (zone.getType() == null) return;
        int messageIndex = -1;
        if (!messageList.isEmpty()) {
            boolean[] usedIndexes = new boolean[messageList.size()];
            for (Spawned existing : plugin.findSpawned(zone)) {
                if (existing.messageIndex < usedIndexes.length) {
                    usedIndexes[existing.messageIndex] = true;
                }
            }
            int lowest = -1;
            for (int i = 0; i < usedIndexes.length; i += 1) {
                if (!usedIndexes[i]) {
                    lowest = i;
                    break;
                }
            }
            if (lowest >= 0) messageIndex = lowest;
        }
        Mob entity = zone.getType().spawn(plugin, location);
        if (entity == null || entity.isDead()) return;
        int entityId = entity.getEntityId();
        plugin.spawnedMap.put(entity.getEntityId(), new Spawned(entity, zone, messageIndex));
        Bukkit.getMobGoals().removeAllGoals(entity);
    }

    protected void move() {
        long now = System.currentTimeMillis();
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;
        long then = now - 5000L;
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
        List<Vec3i> loadedBlockList = computeLoadedBlockList(world);
        loadedBlockList.removeIf(blockVector -> {
                int distance = blockVector.maxDistance(mobVector);
                return distance < 8 || distance > 24;
            });
        if (loadedBlockList.isEmpty()) {
            spawned.entity.remove(); // modifies spawnedMap?
            return;
        }
        Vec3i targetVector = loadedBlockList.get(plugin.random.nextInt(loadedBlockList.size()));
        Block block = targetVector.toBlock(world);
        if (!canSpawnOnBlock(block)) return;
        Location location = block.getLocation().add(0.5, 1.0, 0.5);
        spawned.entity.getPathfinder().moveTo(location);
    }

    public boolean contains(Location location) {
        return chunkBlockMap.containsKey(Vec2i.of(location.getBlockX() >> 4, location.getBlockZ() >> 4));
    }
}
