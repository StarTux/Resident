package com.cavetale.resident;

import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Entities;
import com.cavetale.resident.save.Zone;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Data holder for a spawned mob.
 */
@RequiredArgsConstructor
public final class Spawned {
    protected final Mob entity;
    protected final Zone zone;
    protected final int messageIndex;
    protected long lastMoved;
    protected Vec3i lastMovedVec;
    protected long moveCooldown;
    protected Vec3i movingTo;
    protected boolean pathing;
    protected PluginSpawn pluginSpawn;
    // April Fools
    private ItemDisplay itemDisplay;
    private BukkitTask task;

    public Vec3i getEntityVector() {
        return movingTo != null
            ? movingTo
            : Vec3i.of(entity.getLocation());
    }

    public boolean hasZone() {
        return !zone.isNull();
    }

    public void remove() {
        if (entity != null) {
            entity.remove();
        }
        if (itemDisplay != null) {
            itemDisplay.remove();
            itemDisplay = null;
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    protected void makeAprilFools() {
        entity.setInvisible(true);
        itemDisplay = entity.getWorld().spawn(entity.getLocation(), ItemDisplay.class, e -> {
                e.setPersistent(false);
                Entities.setTransient(e);
                e.setItemStack(Mytems.APRIL_FOOLS.createItemStack());
                e.setBillboard(Display.Billboard.VERTICAL);
                e.setBrightness(new Display.Brightness(15, 15));
                e.setTransformation(new Transformation(new Vector3f(0f, 1.0f, 0f),
                                                       new AxisAngle4f(0f, 0f, 0f, 0f),
                                                       new Vector3f(2f, 2f, 0f),
                                                       new AxisAngle4f(0f, 0f, 0f, 0f)));
            });
        task = Bukkit.getScheduler().runTaskTimer(ResidentPlugin.instance, () -> {
                if (itemDisplay == null) return;
                itemDisplay.teleport(entity.getLocation());
            }, 1L, 1L);
    }
}
