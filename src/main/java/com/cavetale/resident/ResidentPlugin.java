package com.cavetale.resident;

import com.cavetale.core.util.Json;
import com.cavetale.resident.message.ZoneMessageList;
import com.cavetale.resident.save.Save;
import com.cavetale.resident.save.Zone;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class ResidentPlugin extends JavaPlugin {
    protected static ResidentPlugin instance;
    protected ResidentCommand residentCommand = new ResidentCommand(this);
    protected EventListener eventListener = new EventListener(this);
    protected final Map<Integer, Spawned> spawnedMap = new HashMap<>();
    protected final Map<String, Zoned> zonedMap = new HashMap<>();
    protected File legacySaveFile;
    protected File zonesFolder;
    protected Random random = new Random();
    protected YamlConfiguration messagesConfig;
    protected final Map<UUID, Session> sessions = new HashMap<>();
    private List<ItemStack> halloweenSkulls; // lazy loaded
    protected List<PluginSpawn> pluginSpawns = new ArrayList<>();

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        legacySaveFile = new File(getDataFolder(), "save.json");
        zonesFolder = new File(getDataFolder(), "zones");
        zonesFolder.mkdirs();
        residentCommand.enable();
        eventListener.enable();
        loadZones();
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
    }

    @Override
    public void onDisable() {
        clearZones();
        clearPluginSpawns();
    }


    protected void loadZones() {
        clearZones();
        List<Zone> zones = new ArrayList<>();
        // Legacy
        if (legacySaveFile.exists()) {
            getLogger().info("Loading and deleting legacy save file...");
            Save save = Json.load(legacySaveFile, Save.class);
            if (save != null) {
                for (Zone zone : save.getZones()) {
                    zones.add(zone);
                    saveZone(zone);
                }
            }
            legacySaveFile.delete();
        }
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
        messagesConfig = YamlConfiguration.loadConfiguration(messageFile);
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
                spawned.entity.remove();
            }
        }
    }

    protected void enableZone(Zone zone) {
        @SuppressWarnings("unchecked")
        List<Object> objectList = (List<Object>) messagesConfig.getList(zone.getName());
        Zoned zoned = new Zoned(this, zone, ZoneMessageList.ofConfig(objectList));
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
}
