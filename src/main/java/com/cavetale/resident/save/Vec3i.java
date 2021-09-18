package com.cavetale.resident.save;

import lombok.Value;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

@Value
public final class Vec3i {
    static final Vec3i ZERO = new Vec3i(0, 0, 0);
    public final int x;
    public final int y;
    public final int z;

    public static Vec3i of(int x, int y, int z) {
        return new Vec3i(x, y, z);
    }

    public static Vec3i of(Block block) {
        return new Vec3i(block.getX(), block.getY(), block.getZ());
    }

    public static Vec3i of(Location location) {
        return new Vec3i(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public Block toBlock(World w) {
        return w.getBlockAt(x, y, z);
    }

    public Vec2i toChunk() {
        return Vec2i.of(x >> 4, z >> 4);
    }

    public int maxDistance(Vec3i other) {
        return Math.max(Math.abs(other.x - x),
                        Math.max(Math.abs(other.y - y),
                                 Math.abs(other.z - z)));
    }
}
