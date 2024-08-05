package com.cavetale.resident.message;

import com.cavetale.core.font.Emoji;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.mobface.MobFace;
import com.cavetale.resident.Zoned;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import static com.cavetale.resident.ResidentPlugin.plugin;
import static com.cavetale.resident.message.BookmarkResolver.bookmarkResolver;
import static com.cavetale.resident.message.EmojiResolver.emojiResolver;
import static io.papermc.paper.registry.RegistryAccess.registryAccess;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

@Data
@RequiredArgsConstructor
public final class ZoneMessage {
    @ToString.Exclude
    private final Zoned zoned;
    private final String key;
    @ToString.Exclude
    private Component displayName;
    @ToString.Exclude
    private final List<Component> pages = new ArrayList<>();
    private Emoji icon;
    private EntityType entityType;
    // Villager
    private Villager.Profession villagerProfession;
    private Villager.Type villagerType;
    private int villagerLevel;
    private boolean baby;
    private double scale;

    public void load(ConfigurationSection config) {
        final var rawDisplayName = config.getString("DisplayName");
        displayName = rawDisplayName != null
            ? miniMessage().deserialize(rawDisplayName, emojiResolver(), bookmarkResolver())
            : null;
        for (String rawPage : config.getStringList("Pages")) {
            pages.add(miniMessage().deserialize(rawPage, emojiResolver(), bookmarkResolver()));
        }
        final var rawIcon = config.getString("Icon");
        if (rawIcon != null) {
            icon = Emoji.getEmoji(rawIcon.toLowerCase());
        }
        entityType = parseEnum(config, "EntityType", EntityType.class);
        if (config.isConfigurationSection("Villager")) {
            entityType = EntityType.VILLAGER;
            villagerProfession = parseKeyed(config, "Villager.Profession", RegistryKey.VILLAGER_PROFESSION);
            villagerType = parseKeyed(config, "Villager.Type", RegistryKey.VILLAGER_TYPE);
            villagerLevel = config.getInt("Villager.Level", 1);
        }
        baby = config.getBoolean("Baby", false);
        scale = config.getDouble("Scale", 0.0);
    }

    private <T extends Enum<T>> T parseEnum(ConfigurationSection config, String configKey, Class<T> enumClass) {
        final String raw = config.getString(configKey);
        if (raw == null) return null;
        try {
            return Enum.valueOf(enumClass, raw.toUpperCase());
        } catch (IllegalArgumentException iae) {
            plugin().getLogger().warning(zoned.getZone().getName() + ": " + key + ": Invalid " + configKey + ": " + raw);
            return null;
        }
    }

    private <T extends Keyed> T parseKeyed(ConfigurationSection config, String configKey, RegistryKey<T> registryKey) {
        final String raw = config.getString(configKey);
        if (raw == null) return null;
        final NamespacedKey theKey = NamespacedKey.fromString(raw);
        final T result = registryAccess().getRegistry(registryKey).get(theKey);
        if (result == null) {
            plugin().getLogger().warning(zoned.getZone().getName() + ": " + key + ": Invalid " + configKey + ": " + raw);
        }
        return null;
    }

    public void send(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        ComponentLike textIcon = icon != null
            ? icon.getComponent()
            : null;
        if (textIcon == null && entityType != null) {
            final var mobFace = MobFace.of(entityType);
            if (mobFace != null) {
                textIcon = mobFace.mytems;
            }
        }
        if (textIcon == null) {
            textIcon = Mytems.VILLAGER_FACE;
        }
        final List<Component> bookPages = new ArrayList<>();
        for (Component page : pages) {
            bookPages.add(join(noSeparators(),
                               textIcon,
                               space(),
                               (displayName != null
                                ? displayName
                                : text("Villager", DARK_GREEN)),
                               newline(), newline(),
                               page));
        }
        book.editMeta(m -> {
                BookMeta meta = (BookMeta) m;
                meta.author(text("Cavetale"));
                meta.title(text("Resident"));
                meta.pages(bookPages);
            });
        player.closeInventory();
        player.openBook(book);
    }

    public void applyEntity(Entity entity) {
        if (entity instanceof Villager villager) {
            if (villagerProfession != null) {
                villager.setProfession(villagerProfession);
            }
            if (villagerType != null) {
                villager.setVillagerType(villagerType);
            }
            if (villagerLevel >= 1 && villagerLevel <= 5) {
                villager.setVillagerLevel(villagerLevel);
            }
        }
        if (entity instanceof Ageable ageable) {
            if (baby) {
                ageable.setBaby();
            } else {
                ageable.setAdult();
            }
        }
        if (scale > 0.0 && entity instanceof Attributable attributable) {
            attributable.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(scale);
            mul(attributable, Attribute.GENERIC_MOVEMENT_SPEED, Math.sqrt(scale));
            mul(attributable, Attribute.GENERIC_FLYING_SPEED, scale);
            mul(attributable, Attribute.GENERIC_GRAVITY, scale);
            mul(attributable, Attribute.GENERIC_JUMP_STRENGTH, scale);
            mul(attributable, Attribute.GENERIC_STEP_HEIGHT, scale);
        }
    }

    private static void mul(Attributable attributable, Attribute attribute, double value) {
        final var instance = attributable.getAttribute(attribute);
        if (instance == null) return;
        instance.setBaseValue(instance.getBaseValue() * value);
    }
}
