package com.cavetale.resident;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;

public enum ZoneType {
    NONE {
        @Override protected Mob spawn(ResidentPlugin plugin, Location location) {
            return location.getWorld().spawn(location, Villager.class, e -> {
                    prepLiving(e);
                    Villager.Profession[] professions = Villager.Profession.values();
                    e.setProfession(professions[plugin.random.nextInt(professions.length)]);
                    Villager.Type[] types = Villager.Type.values();
                    e.setVillagerType(types[plugin.random.nextInt(types.length)]);
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                });
        }
    },
    SPAWN {
        private List<Villager.Profession> professions = Stream.of(Villager.Profession.values())
            .filter(p -> p != Villager.Profession.NITWIT)
            .collect(Collectors.toList());
        private List<Villager.Type> types = List.of(Villager.Type.values());
        @Override protected Mob spawn(ResidentPlugin plugin, Location location) {
            return location.getWorld().spawn(location, Villager.class, e -> {
                    prepLiving(e);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(types.get(plugin.random.nextInt(types.size())));
                    e.setVillagerLevel(1 + plugin.random.nextInt(3));
                });
        }
    },
    BAZAAR {
        private List<Villager.Profession> professions = Stream.of(Villager.Profession.values())
            .filter(p -> p != Villager.Profession.NITWIT)
            .collect(Collectors.toList());
        @Override protected Mob spawn(ResidentPlugin plugin, Location location) {
            return location.getWorld().spawn(location, Villager.class, e -> {
                    prepLiving(e);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(Villager.Type.DESERT);
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                });
        }
    },
    WITCH {
        private List<Villager.Profession> professions = Stream.of(Villager.Profession.values())
            .filter(p -> p != Villager.Profession.NITWIT)
            .collect(Collectors.toList());
        @Override protected Mob spawn(ResidentPlugin plugin, Location location) {
            return location.getWorld().spawn(location, Villager.class, e -> {
                    prepLiving(e);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(Villager.Type.SWAMP);
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                });
        }
    },
    DWARVEN {
        private List<Villager.Profession> professions = List.of(Villager.Profession.ARMORER,
                                                                Villager.Profession.CARTOGRAPHER,
                                                                Villager.Profession.LIBRARIAN,
                                                                Villager.Profession.MASON,
                                                                Villager.Profession.TOOLSMITH,
                                                                Villager.Profession.WEAPONSMITH);
        private List<Villager.Type> types = List.of(Villager.Type.values());
        @Override protected Mob spawn(ResidentPlugin plugin, Location location) {
            return location.getWorld().spawn(location, Villager.class, e -> {
                    prepLiving(e);
                    e.setProfession(professions.get(plugin.random.nextInt(professions.size())));
                    e.setVillagerType(types.get(plugin.random.nextInt(types.size())));
                    e.setVillagerLevel(1 + plugin.random.nextInt(5));
                });
        }
    };

    abstract Mob spawn(ResidentPlugin plugin, Location location);

    private static void prepLiving(LivingEntity living) {
        living.setPersistent(false);
        living.setRemoveWhenFarAway(true);
    }
}
