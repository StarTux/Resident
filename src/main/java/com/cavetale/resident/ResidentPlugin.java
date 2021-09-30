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
    protected Save save;
    protected final Map<Integer, Spawned> spawnedMap = new HashMap<>();
    protected final Map<String, Zoned> zonedMap = new HashMap<>();
    protected File saveFile;
    protected Random random = new Random();
    protected YamlConfiguration messagesConfig;
    protected final Map<UUID, Session> sessions = new HashMap<>();
    protected List<ItemStack> halloweenSkulls; // lazy loaded
    protected List<PluginSpawn> pluginSpawns = new ArrayList<>();

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveFile = new File(getDataFolder(), "save.json");
        residentCommand.enable();
        eventListener.enable();
        load();
        setupZones();
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, 20L);
    }

    @Override
    public void onDisable() {
        clear();
    }

    protected void load() {
        save = Json.load(saveFile, Save.class, Save::new);
        String messagePath = "messages.yml";
        File messageFile = new File(getDataFolder(), messagePath);
        if (!messageFile.exists()) {
            saveResource(messagePath, false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messageFile);
    }

    protected void setupZones() {
        clear();
        for (Zone zone : save.getZones()) {
            enableZone(zone);
        }
        clearPluginSpawns();
    }

    protected void enableZone(Zone zone) {
        @SuppressWarnings("unchecked")
        List<Object> objectList = (List<Object>) messagesConfig.getList(zone.getName());
        Zoned zoned = new Zoned(this, zone, ZoneMessageList.ofConfig(objectList));
        zonedMap.put(zone.getName(), zoned);
        zoned.updateSpawnBlocks();
    }

    protected void clear() {
        for (Zoned zoned : new ArrayList<>(zonedMap.values())) {
            zoned.disable();
        }
        zonedMap.clear();
        for (Spawned spawned : new ArrayList<>(spawnedMap.values())) {
            spawned.entity.remove();
        }
        spawnedMap.clear();
    }

    protected void save() {
        getDataFolder().mkdirs();
        Json.save(saveFile, save, true);
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
