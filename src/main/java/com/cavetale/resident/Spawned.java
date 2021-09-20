package com.cavetale.resident;

import com.cavetale.resident.save.Vec3i;
import com.cavetale.resident.save.Zone;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Mob;

/**
 * Data holder for a spawned mob.
 */
@RequiredArgsConstructor
public final class Spawned {
    protected final Mob entity;
    protected final Zone zone;
    protected final int messageIndex;
    protected long lastMoved;
    protected long moveCooldown;
    protected Vec3i movingTo;
    protected boolean pathing;
}
