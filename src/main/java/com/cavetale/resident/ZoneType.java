package com.cavetale.resident;

import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.util.Entities;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Breedable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

public enum ZoneType {
    NONE {
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, false, e -> {
                    prepEntity(e);
                    Villager.Profession[] professions = Villager.Profession.values();
                    e.setProfession(professions[plugin.random.nextInt(professions.length)]);
                    Villager.Type[] types = Villager.Type.values();
                    e.setVillagerType(types[plugin.random.nextInt(types.length)]);
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                    consumer.accept(e);
                });
        }
    },
    SPAWN {
        private final List<Villager.Profession> professions = Stream.of(Villager.Profession.values())
            .filter(p -> p != Villager.Profession.NITWIT && p != Villager.Profession.NONE)
            .collect(Collectors.toList());
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, false, e -> {
                    prepEntity(e);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(Villager.Type.PLAINS);
                    e.setVillagerLevel(1 + plugin.random.nextInt(3));
                    consumer.accept(e);
                });
        }
    },
    BAZAAR {
        private final List<Villager.Profession> professions = Stream.of(Villager.Profession.values())
            .filter(p -> p != Villager.Profession.NITWIT && p != Villager.Profession.NONE)
            .collect(Collectors.toList());
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, false, e -> {
                    prepEntity(e);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(Villager.Type.DESERT);
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                    consumer.accept(e);
                });
        }
    },
    WITCH {
        private final List<Villager.Profession> professions = Stream.of(Villager.Profession.values())
            .filter(p -> p != Villager.Profession.NITWIT && p != Villager.Profession.NONE)
            .collect(Collectors.toList());
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, false, e -> {
                    prepEntity(e);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(Villager.Type.SWAMP);
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                    consumer.accept(e);
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
            return location.getWorld().spawn(location, Villager.class, false, e -> {
                    prepEntity(e);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(types.get(plugin.random.nextInt(types.size())));
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                    consumer.accept(e);
                });
        }
    },
    CHRISTMAS {
        private List<Villager.Profession> professions;

        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            if (this.professions == null) {
                EnumSet<Villager.Profession> set = EnumSet.allOf(Villager.Profession.class);
                set.remove(Villager.Profession.FARMER);
                this.professions = List.copyOf(set);
            }
            return location.getWorld().spawn(location, Villager.class, false, e -> {
                    prepEntity(e);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(Villager.Type.SNOW);
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                    ItemStack hat;
                    int roll = plugin.random.nextInt(2);
                    switch (roll) {
                    case 0: hat = Mytems.STOCKING_CAP.createItemStack(); break;
                    case 1: hat = Mytems.SANTA_HAT.createItemStack(); break;
                    default: throw new IllegalStateException("roll=" + roll);
                    }
                    e.getEquipment().setHelmet(hat);
                    consumer.accept(e);
                });
        }
    },
    HALLOWEEN {
        private final List<Villager.Profession> professions = List.of(Villager.Profession.NONE,
                                                                      Villager.Profession.NITWIT,
                                                                      Villager.Profession.CLERIC);
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, false, e -> {
                    prepEntity(e);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(Villager.Type.PLAINS);
                    e.setVillagerLevel(1);
                    List<ItemStack> skulls = plugin.getHalloweenSkulls();
                    if (!skulls.isEmpty()) {
                        ItemStack skull = skulls.get(plugin.random.nextInt(skulls.size()));
                        e.getEquipment().setHelmet(skull);
                    }
                    consumer.accept(e);
                });
        }
    },
    CHILDREN {
        @Override protected Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer) {
            return location.getWorld().spawn(location, Villager.class, false, e -> {
                    prepEntity(e);
                    e.setProfession(Villager.Profession.NONE);
                    Villager.Type[] types = Villager.Type.values();
                    e.setVillagerType(types[plugin.random.nextInt(types.length)]);
                    e.setBaby();
                    e.setAgeLock(true);
                    consumer.accept(e);
                });
        }
    },
    ;

    abstract Mob spawn(ResidentPlugin plugin, Location location, Consumer<Mob> consumer);

    public static void prepEntity(Entity entity) {
        entity.setPersistent(false);
        Entities.setTransient(entity);
        entity.setSilent(true);
        if (entity instanceof Attributable living) {
            AttributeInstance movementSpeed = living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            movementSpeed.setBaseValue(0.5);
            for (AttributeModifier it : new ArrayList<>(movementSpeed.getModifiers())) {
                movementSpeed.removeModifier(it);
            }
        }
        if (entity instanceof Mob mob) {
            mob.setRemoveWhenFarAway(true);
            Bukkit.getMobGoals().removeAllGoals(mob);
        }
        if (entity instanceof Villager villager) {
            villager.setRecipes(List.of());
        }
        if (entity instanceof Breedable breedable) {
            breedable.setAgeLock(true);
        }
    }
}
