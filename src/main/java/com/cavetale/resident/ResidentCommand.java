package com.cavetale.resident;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandContext;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.mytems.item.axis.CuboidOutline;
import com.cavetale.resident.save.Loc;
import com.cavetale.resident.save.Zone;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class ResidentCommand extends AbstractCommand<ResidentPlugin> {
    protected ResidentCommand(final ResidentPlugin plugin) {
        super(plugin, "resident");
    }

    @Override
    public void onEnable() {
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload save file")
            .senderCaller(this::reload);
        // Zone
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
            .description("Set the zone type")
            .completers(this::completeZoneNames, CommandArgCompleter.enumLowerList(ZoneType.class))
            .senderCaller(this::zoneType);
        zoneNode.addChild("max").arguments("<name> <amount>")
            .description("Set the resident maximum")
            .completers(this::completeZoneNames, CommandArgCompleter.integer(i -> i > 0))
            .senderCaller(this::zoneMax);
        zoneNode.addChild("clear").arguments("<name>")
            .description("Clear all residents")
            .completers(this::completeZoneNames)
            .senderCaller(this::zoneClear);
        // Region
        CommandNode regionNode = rootNode.addChild("region")
            .description("Zone region commands");
        regionNode.addChild("add").arguments("<zone>")
            .description("Add WorldEdit selection to zone")
            .completers(this::completeZoneNames)
            .playerCaller(this::regionAdd);
        regionNode.addChild("remove").arguments("<zone>")
            .description("Remove regions in WorldEdit selection")
            .completers(this::completeZoneNames)
            .playerCaller(this::regionRemove);
        regionNode.addChild("highlight").arguments("<zone>")
            .description("Highlight zone regions")
            .completers(this::completeZoneNames)
            .playerCaller(this::regionHighlight);
        // PluginSpawn
        CommandNode pluginSpawnNode = rootNode.addChild("pluginspawn")
            .description("Plugin spawn commands");
        pluginSpawnNode.addChild("list").denyTabCompletion()
            .description("List plugin spawns")
            .senderCaller(this::pluginSpawnList);
        pluginSpawnNode.addChild("clear").arguments("[plugin]")
            .description("Clear plugin spawns")
            .completers(CommandArgCompleter.supplyStream(() -> {
                        return Stream.of(Bukkit.getPluginManager().getPlugins())
                            .map(Plugin::getName);
                    }))
            .senderCaller(this::pluginSpawnClear);
        pluginSpawnNode.addChild("add").arguments("<type>")
            .description("Add plugin spawn")
            .completers(CommandArgCompleter.enumLowerList(ZoneType.class))
            .playerCaller(this::pluginSpawnAdd);
        // Message
        CommandNode messageNode = rootNode.addChild("message");
        messageNode.addChild("info").arguments("<zone> <key>")
            .description("Print message info")
            .completer(this::completeMessage)
            .playerCaller(this::messageInfo);
        messageNode.addChild("view").arguments("<zone> <key>")
            .description("View messages")
            .completer(this::completeMessage)
            .playerCaller(this::messageView);
    }

    private boolean reload(CommandSender sender, String[] args) {
        plugin.loadZones();
        sender.sendMessage(Component.text("Save file reloaded.", YELLOW));
        return true;
    }

    private boolean zoneInfo(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final String name = args[0];
        Zoned zoned = plugin.zonedMap.get(name);
        if (zoned == null) {
            throw new CommandWarn("Zone not found: " + name);
        }
        sender.sendMessage(Component.text("Zone Info " + zoned.zone.getName(), YELLOW));
        sender.sendMessage(Component.text("World " + zoned.zone.getWorld(), YELLOW));
        sender.sendMessage(Component.text("Regions " + zoned.zone.getRegions().size(), YELLOW));
        sender.sendMessage(Component.text("SpawnBlocks " + zoned.spawnBlocks.size(), YELLOW));
        sender.sendMessage(Component.text("LoadedSpawnBlocks " + zoned.loadedSpawnBlocks.size(), YELLOW));
        sender.sendMessage(Component.text("SpawnChunks " + zoned.chunkBlockMap.size(), YELLOW));
        sender.sendMessage(Component.text("UpdateId " + zoned.updateId, YELLOW));
        sender.sendMessage(Component.text("Disabled " + zoned.disabled, YELLOW));
        sender.sendMessage(Component.text("Messages " + zoned.messageList.size(), YELLOW));
        sender.sendMessage(Component.text("Max " + zoned.zone.getMaxResidents(), YELLOW));
        sender.sendMessage(Component.text("Total " + zoned.total, YELLOW));
        sender.sendMessage(Component.text("Spawned " + plugin.countSpawned(zoned.zone), YELLOW));
        return true;
    }

    private boolean zoneCreate(Player player, String[] args) {
        if (args.length != 1) return false;
        final String name = args[0];
        if (plugin.zonedMap.get(name) != null) {
            throw new CommandWarn("Zone already exists: " + name);
        }
        Zone zone = new Zone(name, player.getWorld().getName());
        plugin.saveZone(zone);
        plugin.enableZone(zone);
        player.sendMessage(Component.text("Zone created in world " + zone.getWorld() + ": " + zone.getName()));
        return true;
    }

    private boolean zoneType(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        Zoned zoned = requireZoned(args[0]);
        ZoneType type = requireZoneType(args[1]);
        zoned.zone.setType(type);
        plugin.saveZone(zoned.zone);
        sender.sendMessage(Component.text("Set type of " + zoned.zone.getName() + " to " + type.name().toLowerCase(),
                                          YELLOW));
        return true;
    }

    private boolean zoneMax(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        Zoned zoned = requireZoned(args[0]);
        int amount = requireInt(args[1], i -> i > 0);
        zoned.zone.setMaxResidents(amount);
        plugin.saveZone(zoned.zone);
        sender.sendMessage(Component.text("Set max residents of " + zoned.zone.getName() + " to " + amount,
                                          YELLOW));
        return true;
    }

    private boolean zoneClear(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Zoned zoned = requireZoned(args[0]);
        int count = 0;
        for (Spawned spawned : plugin.findSpawned(zoned.zone)) {
            spawned.remove();
            count += 1;
        }
        sender.sendMessage(Component.text("Cleared " + count + " residents from " + zoned.zone.getName(),
                                          YELLOW));
        return true;
    }

    private boolean regionAdd(Player player, String[] args) {
        if (args.length != 1) return false;
        final String name = args[0];
        Zoned zoned = plugin.zonedMap.get(name);
        if (zoned == null) {
            throw new CommandWarn("Zone not found: " + name);
        }
        if (!player.getWorld().getName().equals(zoned.zone.getWorld())) {
            throw new CommandWarn("You're not in world " + zoned.zone.getWorld());
        }
        Cuboid region = Cuboid.requireSelectionOf(player);
        zoned.zone.getRegions().add(region);
        zoned.updateSpawnBlocks();
        plugin.saveZone(zoned.zone);
        player.sendMessage(Component.text("Region added to zone " + zoned.zone.getName()
                                          + ": " + region, YELLOW));
        return true;
    }

    private boolean regionRemove(Player player, String[] args) {
        if (args.length != 1) return false;
        final String name = args[0];
        Zoned zoned = plugin.zonedMap.get(name);
        if (zoned == null) {
            throw new CommandWarn("Zone not found: " + name);
        }
        if (!player.getWorld().getName().equals(zoned.zone.getWorld())) {
            throw new CommandWarn("You're not in world " + zoned.zone.getWorld());
        }
        Cuboid selection = Cuboid.requireSelectionOf(player);
        List<Cuboid> removeList = new ArrayList<>();
        for (Cuboid region : zoned.zone.getRegions()) {
            if (selection.contains(region)) {
                removeList.add(region);
            }
        }
        if (removeList.isEmpty()) {
            throw new CommandWarn("No regions contained in your selection!");
        }
        zoned.zone.getRegions().removeAll(removeList);
        zoned.updateSpawnBlocks();
        plugin.saveZone(zoned.zone);
        player.sendMessage(Component.text(removeList.size() + " regions removed from zone " + zoned.zone.getName()
                                          + ": " + removeList, YELLOW));
        return true;
    }

    private boolean regionHighlight(Player player, String[] args) {
        if (args.length != 1) return false;
        final String name = args[0];
        Zoned zoned = plugin.zonedMap.get(name);
        if (zoned == null) {
            throw new CommandWarn("Zone not found: " + name);
        }
        if (!player.getWorld().getName().equals(zoned.zone.getWorld())) {
            throw new CommandWarn("You're not in world " + zoned.zone.getWorld());
        }
        for (Cuboid region : zoned.zone.getRegions()) {
            final CuboidOutline outline = new CuboidOutline(player.getWorld(), region);
            outline.showOnlyTo(player);
            outline.spawn();
            outline.removeLater(100L);
            outline.glow(Color.YELLOW);
        }
        player.sendMessage(Component.text("Highlighting " + zoned.zone.getName(), YELLOW));
        return true;
    }

    private boolean pluginSpawnList(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        sender.sendMessage(Component.text("Total " + plugin.pluginSpawns.size() + " plugin spawns",
                                          YELLOW));
        for (PluginSpawn pluginSpawn : plugin.pluginSpawns) {
            sender.sendMessage(Component.text("- " + pluginSpawn.plugin.getName()
                                              + " " + pluginSpawn.type.name().toLowerCase()
                                              + " " + pluginSpawn.loc
                                              + " spawned=" + pluginSpawn.isSpawned()));
        }
        return true;
    }

    private boolean pluginSpawnClear(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        Plugin thePlugin = args.length >= 1
            ? Bukkit.getPluginManager().getPlugin(args[0])
            : plugin;
        if (thePlugin == null) {
            throw new CommandWarn("Plugin not found: " + args[0]);
        }
        plugin.clearPluginSpawns(thePlugin);
        sender.sendMessage(Component.text("Spawns of " + thePlugin.getName() + " cleared!", YELLOW));
        return true;
    }

    private boolean pluginSpawnAdd(Player player, String[] args) {
        if (args.length != 1) return false;
        PluginSpawn pluginSpawn = PluginSpawn.register(plugin,
                                                       requireZoneType(args[0]),
                                                       Loc.of(player.getLocation()));
        pluginSpawn.setOnPlayerClick(p -> p.sendMessage("Hello World"));
        player.sendMessage(Component.text("Plugin spawn added at your current location", YELLOW));
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

    private boolean messageInfo(Player player, String[] args) {
        if (args.length != 2) return false;
        final String zonedArg = args[0];
        final String messageArg = args[1];
        final var zoned = plugin.getZonedMap().get(args[0]);
        if (zoned == null) {
            throw new CommandWarn("Zone not found: " + zonedArg);
        }
        final var message = zoned.getMessageList().getMessages().get(messageArg);
        if (message == null) {
            throw new CommandWarn("Message not found: " + messageArg);
        }
        player.sendMessage(text(message.toString(), YELLOW));
        return true;
    }

    private boolean messageView(Player player, String[] args) {
        if (args.length != 2) return false;
        final String zonedArg = args[0];
        final String messageArg = args[1];
        final var zoned = plugin.getZonedMap().get(args[0]);
        if (zoned == null) {
            throw new CommandWarn("Zone not found: " + zonedArg);
        }
        final var message = zoned.getMessageList().getMessages().get(messageArg);
        if (message == null) {
            throw new CommandWarn("Message not found: " + messageArg);
        }
        message.send(player);
        return true;
    }

    public List<String> completeMessage(CommandContext context, CommandNode node, String[] args) {
        if (args.length == 1) {
            return completeZoneNames(context, node, args[0]);
        } else if (args.length == 2) {
            final String zonedArg = args[0];
            final String messageArg = args[1];
            final var zoned = plugin.getZonedMap().get(zonedArg);
            if (zoned == null) return List.of();
            final var list = new ArrayList<String>();
            final String lower = messageArg.toLowerCase();
            for (String key : zoned.getMessageList().getMessages().keySet()) {
                if (key.contains(lower)) list.add(key);
            }
            return list;
        } else {
            return List.of();
        }
    }
}
