package com.cavetale.resident;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

public enum ZoneType {
    NONE {
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, e -> {
                    prepMob(e, consumer);
                    Villager.Profession[] professions = Villager.Profession.values();
                    e.setProfession(professions[plugin.random.nextInt(professions.length)]);
                    Villager.Type[] types = Villager.Type.values();
                    e.setVillagerType(types[plugin.random.nextInt(types.length)]);
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                });
        }
    },
    SPAWN {
        private final List<Villager.Profession> professions = Stream.of(Villager.Profession.values())
            .filter(p -> p != Villager.Profession.NITWIT && p != Villager.Profession.NONE)
            .collect(Collectors.toList());
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, e -> {
                    prepMob(e, consumer);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(Villager.Type.PLAINS);
                    e.setVillagerLevel(1 + plugin.random.nextInt(3));
                });
        }
    },
    BAZAAR {
        private final List<Villager.Profession> professions = Stream.of(Villager.Profession.values())
            .filter(p -> p != Villager.Profession.NITWIT && p != Villager.Profession.NONE)
            .collect(Collectors.toList());
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, e -> {
                    prepMob(e, consumer);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(Villager.Type.DESERT);
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                });
        }
    },
    WITCH {
        private final List<Villager.Profession> professions = Stream.of(Villager.Profession.values())
            .filter(p -> p != Villager.Profession.NITWIT && p != Villager.Profession.NONE)
            .collect(Collectors.toList());
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, e -> {
                    prepMob(e, consumer);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(Villager.Type.SWAMP);
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                });
        }
    },
    DWARVEN {
        private final List<Villager.Profession> professions = List.of(Villager.Profession.ARMORER,
                                                                      Villager.Profession.CARTOGRAPHER,
                                                                      Villager.Profession.LIBRARIAN,
                                                                      Villager.Profession.MASON,
                                                                      Villager.Profession.TOOLSMITH,
                                                                      Villager.Profession.WEAPONSMITH);
        private final List<Villager.Type> types = List.of(Villager.Type.values());
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, e -> {
                    prepMob(e, consumer);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(types.get(plugin.random.nextInt(types.size())));
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                });
        }
    },
    HALLOWEEN {
        private final List<Villager.Profession> professions = List.of(Villager.Profession.NONE,
                                                                      Villager.Profession.NITWIT,
                                                                      Villager.Profession.CLERIC);
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, e -> {
                    prepMob(e, consumer);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(Villager.Type.PLAINS);
                    e.setVillagerLevel(1);
                    List<ItemStack> skulls = plugin.getHalloweenSkulls();
                    if (!skulls.isEmpty()) {
                        ItemStack skull = skulls.get(plugin.random.nextInt(skulls.size()));
                        e.getEquipment().setHelmet(skull);
                    }
                });
        }
    };

    abstract Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer);

    private static void prepMob(Mob mob, Consumer<Mob> cons) {
        mob.setPersistent(false);
        cons.accept(mob);
        Bukkit.getMobGoals().removeAllGoals(mob);
        AttributeInstance movementSpeed = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        movementSpeed.setBaseValue(0.5);
        for (AttributeModifier it : new ArrayList<>(movementSpeed.getModifiers())) {
            movementSpeed.removeModifier(it);
        }
        if (mob instanceof Villager) {
            Villager villager = (Villager) mob;
            villager.setRecipes(List.of());
        }
        mob.setSilent(true);
    }
}
