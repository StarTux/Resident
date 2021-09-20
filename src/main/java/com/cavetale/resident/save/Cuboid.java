package com.cavetale.resident.save;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

@Value
public final class Cuboid {
    static final Cuboid ZERO = new Cuboid(Vec3i.ZERO, Vec3i.ZERO);
    public final Vec3i min;
    public final Vec3i max;

    public boolean contains(int x, int y, int z) {
        return x >= min.x && x <= max.x
            && y >= min.y && y <= max.y
            && z >= min.z && z <= max.z;
    }

    public List<Vec3i> all() {
        List<Vec3i> result = new ArrayList<>();
        for (int y = min.y; y <= max.y; y += 1) {
            for (int z = min.z; z <= max.z; z += 1) {
                for (int x = min.x; x <= max.x; x += 1) {
                    result.add(Vec3i.of(x, y, z));
                    if (result.size() > 10000) return result; // Magic number
                }
            }
        }
        return result;
    }

    public boolean contains(Cuboid other) {
        return other.min.gte(min)
            && max.gte(other.max);
    }

    public void highlight(World world, Player player) {
        if (!world.equals(player.getWorld())) return;
        highlight(world, location -> {
                player.spawnParticle(Particle.END_ROD, location, 1, 0.0, 0.0, 0.0, 0.0);
            });
    }

    public void highlight(World world, Consumer<Location> callback) {
        if (!world.isChunkLoaded(min.x >> 4, min.z >> 4)) return;
        if (!world.isChunkLoaded(max.x >> 4, max.z >> 4)) return;
        Block a = min.toBlock(world);
        Block b = max.toBlock(world);
        final int ax = a.getX();
        final int ay = a.getY();
        final int az = a.getZ();
        final int bx = b.getX();
        final int by = b.getY();
        final int bz = b.getZ();
        Location loc = a.getLocation();
        int sizeX = bx - ax + 1;
        int sizeY = by - ay + 1;
        int sizeZ = bz - az + 1;
        for (int y = 0; y < sizeY; y += 1) {
            double dy = (double) y;
            callback.accept(loc.clone().add(0, dy, 0));
            callback.accept(loc.clone().add(0, dy, sizeZ));
            callback.accept(loc.clone().add(sizeX, dy, 0));
            callback.accept(loc.clone().add(sizeX, dy, sizeZ));
        }
        for (int z = 0; z < sizeZ; z += 1) {
            double dz = (double) z;
            callback.accept(loc.clone().add(0, 0, dz));
            callback.accept(loc.clone().add(0, sizeY, dz));
            callback.accept(loc.clone().add(sizeX, 0, dz));
            callback.accept(loc.clone().add(sizeX, sizeY, dz));
        }
        for (int x = 0; x < sizeX; x += 1) {
            double dx = (double) x;
            callback.accept(loc.clone().add(dx, 0, 0));
            callback.accept(loc.clone().add(dx, 0, sizeZ));
            callback.accept(loc.clone().add(dx, sizeY, 0));
            callback.accept(loc.clone().add(dx, sizeY, sizeZ));
        }
    }
}
