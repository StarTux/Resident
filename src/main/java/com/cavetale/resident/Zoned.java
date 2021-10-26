package com.cavetale.resident;

import com.cavetale.core.font.VanillaItems;
import com.cavetale.resident.message.ZoneMessage;
import com.cavetale.resident.message.ZoneMessageList;
import com.cavetale.resident.save.Cuboid;
import com.cavetale.resident.save.Vec2i;
import com.cavetale.resident.save.Vec3i;
import com.cavetale.resident.save.Zone;
import com.destroystokyo.paper.entity.Pathfinder;
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
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
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

/**
 * This class represents the runtime object of a Zone.
 */
@RequiredArgsConstructor
public final class Zoned {
    protected static final int SPAWN_DISTANCE = 8;
    protected static final int PLAYER_DISTANCE = 24;
    protected static final int MIN_MOVE_DISTANCE = 4;
    protected static final int MAX_MOVE_DISTANCE = 32;
    protected final ResidentPlugin plugin;
    protected final Zone zone;
    protected final ZoneMessageList messageList;
    protected final Set<Vec3i> spawnBlocks = new HashSet<>();
    protected final Set<Vec3i> loadedSpawnBlocks = new HashSet<>();
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
                    } else {
                        spawnBlocks.add(vector);
                        loadedSpawnBlocks.add(vector);
                        if (spawnBlocks.size() > 100000) return; // Magic number
                    }
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
            List<Integer> indexOptions = new ArrayList<>(usedIndexes.length);
            for (int i = 0; i < usedIndexes.length; i += 1) {
                if (usedIndexes[i] < minValue) {
                    minValue = usedIndexes[i];
                    indexOptions.clear();
                    indexOptions.add(i);
                } else if (usedIndexes[i] == minValue) {
                    indexOptions.add(i);
                }
            }
            messageIndex = indexOptions.get(plugin.random.nextInt(indexOptions.size()));
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
            spawned.entity.remove();
            return;
        }
        List<Vec3i> loadedBlockList = new ArrayList<>(loadedSpawnBlocks);
        loadedBlockList.removeIf(blockVector -> {
                int distance = blockVector.maxDistance(mobVector);
                return distance < MIN_MOVE_DISTANCE || distance > MAX_MOVE_DISTANCE;
            });
        if (loadedBlockList.isEmpty()) {
            spawned.entity.remove(); // modifies spawnedMap?
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
            Vec3i vec = Vec3i.of(location);
            if (!spawnBlocks.contains(vec) && !spawnBlocks.contains(vec.add(0, -1, 0))) {
                return null;
            }
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
        if (spawned.messageIndex < 0 || spawned.messageIndex >= messageList.size()) return;
        ZoneMessage message = messageList.get(spawned.messageIndex);
        if (session.talkingTo == spawned) {
            session.lineIndex += 1;
            if (session.lineIndex >= message.size()) {
                session.lineIndex = 0;
            }
        } else {
            session.talkingTo = spawned;
            session.lineIndex = 0;
        }
        player.sendMessage(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                    VanillaItems.EMERALD.component,
                    Component.text("Villager: ", NamedTextColor.GRAY),
                    Component.text(message.getLine(session.lineIndex), NamedTextColor.WHITE),
                }));
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
