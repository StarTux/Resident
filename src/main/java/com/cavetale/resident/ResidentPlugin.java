package com.cavetale.resident;

import com.cavetale.core.util.Json;
import com.cavetale.resident.save.Zone;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class ResidentPlugin extends JavaPlugin {
    protected static ResidentPlugin instance;
    protected ResidentCommand residentCommand = new ResidentCommand(this);
    protected EventListener eventListener = new EventListener(this);
    protected final Map<Integer, Spawned> spawnedMap = new HashMap<>();
    protected final Map<String, Zoned> zonedMap = new HashMap<>();
    protected File zonesFolder;
    protected File messagesFolder;
    protected Random random = new Random();
    protected final Map<UUID, Session> sessions = new HashMap<>();
    private List<ItemStack> halloweenSkulls; // lazy loaded
    protected List<PluginSpawn> pluginSpawns = new ArrayList<>();
    protected boolean aprilFools;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        zonesFolder = new File(getDataFolder(), "zones");
        zonesFolder.mkdirs();
        messagesFolder = new File(getDataFolder(), "messages");
        messagesFolder.mkdirs();
        residentCommand.enable();
        eventListener.enable();
        loadZones();
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
        // April Fools
        Instant instant = Instant.now();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC-11"));
        LocalDate localDate = localDateTime.toLocalDate();
        final int month = localDate.getMonth().getValue();
        final int day = localDate.getDayOfMonth();
        aprilFools = month == 4 && day == 1;
        getLogger().info("month:" + month + " day:" + day + " fool:" + aprilFools);
    }

    @Override
    public void onDisable() {
        clearZones();
        clearPluginSpawns();
    }


    protected void loadZones() {
        clearZones();
        List<Zone> zones = new ArrayList<>();
        for (File zoneFile : zonesFolder.listFiles()) {
            if (!zoneFile.isFile()) continue;
            String name = zoneFile.getName();
            if (!name.endsWith(".json")) continue;
            name = name.substring(0, name.length() - 5);
            Zone zone = Json.load(zoneFile, Zone.class);
            if (zone == null) {
                getLogger().warning("Invalid zone file: " + zoneFile);
                continue;
            }
            zone.setName(name);
            zones.add(zone);
        }
        String messagePath = "messages.yml";
        File messageFile = new File(getDataFolder(), messagePath);
        if (!messageFile.exists()) {
            saveResource(messagePath, false);
        }
        for (Zone zone : zones) {
            enableZone(zone);
        }
    }

    protected void saveZone(Zone zone) {
        if (zone.isNull()) throw new IllegalArgumentException("Zone is null!");
        zonesFolder.mkdirs();
        File zoneFile = new File(zonesFolder, zone.getName() + ".json");
        Json.save(zoneFile, zone, true);
    }

    protected void clearZones() {
        for (Zoned zoned : new ArrayList<>(zonedMap.values())) {
            zoned.disable();
        }
        zonedMap.clear();
        for (Spawned spawned : new ArrayList<>(spawnedMap.values())) {
            if (spawned.hasZone()) {
                spawned.remove();
            }
        }
    }

    protected void enableZone(Zone zone) {
        Zoned zoned = new Zoned(this, zone);
        zoned.enable();
        zonedMap.put(zone.getName(), zoned);
        zoned.updateSpawnBlocks();
    }

    public List<Spawned> findSpawned(Zone zone) {
        List<Spawned> result = new ArrayList<>();
        for (Spawned spawned : new ArrayList<>(spawnedMap.values())) {
            if (Objects.equals(zone.getName(), spawned.zone.getName())) {
                result.add(spawned);
            }
        }
        return result;
    }

    public int countSpawned(Zone zone) {
        int result = 0;
        for (Spawned spawned : new ArrayList<>(spawnedMap.values())) {
            if (Objects.equals(zone.getName(), spawned.zone.getName())) {
                result += 1;
            }
        }
        return result;
    }

    private void tick() {
        for (Zoned zoned : zonedMap.values()) {
            zoned.spawn();
            zoned.move();
        }
        for (PluginSpawn pluginSpawn : pluginSpawns) {
            pluginSpawn.spawn();
        }
    }

    protected Session session(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), uuid -> new Session());
    }

    public ItemStack makeSkull(String name, String texture, String signature) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        item.editMeta(m -> {
                SkullMeta meta = (SkullMeta) m;
                PlayerProfile profile = Bukkit.getServer().createProfile(UUID.randomUUID(), name);
                ProfileProperty prop = new ProfileProperty("textures", texture, signature);
                profile.setProperty(prop);
                meta.setPlayerProfile(profile);
            });
        return item;
    }

    protected List<ItemStack> getHalloweenSkulls() {
        if (halloweenSkulls == null) {
            getLogger().info("Loading halloween skulls");
            List<ItemStack> result = new ArrayList<>();
            InputStreamReader input = new InputStreamReader(getResource("halloween.yml"));
            YamlConfiguration config = YamlConfiguration.loadConfiguration(input);
            for (String key : config.getKeys(false)) {
                String texture = config.getString(key + ".texture");
                if (texture == null) {
                    getLogger().info("Texture key is null: " + key);
                    continue;
                }
                String signature = config.getString(key + ".signature");
                ItemStack skull = makeSkull(key, texture, signature);
                result.add(skull);
            }
            halloweenSkulls = result;
        }
        return halloweenSkulls;
    }

    protected void clearPluginSpawns() {
        for (PluginSpawn pluginSpawn : pluginSpawns) {
            pluginSpawn.despawn();
        }
        pluginSpawns.clear();
    }

    protected void clearPluginSpawns(Plugin plugin) {
        List<PluginSpawn> removePluginSpawns = new ArrayList<>();
        for (PluginSpawn pluginSpawn : pluginSpawns) {
            if (pluginSpawn.plugin == plugin) {
                removePluginSpawns.add(pluginSpawn);
            }
        }
        for (PluginSpawn pluginSpawn : removePluginSpawns) {
            pluginSpawn.despawn();
            pluginSpawns.remove(pluginSpawn);
        }
    }

    public static ResidentPlugin plugin() {
        return instance;
    }
}
