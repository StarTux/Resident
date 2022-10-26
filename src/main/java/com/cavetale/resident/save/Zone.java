package com.cavetale.resident.save;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.resident.ZoneType;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.bukkit.Location;

/**
 * Saved as part of Save.
 */
@Data
public final class Zone {
    public static final Zone NULL = new Zone("", "");
    protected String name;
    protected ZoneType type = ZoneType.NONE;
    protected String world;
    protected final List<Cuboid> regions = new ArrayList<>();
    protected int maxResidents = 10;

    public Zone() { }

    public Zone(final String name, final String world) {
        this.name = name;
        this.world = world;
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(world)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        for (Cuboid cuboid : regions) {
            if (cuboid.contains(x, y, z)) return true;
        }
        return false;
    }

    public boolean isNull() {
        return equals(NULL);
    }
}
