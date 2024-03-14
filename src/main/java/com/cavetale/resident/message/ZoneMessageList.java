package com.cavetale.resident.message;

import com.cavetale.resident.Zoned;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.YamlConfiguration;
import static com.cavetale.resident.ResidentPlugin.plugin;

@Data
@RequiredArgsConstructor
public final class ZoneMessageList {
    private final Zoned zoned;
    private final Map<String, ZoneMessage> messages = new HashMap<>();

    public void load() {
        final String name = zoned.getZone().getName();
        final File file = new File(plugin().getMessagesFolder(), name + ".yml");
        if (!file.exists()) return;
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            if (!config.isConfigurationSection(key)) continue;
            ZoneMessage message = new ZoneMessage(zoned, key);
            message.load(config.getConfigurationSection(key));
            messages.put(key, message);
        }
    }

    public int size() {
        return messages.size();
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public ZoneMessage get(String key) {
        return messages.get(key);
    }
}
