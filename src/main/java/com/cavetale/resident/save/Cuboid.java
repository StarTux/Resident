package com.cavetale.resident.save;

import java.util.ArrayList;
import java.util.List;
import lombok.Value;

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
}
