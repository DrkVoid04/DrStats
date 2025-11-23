package dev.drkvoid;

import dev.drkvoid.managers.BackupManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StatsCommand implements CommandExecutor, TabCompleter {

    private final DrStats plugin;
    private final List<String> subCommands = Arrays.asList("reset", "undo", "edit", "view", "invsee", "echest", "rollback");

    public StatsCommand(DrStats plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reset":
                handleReset(sender, args);
                break;
            case "edit":
                handleEdit(sender, args);
                break;
            case "view":
                handleView(sender, args);
                break;
            case "undo":
                handleUndo(sender);
                break;
            case "invsee":
                handleInvSee(sender, args);
                break;
            case "echest":
                handleEChest(sender, args);
                break;
            case "rollback":
                handleRollback(sender, args);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Usage: /stats <reset|edit|view|undo|invsee|echest|rollback>");
        }
        return true;
    }

    private void handleView(CommandSender sender, String[] args) {
        if (!sender.hasPermission("drstats.view")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /stats view <statname> <player>");
            return;
        }

        String statName = args[1].toUpperCase();
        String targetName = args[2];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' has never played on this server.");
            return;
        }

        try {
            Statistic stat = Statistic.valueOf(statName);
            int value = target.getStatistic(stat);
            sender.sendMessage(ChatColor.GREEN + target.getName() + "'s " + ChatColor.YELLOW + stat.name() + ChatColor.GREEN + ": " + ChatColor.WHITE + value);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid statistic: " + statName);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Could not retrieve stat (Player might need to be online depending on server version): " + e.getMessage());
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("drstats.reset")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /stats reset <statname|inventory|echest> <player|*|**>");
            return;
        }

        String statName = args[1].toUpperCase();
        String targetName = args[2];
        List<OfflinePlayer> targets = getTargets(sender, targetName);

        if (targets.isEmpty()) return;

        plugin.getBackupManager().performReset(sender, targets, statName);
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("drstats.edit")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length != 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /stats edit <statname> <player> <amount>");
            return;
        }

        String statName = args[1].toUpperCase();
        String targetName = args[2];
        String amountStr = args[3];

        if (statName.equals("INVENTORY") || statName.equals("ECHEST")) {
            sender.sendMessage(ChatColor.RED + "Inventory and Ender Chest cannot be edited via command. Use /stats invsee or /stats echest.");
            return;
        }

        Statistic stat;
        try {
            stat = Statistic.valueOf(statName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid statistic: " + statName);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number: " + amountStr);
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        plugin.getBackupManager().performEdit(sender, target, stat, amount);
    }

    private void handleUndo(CommandSender sender) {
        if (!sender.hasPermission("drstats.undo")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        plugin.getBackupManager().undoLastAction(sender);
    }

    private void handleInvSee(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }
        if (!sender.hasPermission("drstats.invsee")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /stats invsee <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player must be online to view inventory.");
            return;
        }

        ((Player) sender).openInventory(target.getInventory());
        plugin.getLogManager().logAction(sender.getName(), "Opened inventory of " + target.getName());
    }

    private void handleEChest(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }
        if (!sender.hasPermission("drstats.echest")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /stats echest <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player must be online to view ender chest.");
            return;
        }

        ((Player) sender).openInventory(target.getEnderChest());
        plugin.getLogManager().logAction(sender.getName(), "Opened ender chest of " + target.getName());
    }

    private void handleRollback(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use the rollback GUI.");
            return;
        }
        if (!sender.hasPermission("drstats.rollback")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /stats rollback <statname|*> <player>");
            return;
        }

        String statName = args[1].toUpperCase();
        String playerName = args[2];

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Player has never played.");
            return;
        }

        plugin.getBackupManager().openRollbackGUI((Player) sender, target, statName);
    }

    private List<OfflinePlayer> getTargets(CommandSender sender, String target) {
        List<OfflinePlayer> players = new ArrayList<>();
        if (target.equals("*")) {
            players.addAll(Bukkit.getOnlinePlayers());
        } else if (target.equals("**")) {
            players.addAll(Arrays.asList(Bukkit.getOfflinePlayers()));
        } else {
            OfflinePlayer p = Bukkit.getOfflinePlayer(target);
            if (p.hasPlayedBefore() || p.isOnline()) {
                players.add(p);
            } else {
                sender.sendMessage(ChatColor.RED + "Player '" + target + "' not found.");
            }
        }

        if (players.isEmpty() && !target.equals("*") && !target.equals("**")) {
        } else if (players.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No players found.");
        }

        return players;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- DrStats Help ---");
        sender.sendMessage(ChatColor.YELLOW + "/stats view <stat> <player>" + ChatColor.GRAY + " - View a player's stat.");
        sender.sendMessage(ChatColor.YELLOW + "/stats reset <stat> <target>" + ChatColor.GRAY + " - Reset a stat/inv/echest.");
        sender.sendMessage(ChatColor.YELLOW + "/stats edit <stat> <player> <val>" + ChatColor.GRAY + " - Set a specific stat value.");
        sender.sendMessage(ChatColor.YELLOW + "/stats invsee <player>" + ChatColor.GRAY + " - Edit online player inventory.");
        sender.sendMessage(ChatColor.YELLOW + "/stats echest <player>" + ChatColor.GRAY + " - Edit online player echest.");
        sender.sendMessage(ChatColor.YELLOW + "/stats rollback <stat|*> <player>" + ChatColor.GRAY + " - Open rollback menu.");
        sender.sendMessage(ChatColor.YELLOW + "/stats undo" + ChatColor.GRAY + " - Undo the last admin reset/edit.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], subCommands, completions);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            if (sub.equals("reset") || sub.equals("view") || sub.equals("rollback")) {
                List<String> stats = Arrays.stream(Statistic.values()).map(Statistic::name).collect(Collectors.toList());
                if (sub.equals("reset") || sub.equals("rollback")) {
                    stats.add("INVENTORY");
                    stats.add("ECHEST");
                }
                if (sub.equals("rollback")) {
                    stats.add("*");
                }
                return StringUtil.copyPartialMatches(args[1], stats, completions);
            }
            if (sub.equals("edit")) {
                List<String> stats = Arrays.stream(Statistic.values()).map(Statistic::name).collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[1], stats, completions);
            }
        }


        if ((args.length == 3 && (sub.equals("reset") || sub.equals("view") || sub.equals("edit") || sub.equals("rollback")))
                || (args.length == 2 && (sub.equals("invsee") || sub.equals("echest")))) {

            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));

            String currentArg = args[args.length - 1];
            if (currentArg.length() > 0) {
                Arrays.stream(Bukkit.getOfflinePlayers())
                        .map(OfflinePlayer::getName)
                        .filter(Objects::nonNull)
                        .filter(name -> name.toLowerCase().startsWith(currentArg.toLowerCase()))
                        .limit(50)
                        .forEach(names::add);
            }

            if (sub.equals("reset")) {
                names.add("*");
                names.add("**");
            }

            List<String> uniqueNames = names.stream().distinct().collect(Collectors.toList());

            return StringUtil.copyPartialMatches(args[args.length - 1], uniqueNames, completions);
        }

        return Collections.emptyList();
    }
}