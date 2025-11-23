package dev.drkvoid.managers;

import dev.drkvoid.DrStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupManager implements Listener {

    private final DrStats plugin;
    private final LinkedList<Runnable> undoStack = new LinkedList<>();
    private final Map<UUID, RollbackSession> openSessions = new HashMap<>();

    private enum MenuType {
        LIST,
        CONFIRM,
        PREVIEW_INTERACTIVE
    }

    private enum PreviewTab {
        INVENTORY,
        ENDERCHEST,
        STATS
    }

    public BackupManager(DrStats plugin) {
        this.plugin = plugin;
        File backupsDir = new File(plugin.getDataFolder(), "backups");
        if (!backupsDir.exists()) backupsDir.mkdirs();
    }

    public void shutdown() {
        undoStack.clear();
        openSessions.clear();
    }

    public void performReset(CommandSender sender, List<OfflinePlayer> targets, String statName) {
        List<Runnable> undoActions = new ArrayList<>();
        int count = 0;

        for (OfflinePlayer player : targets) {
            try {
                if (statName.equals("*")) {
                    if (player.isOnline()) {
                        Player p = player.getPlayer();
                        ItemStack[] oldContents = p.getInventory().getContents();
                        p.getInventory().clear();
                        undoActions.add(() -> p.getInventory().setContents(oldContents));
                    }

                    if (player.isOnline()) {
                        Player p = player.getPlayer();
                        ItemStack[] oldEc = p.getEnderChest().getContents();
                        p.getEnderChest().clear();
                        undoActions.add(() -> p.getEnderChest().setContents(oldEc));
                    }

                    for (Statistic stat : Statistic.values()) {
                        if (stat.getType() == Statistic.Type.UNTYPED) {
                            try {
                                int oldVal = player.getStatistic(stat);
                                if (oldVal != 0) {
                                    player.setStatistic(stat, 0);
                                    undoActions.add(() -> player.setStatistic(stat, oldVal));
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    count++;

                } else if (statName.equals("INVENTORY")) {
                    if (player.isOnline()) {
                        Player p = player.getPlayer();
                        ItemStack[] oldContents = p.getInventory().getContents();
                        p.getInventory().clear();
                        undoActions.add(() -> p.getInventory().setContents(oldContents));
                        count++;
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "Skipping offline player " + player.getName() + " for inventory reset (Must be online).");
                    }
                } else if (statName.equals("ECHEST")) {
                    if (player.isOnline()) {
                        Player p = player.getPlayer();
                        ItemStack[] oldContents = p.getEnderChest().getContents();
                        p.getEnderChest().clear();
                        undoActions.add(() -> p.getEnderChest().setContents(oldContents));
                        count++;
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "Skipping offline player " + player.getName() + " for echest reset (Must be online).");
                    }
                } else {
                    Statistic stat = Statistic.valueOf(statName);
                    int oldVal = player.getStatistic(stat);
                    player.setStatistic(stat, 0);
                    undoActions.add(() -> player.setStatistic(stat, oldVal));
                    count++;
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid statistic: " + statName);
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("Error resetting " + player.getName() + ": " + e.getMessage());
            }
        }

        if (count > 0) {
            undoStack.push(() -> {
                for (Runnable r : undoActions) r.run();
            });
            plugin.getLogManager().logAction(sender.getName(), "Reset " + statName + " for " + count + " players.");
            sender.sendMessage(ChatColor.GREEN + "Reset " + statName + " for " + count + " players.");
            sender.sendMessage(ChatColor.GRAY + "Use /stats undo to reverse.");
        }
    }

    public void performEdit(CommandSender sender, OfflinePlayer target, Statistic stat, int newValue) {
        try {
            int oldValue = target.getStatistic(stat);
            target.setStatistic(stat, newValue);

            undoStack.push(() -> target.setStatistic(stat, oldValue));

            plugin.getLogManager().logAction(sender.getName(), "Set " + stat.name() + " of " + target.getName() + " to " + newValue);
            sender.sendMessage(ChatColor.GREEN + "Updated " + stat.name() + " for " + target.getName() + " to " + newValue + ".");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error editing stat: " + e.getMessage());
        }
    }

    public void undoLastAction(CommandSender sender) {
        if (undoStack.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Nothing to undo.");
            return;
        }
        Runnable undo = undoStack.pop();
        undo.run();
        sender.sendMessage(ChatColor.GREEN + "Undo successful.");
        plugin.getLogManager().logAction(sender.getName(), "Undid last action.");
    }

    public void createSnapshot(Player player) {
        File folder = new File(plugin.getDataFolder() + "/backups/" + player.getUniqueId());
        if (!folder.exists()) folder.mkdirs();

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File file = new File(folder, timestamp + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("inventory", player.getInventory().getContents());
        config.set("armor", player.getInventory().getArmorContents());
        config.set("echest", player.getEnderChest().getContents());

        for (Statistic s : Statistic.values()) {
            try {
                if (s.getType() == Statistic.Type.UNTYPED) {
                    config.set("stats." + s.name(), player.getStatistic(s));
                }
            } catch (Exception ignored) {}
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openRollbackGUI(Player admin, OfflinePlayer target, String statTarget) {
        File folder = new File(plugin.getDataFolder() + "/backups/" + target.getUniqueId());
        if (!folder.exists() || folder.listFiles() == null) {
            admin.sendMessage(ChatColor.RED + "No backups found for this player.");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 54, "Rollback: " + target.getName());
        List<File> files = Arrays.asList(folder.listFiles((dir, name) -> name.endsWith(".yml")));
        files.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        int slot = 0;
        for (File f : files) {
            if (slot >= 54) break;
            String name = f.getName().replace(".yml", "");
            ItemStack icon = new ItemStack(Material.PAPER);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + name);
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "Click to Rollback"));
            icon.setItemMeta(meta);
            gui.setItem(slot++, icon);
        }

        admin.openInventory(gui);
        openSessions.put(admin.getUniqueId(), new RollbackSession(target, statTarget, files, MenuType.LIST));
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player admin = (Player) e.getWhoClicked();

        if (!openSessions.containsKey(admin.getUniqueId())) return;
        RollbackSession session = openSessions.get(admin.getUniqueId());

        String title = e.getView().getTitle();

        if (title.startsWith("Rollback:") || title.startsWith("Preview:") || title.equals("Confirm Rollback?")) {
            e.setCancelled(true);
            if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        }

        if (title.startsWith("Rollback:") && session.menuType == MenuType.LIST) {
            if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;

            int slot = e.getSlot();
            if (slot < 0 || slot >= session.files.size()) return;

            File selectedFile = session.files.get(slot);
            openConfirmMenu(admin, session, selectedFile);
        }
        else if (title.equals("Confirm Rollback?") && session.menuType == MenuType.CONFIRM) {
            if (e.getCurrentItem() == null) return;
            Material type = e.getCurrentItem().getType();

            if (type == Material.LIME_STAINED_GLASS_PANE) {
                if (session.selectedFile != null) {
                    applyRollback(admin, session);
                    admin.closeInventory();
                }
            } else if (type == Material.RED_STAINED_GLASS_PANE) {
                session.isTransitioning = true;
                openRollbackGUI(admin, session.target, session.statTarget);
            } else if (type == Material.SPYGLASS) {
                handlePreviewRequest(admin, session);
            }
        }
        else if (title.startsWith("Preview:") && session.menuType == MenuType.PREVIEW_INTERACTIVE) {
            handlePreviewClick(admin, session, e.getSlot(), e.getCurrentItem());
        }
    }

    private void openConfirmMenu(Player admin, RollbackSession session, File file) {
        Inventory confirm = Bukkit.createInventory(null, InventoryType.HOPPER, "Confirm Rollback?");

        ItemStack yes = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta ym = yes.getItemMeta();
        ym.setDisplayName(ChatColor.GREEN + "CONFIRM ROLLBACK");
        yes.setItemMeta(ym);

        ItemStack preview = new ItemStack(Material.SPYGLASS);
        ItemMeta pm = preview.getItemMeta();
        pm.setDisplayName(ChatColor.AQUA + "PREVIEW");
        pm.setLore(Collections.singletonList(ChatColor.GRAY + "Click to view contents"));
        preview.setItemMeta(pm);

        ItemStack no = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta nm = no.getItemMeta();
        nm.setDisplayName(ChatColor.RED + "CANCEL");
        no.setItemMeta(nm);

        confirm.setItem(0, yes);
        confirm.setItem(2, preview);
        confirm.setItem(4, no);

        session.selectedFile = file;
        session.menuType = MenuType.CONFIRM;
        session.isTransitioning = true;
        admin.openInventory(confirm);
    }

    private void handlePreviewRequest(Player admin, RollbackSession session) {
        if (!session.statTarget.equals("*") && !session.statTarget.equals("INVENTORY") && !session.statTarget.equals("ECHEST")) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(session.selectedFile);
            String stat = session.statTarget;
            openSessions.remove(admin.getUniqueId());
            admin.closeInventory();
            admin.sendMessage(ChatColor.YELLOW + "Preview (" + stat + "): " + ChatColor.WHITE + config.getInt("stats." + stat));
            return;
        }

        session.previewTab = PreviewTab.INVENTORY;
        session.statsPage = 0;
        session.menuType = MenuType.PREVIEW_INTERACTIVE;
        session.isTransitioning = true;
        renderInteractivePreview(admin, session);
    }

    private void renderInteractivePreview(Player admin, RollbackSession session) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(session.selectedFile);
        String title = "Preview: " + session.selectedFile.getName().replace(".yml", "");
        Inventory inv = Bukkit.createInventory(null, 54, title);

        if (session.previewTab == PreviewTab.INVENTORY) {
            List<ItemStack> items = (List<ItemStack>) config.getList("inventory");
            if (items != null) {
                for (int i = 0; i < items.size() && i < 36; i++) {
                    inv.setItem(i, items.get(i));
                }
            }
        } else if (session.previewTab == PreviewTab.ENDERCHEST) {
            List<ItemStack> items = (List<ItemStack>) config.getList("echest");
            if (items != null) {
                for (int i = 0; i < items.size() && i < 27; i++) {
                    inv.setItem(i, items.get(i));
                }
            }
        } else if (session.previewTab == PreviewTab.STATS) {
            renderStatsPage(inv, config, session.statsPage);
        }

        renderNavigationBar(inv, session);
        admin.openInventory(inv);
    }

    private void renderStatsPage(Inventory inv, FileConfiguration config, int page) {
        List<String> stats = new ArrayList<>();
        if (config.contains("stats")) {
            stats.addAll(config.getConfigurationSection("stats").getKeys(false));
        }
        Collections.sort(stats);

        int itemsPerPage = 45;
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, stats.size());

        for (int i = start; i < end; i++) {
            String statName = stats.get(i);
            int val = config.getInt("stats." + statName);

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + statName);
            meta.setLore(Collections.singletonList(ChatColor.WHITE + "Value: " + val));
            item.setItemMeta(meta);

            inv.setItem(i - start, item);
        }
    }

    private void renderNavigationBar(Inventory inv, RollbackSession session) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.setDisplayName(" ");
        pane.setItemMeta(pm);
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);

        if (session.previewTab != PreviewTab.INVENTORY) {
            inv.setItem(45, createNavButton(Material.CHEST, ChatColor.GOLD + "View Inventory"));
        } else {
            inv.setItem(45, createNavButton(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Current: Inventory"));
        }

        if (session.previewTab != PreviewTab.ENDERCHEST) {
            inv.setItem(46, createNavButton(Material.ENDER_CHEST, ChatColor.LIGHT_PURPLE + "View EnderChest"));
        } else {
            inv.setItem(46, createNavButton(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Current: EnderChest"));
        }

        if (session.previewTab != PreviewTab.STATS) {
            inv.setItem(47, createNavButton(Material.WRITABLE_BOOK, ChatColor.AQUA + "View Stats"));
        } else {
            inv.setItem(47, createNavButton(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Current: Stats"));
        }

        if (session.previewTab == PreviewTab.STATS) {
            inv.setItem(48, createNavButton(Material.ARROW, ChatColor.YELLOW + "Previous Page"));
            inv.setItem(50, createNavButton(Material.ARROW, ChatColor.YELLOW + "Next Page"));
        }

        inv.setItem(53, createNavButton(Material.BARRIER, ChatColor.RED + "Back to Confirm"));
    }

    private ItemStack createNavButton(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private void handlePreviewClick(Player admin, RollbackSession session, int slot, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());

        if (slot >= 45) {
            if (name.contains("View Inventory")) {
                session.previewTab = PreviewTab.INVENTORY;
                session.isTransitioning = true;
                renderInteractivePreview(admin, session);
            } else if (name.contains("View EnderChest")) {
                session.previewTab = PreviewTab.ENDERCHEST;
                session.isTransitioning = true;
                renderInteractivePreview(admin, session);
            } else if (name.contains("View Stats")) {
                session.previewTab = PreviewTab.STATS;
                session.isTransitioning = true;
                renderInteractivePreview(admin, session);
            } else if (name.contains("Back to Confirm")) {
                session.isTransitioning = true;
                openConfirmMenu(admin, session, session.selectedFile);
            } else if (name.contains("Next Page")) {
                session.statsPage++;
                session.isTransitioning = true;
                renderInteractivePreview(admin, session);
            } else if (name.contains("Previous Page")) {
                if (session.statsPage > 0) {
                    session.statsPage--;
                    session.isTransitioning = true;
                    renderInteractivePreview(admin, session);
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (openSessions.containsKey(uuid)) {
            RollbackSession session = openSessions.get(uuid);

            if (session.isTransitioning) {
                session.isTransitioning = false;
                return;
            }

            if (session.menuType == MenuType.CONFIRM) {
                session.menuType = MenuType.LIST;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    openRollbackGUI(p, session.target, session.statTarget);
                });
            } else {
                openSessions.remove(uuid);
            }
        }
    }

    private void applyRollback(Player admin, RollbackSession session) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(session.selectedFile);
        OfflinePlayer target = session.target;

        openSessions.remove(admin.getUniqueId());

        if ((session.statTarget.equals("INVENTORY") || session.statTarget.equals("*")) && target.isOnline()) {
            Player p = target.getPlayer();
            List<ItemStack> inv = (List<ItemStack>) config.getList("inventory");
            List<ItemStack> armor = (List<ItemStack>) config.getList("armor");
            if (inv != null) p.getInventory().setContents(inv.toArray(new ItemStack[0]));
            if (armor != null) p.getInventory().setArmorContents(armor.toArray(new ItemStack[0]));
        }

        if ((session.statTarget.equals("ECHEST") || session.statTarget.equals("*")) && target.isOnline()) {
            Player p = target.getPlayer();
            List<ItemStack> ec = (List<ItemStack>) config.getList("echest");
            if (ec != null) p.getEnderChest().setContents(ec.toArray(new ItemStack[0]));
        }

        if (target.isOnline() || session.statTarget.equals("*") || !session.statTarget.equals("INVENTORY")) {
            if (session.statTarget.equals("*")) {
                for (Statistic s : Statistic.values()) {
                    if (s.getType() == Statistic.Type.UNTYPED && config.contains("stats." + s.name())) {
                        try {
                            target.setStatistic(s, config.getInt("stats." + s.name()));
                        } catch (Exception ignored) {}
                    }
                }
            } else if (!session.statTarget.equals("INVENTORY") && !session.statTarget.equals("ECHEST")) {
                Statistic s = Statistic.valueOf(session.statTarget);
                if (config.contains("stats." + s.name())) {
                    try {
                        target.setStatistic(s, config.getInt("stats." + s.name()));
                    } catch (Exception ignored) {}
                }
            }
        }

        admin.sendMessage(ChatColor.GREEN + "Rollback applied successfully!");
        plugin.getLogManager().logAction(admin.getName(), "Rolled back " + session.statTarget + " for " + target.getName() + " to version " + session.selectedFile.getName());
    }

    private static class RollbackSession {
        OfflinePlayer target;
        String statTarget;
        List<File> files;
        File selectedFile;
        MenuType menuType;
        boolean isTransitioning = false;

        PreviewTab previewTab = PreviewTab.INVENTORY;
        int statsPage = 0;

        public RollbackSession(OfflinePlayer target, String statTarget, List<File> files, MenuType menuType) {
            this.target = target;
            this.statTarget = statTarget;
            this.files = files;
            this.menuType = menuType;
        }
    }
}