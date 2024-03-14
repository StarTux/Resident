package com.cavetale.resident;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.resident.message.ZoneMessage;
import com.cavetale.resident.message.ZoneMessageList;
import com.cavetale.resident.save.Zone;
import com.destroystokyo.paper.entity.Pathfinder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * This class represents the runtime object of a Zone.
 */
@Getter
@RequiredArgsConstructor
public final class Zoned {
    protected static final int SPAWN_DISTANCE = 8;
    protected static final int PLAYER_DISTANCE = 24;
    protected static final int MIN_MOVE_DISTANCE = 4;
    protected static final int MAX_MOVE_DISTANCE = 32;
    protected final ResidentPlugin plugin;
    protected final Zone zone;
    protected ZoneMessageList messageList;
    protected final Set<Vec3i> spawnBlocks = new HashSet<>();
    protected final Set<Vec3i> loadedSpawnBlocks = new HashSet<>();
    protected final Map<Vec2i, Set<Vec3i>> chunkBlockMap = new HashMap<>();
    protected int updateId = 0;
    protected int total = 0;
    protected boolean disabled;

    public World getWorld() {
        return Bukkit.getWorld(zone.getWorld());
    }

    protected void enable() {
        messageList = new ZoneMessageList(this);
        messageList.load();
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
            vectorSet.addAll(cuboid.enumerate());
        }
        Map<Vec2i, Set<Vec3i>> chunkBlocks = new HashMap<>();
        for (Vec3i vector : vectorSet) {
            Set<Vec3i> set = chunkBlocks.computeIfAbsent(vector.blockToChunk(), u -> new HashSet<>());
            set.add(vector);
        }
        for (Map.Entry<Vec2i, Set<Vec3i>> entry : chunkBlocks.entrySet()) {
            Vec2i chunkVector = entry.getKey();
            Set<Vec3i> chunkVectorSet = entry.getValue();
            computeChunkSpawnBlocks(world, chunkVector, chunkVectorSet, updateId);
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

    private void computeChunkSpawnBlocks(World world, Vec2i chunkVector, Set<Vec3i> chunkVectorSet, final int id) {
        world.getChunkAtAsync(chunkVector.x, chunkVector.z, (Consumer<Chunk>) c -> {
                if (id != this.updateId || disabled) return;
                chunkVectorSet.removeIf(vector -> !canSpawnOnBlock(vector.toBlock(world)));
                this.chunkBlockMap.put(chunkVector, chunkVectorSet);
                this.spawnBlocks.addAll(chunkVectorSet);
                if (world.isChunkLoaded(chunkVector.x, chunkVector.z)) {
                    this.loadedSpawnBlocks.addAll(chunkVectorSet);
                }
            });
    }

    private Set<Vec3i> computePlayerVectorSet(World world) {
        Set<Vec3i> result = new HashSet<>();
        for (Player player : world.getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            result.add(Vec3i.of(player.getLocation()));
        }
        return result;
    }

    protected void spawn() {
        if (loadedSpawnBlocks.isEmpty()) {
            total = -2;
            return;
        }
        World world = getWorld();
        if (world == null) {
            total = -3;
            return;
        }
        int count = plugin.countSpawned(zone);
        if (count >= zone.getMaxResidents()) return;
        List<Vec3i> loadedBlockList = new ArrayList<>(loadedSpawnBlocks);
        if (loadedBlockList.isEmpty()) {
            total = -1;
            return;
        }
        // Total
        total = (int) Math.ceil((double) zone.getMaxResidents()
                                * (((double) loadedBlockList.size())
                                   / ((double) spawnBlocks.size())));
        final int difference = total - count;
        if (difference < 1) return;
        // Do not spawn too close to players
        final Set<Vec3i> playerVectorSet = computePlayerVectorSet(world);
        loadedBlockList.removeIf(it -> {
                for (Vec3i playerVector : playerVectorSet) {
                    return it.maxDistance(playerVector) < PLAYER_DISTANCE;
                }
                return false;
            });
        if (loadedBlockList.isEmpty()) return;
        // Do not spawn near others
        List<Spawned> existingList = plugin.findSpawned(zone);
        loadedBlockList.removeIf(it -> {
                for (Spawned existing : existingList) {
                    if (existing.getEntityVector().maxDistance(it) < SPAWN_DISTANCE) return true;
                }
                return false;
            });
        if (loadedBlockList.isEmpty()) return;
        // Spawn!
        for (int i = 0; i < difference; i += 1) {
            Vec3i blockVector = loadedBlockList.remove(plugin.random.nextInt(loadedBlockList.size()));
            Vec2i chunkVector = blockVector.blockToChunk();
            if (!world.isChunkLoaded(chunkVector.x, chunkVector.z)) {
                plugin.getLogger().warning(zone.getName() + ": Chunk not loaded: " + chunkVector);
                Set<Vec3i> chunkBlocks = chunkBlockMap.get(chunkVector);
                if (chunkBlocks != null) {
                    loadedSpawnBlocks.removeAll(chunkBlocks);
                }
                return;
            }
            Block block = blockVector.toBlock(world);
            if (!canSpawnOnBlock(block)) continue;
            spawn(block.getLocation().add(0.5, 1.0, 0.5));
            loadedBlockList.removeIf(it -> blockVector.maxDistance(it) < SPAWN_DISTANCE);
            if (loadedBlockList.isEmpty()) break;
        }
    }

    protected void spawn(Location location) {
        if (zone.getType() == null) return;
        final String messageKey;
        if (!messageList.isEmpty()) {
            // Find an unused message key
            final List<String> keys = new ArrayList<>(messageList.size());
            keys.addAll(messageList.getMessages().keySet());
            for (Spawned existing : plugin.findSpawned(zone)) {
                if (existing.messageKey == null) continue;
                keys.remove(existing.messageKey);
            }
            messageKey = !keys.isEmpty()
                ? keys.get(plugin.random.nextInt(keys.size()))
                : null;
        } else {
            messageKey = null;
        }
        final ZoneMessage message = messageKey != null
            ? messageList.get(messageKey)
            : null;
        final Mob mob;
        if (message != null && message.getEntityType() != null) {
            var tmp = location.getWorld().spawn(location, message.getEntityType().getEntityClass(), false, e -> {
                    ZoneType.prepEntity(e);
                    message.applyEntity(e);
                });
            if (!(tmp instanceof Mob m)) {
                plugin.getLogger().severe("EntityType not a Mob: " + message.getEntityType());
                tmp.remove();
                return;
            }
            mob = m;
        } else {
            mob = zone.getType().spawn(plugin, location, e -> { });
        }
        if (message != null && message.getDisplayName() != null) {
            mob.customName(message.getDisplayName());
        }
        final int entityId = mob.getEntityId();
        final Spawned spawned = new Spawned(mob, zone, messageKey);
        plugin.spawnedMap.put(entityId, spawned);
        spawned.movingTo = Vec3i.of(location);
        mob.setMetadata("nomap", new FixedMetadataValue(plugin, true));
        if (plugin.aprilFools) spawned.makeAprilFools();
    }

    protected void move() {
        World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) return;
        List<Spawned> spawnedList = plugin.findSpawned(zone);
        if (spawnedList.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Spawned spawned : spawnedList) {
            if (spawned.lastMoved > now - 2000L) continue;
            if (spawned.moveCooldown > now) continue;
            spawned.moveCooldown = now + 5000L;
            move(spawned, world);
        }
    }

    private void move(Spawned spawned, World world) {
        Vec3i mobVector = Vec3i.of(spawned.entity.getLocation());
        if (!spawnBlocks.contains(mobVector) && !spawnBlocks.contains(mobVector.add(0, -1, 0))) {
            spawned.remove();
            return;
        }
        List<Vec3i> loadedBlockList = new ArrayList<>(loadedSpawnBlocks);
        loadedBlockList.removeIf(blockVector -> {
                int distance = blockVector.maxDistance(mobVector);
                return distance < MIN_MOVE_DISTANCE || distance > MAX_MOVE_DISTANCE;
            });
        if (loadedBlockList.isEmpty()) {
            spawned.remove(); // modifies spawnedMap?
            return;
        }
        // Move!
        Vec3i targetVector = loadedBlockList.get(plugin.random.nextInt(loadedBlockList.size()));
        Block block = targetVector.toBlock(world);
        if (!canSpawnOnBlock(block)) return;
        Location location = block.getLocation().add(0.5, 1.0, 0.5);
        spawned.pathing = true;
        if (null != findPath(spawned.entity, location)) {
            spawned.movingTo = targetVector;
        }
        spawned.pathing = false;
    }

    private Pathfinder.PathResult findPath(Mob entity, Location target) {
        Pathfinder pathfinder = entity.getPathfinder();
        Pathfinder.PathResult pathResult = pathfinder.findPath(target);
        if (pathResult == null) return null;
        for (Location location : pathResult.getPoints()) {
            Block b1 = location.getBlock();
            if (b1.isLiquid()) return null;
            Block b2 = b1.getRelative(0, -1, 0);
            if (b2.isLiquid()) return null;
        }
        pathfinder.moveTo(pathResult, 0.5);
        return pathResult;
    }

    public boolean contains(Location location) {
        return chunkBlockMap.containsKey(Vec2i.of(location.getBlockX() >> 4, location.getBlockZ() >> 4));
    }

    protected void talkTo(Spawned spawned, Player player) {
        // Cooldown
        long now = System.currentTimeMillis();
        Session session = plugin.session(player);
        if (session.talkCooldown > now) return;
        session.talkCooldown = now + 1000L;
        // Stop Entity
        spawned.entity.getPathfinder().stopPathfinding();
        spawned.moveCooldown = now + 5000L;
        spawned.entity.lookAt(player);
        player.playSound(spawned.entity.getEyeLocation(), Sound.ENTITY_VILLAGER_TRADE, SoundCategory.NEUTRAL,
                         1.0f, 1.0f);
        // Message
        if (spawned.messageKey == null) return;
        ZoneMessage message = messageList.get(spawned.messageKey);
        message.send(player);
    }

    protected void onChunkLoad(String chunkWorld, Vec2i chunkVector, boolean loaded) {
        if (!chunkWorld.equals(zone.getWorld())) return;
        Set<Vec3i> chunkBlocks = chunkBlockMap.get(chunkVector);
        if (chunkBlocks == null) return;
        if (loaded) {
            loadedSpawnBlocks.addAll(chunkBlocks);
        } else {
            loadedSpawnBlocks.removeAll(chunkBlocks);
        }
    }
}
