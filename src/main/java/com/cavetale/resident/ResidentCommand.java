package com.cavetale.resident;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandContext;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.resident.save.Cuboid;
import com.cavetale.resident.save.Zone;
import com.cavetale.resident.util.WorldEdit;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ResidentCommand extends AbstractCommand<ResidentPlugin> {
    protected ResidentCommand(final ResidentPlugin plugin) {
        super(plugin, "resident");
    }

    @Override
    public void onEnable() {
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload save file")
            .senderCaller(this::reload);
        CommandNode zoneNode = rootNode.addChild("zone")
            .description("Zone commands");
        zoneNode.addChild("create").arguments("<name>").denyTabCompletion()
            .description("Create a new zone")
            .playerCaller(this::zoneCreate);
        zoneNode.addChild("info").arguments("<name>")
            .description("Print zone info")
            .completers(this::completeZoneNames)
            .senderCaller(this::zoneInfo);
        zoneNode.addChild("type").arguments("<name> <type>")
            .completers(this::completeZoneNames, CommandArgCompleter.enumLowerList(ZoneType.class))
            .senderCaller(this::zoneType);
        zoneNode.addChild("max").arguments("<name> <amount>")
            .completers(this::completeZoneNames, CommandArgCompleter.integer(i -> i > 0))
            .senderCaller(this::zoneMax);
        CommandNode zoneRegionNode = zoneNode.addChild("region")
            .description("Zone region commands");
        zoneRegionNode.addChild("add").arguments("<zone>")
            .description("Add WorldEdit selection to zone")
            .completers(this::completeZoneNames)
            .playerCaller(this::zoneRegionAdd);
    }

    private boolean reload(CommandSender sender, String[] args) {
        plugin.load();
        plugin.setupZones();
        sender.sendMessage(Component.text("Save file reloaded.", NamedTextColor.YELLOW));
        return true;
    }

    private boolean zoneInfo(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final String name = args[0];
        Zoned zoned = plugin.zonedMap.get(name);
        if (zoned == null) {
            throw new CommandWarn("Zone not found: " + name);
        }
        sender.sendMessage(Component.text("Zone Info " + zoned.zone.getName(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("World " + zoned.zone.getWorld(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Regions " + zoned.zone.getRegions().size(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("SpawnBlocks " + zoned.spawnBlocks.size(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("SpawnChunks " + zoned.chunkBlockMap.size(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("UpdateId " + zoned.updateId, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Disabled " + zoned.disabled, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Messages " + zoned.messageList.size(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Spawned " + plugin.countSpawned(zoned.zone) + " ", NamedTextColor.YELLOW)
                           .append(Component.join(Component.text(", ", NamedTextColor.GRAY),
                                                  plugin.findSpawned(zoned.zone).stream()
                                                  .map(spawned -> Component.text("(" + spawned.messageIndex + ")", NamedTextColor.GOLD))
                                                  .collect(Collectors.toList()))));
        return true;
    }

    private boolean zoneCreate(Player player, String[] args) {
        if (args.length != 1) return false;
        final String name = args[0];
        if (plugin.zonedMap.get(name) != null) {
            throw new CommandWarn("Zone already exists: " + name);
        }
        Zone zone = new Zone(name, player.getWorld().getName());
        plugin.save.getZones().add(zone);
        plugin.zonedMap.put(zone.getName(), new Zoned(plugin, zone, plugin.messagesConfig.getStringList(name)));
        player.sendMessage(Component.text("Zone created in world " + zone.getWorld() + ": " + zone.getName()));
        plugin.save();
        return true;
    }

    private boolean zoneType(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        Zoned zoned = requireZoned(args[0]);
        ZoneType type = requireZoneType(args[1]);
        zoned.zone.setType(type);
        plugin.save();
        plugin.setupZones();
        sender.sendMessage(Component.text("Set type of " + zoned.zone.getName() + " to " + type.name().toLowerCase(),
                                          NamedTextColor.YELLOW));
        return true;
    }

    private boolean zoneMax(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        Zoned zoned = requireZoned(args[0]);
        int amount = requireInt(args[1], i -> i > 0);
        zoned.zone.setMaxResidents(amount);
        plugin.save();
        sender.sendMessage(Component.text("Set max residents of " + zoned.zone.getName() + " to " + amount,
                                          NamedTextColor.YELLOW));
        return true;
    }

    private boolean zoneRegionAdd(Player player, String[] args) {
        if (args.length != 1) return false;
        final String name = args[0];
        Zoned zoned = plugin.zonedMap.get(name);
        if (zoned == null) {
            throw new CommandWarn("Zone not found: " + name);
        }
        if (!player.getWorld().getName().equals(zoned.zone.getWorld())) {
            throw new CommandWarn("You're not in world " + zoned.zone.getWorld());
        }
        Cuboid region = WorldEdit.getSelection(player);
        if (region == null) {
            throw new CommandWarn("You don't have a WorldEdit selection!");
        }
        zoned.zone.getRegions().add(region);
        zoned.updateSpawnBlocks();
        plugin.save();
        player.sendMessage(Component.text("Region added to zone " + zoned.zone.getName()
                                          + ": " + region, NamedTextColor.YELLOW));
        return true;
    }

    private Zoned requireZoned(String arg) {
        Zoned zoned = plugin.zonedMap.get(arg);
        if (zoned == null) {
            throw new CommandWarn("Zone not found: " + arg);
        }
        return zoned;
    }

    private ZoneType requireZoneType(String arg) {
        try {
            return ZoneType.valueOf(arg.toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Unknown zone type: " + arg);
        }
    }

    private int requireInt(String arg, IntPredicate predicate) {
        int result;
        try {
            result = Integer.parseInt(arg);
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Number expected: " + arg);
        }
        if (!predicate.test(result)) {
            throw new CommandWarn("Invalid number: " + result);
        }
        return result;
    }

    private List<String> completeZoneNames(CommandContext context, CommandNode node, String arg) {
        return plugin.zonedMap.keySet().stream()
            .filter(it -> it.contains(arg))
            .collect(Collectors.toList());
    }
}
