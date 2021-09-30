package com.cavetale.resident.save;

import lombok.Value;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

@Value
public final class Loc {
    public final String world;
    public final double x;
    public final double y;
    public final double z;
    public final float yaw;
    public final float pitch;

    public static Loc of(Location location) {
        return new Loc(location.getWorld().getName(),
                       location.getX(), location.getY(), location.getZ(),
                       location.getYaw(), location.getPitch());
    }

    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public Vec2i toChunk() {
        return Vec2i.of(getBlockX() >> 4, getBlockZ() >> 4);
    }

    public int getBlockX() {
        return (int) Math.floor(x);
    }

    public int getBlockY() {
        return (int) Math.floor(y);
    }

    public int getBlockZ() {
        return (int) Math.floor(z);
    }

    public String toString() {
        return String.format("%s,%.1f,%.1f,%.1f:%.1f/%.1f",
                             world, x, y, z, yaw, pitch);
    }
}
