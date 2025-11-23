![drstats](https://cdn.modrinth.com/data/cached_images/9adf5c6abc71580082a9eb3770d0ef1a6ea63544.jpeg)
### The Ultimate Player Data & Statistic Management Solution

**DrStats** is an administration tool designed for **managing, editing, resetting, and rolling back** player statistics, inventories, and Ender Chests.

Whether you are running a seasonal server, fixing a player's corrupted data, or recovering items after a death or griefing incident, DrStats provides a safe, GUI-based environment to handle it all.

## ✨ Key Features

### 🛡️ **Comprehensive Data Management**

  * **Statistics:** View, Reset, or Edit *any* specific statistic (e.g., `deaths`, `mob_kills`, `play_one_minute`).
  * **Inventories & Ender Chests:** View, wipe, or restore player inventories and Ender Chests via commands or GUI.
  * **Offline Player Support:** Manage data even if the player is not currently on the server.

### ⏪ **Advanced Rollback System (GUI)**

  * **Auto-Snapshots:** The plugin automatically creates backups when a player **dies** or **quits** the server.
  * **Interactive GUI:** Browse backups visually with time-stamps.
  * **Smart Previews:** Click to preview a backup's Inventory, Ender Chest, or Stats *before* restoring it.
  * **Safety First:** Confirm/Cancel menus to prevent accidents.

![rollback](https://cdn.modrinth.com/data/cached_images/4a8823072d1d03e71ab8bf8d0f67c0be33d0e48b.png)

![confirm](https://cdn.modrinth.com/data/cached_images/19850d734e5e325be177f3ebdc066a32cfb8b7f0.png)

![previe](https://cdn.modrinth.com/data/cached_images/a9a6ca1cd966316066de3b666c257c7f8ae90ec7.png)

### ⚡ **Mass Operations**

  * Target a **specific player**.
  * Target **all online players** (`*`).
  * Target **every player who has ever joined** (`**`).
  * *Perfect for server wipes or seasonal resets.*

### ↩️ **Admin Safety**

  * **Undo Command:** Made a mistake with a command? `/stats undo` instantly reverts the last admin action.
  * **Action Logging:** All admin actions are logged to a file for accountability.

-----

## 🧭 Commands & Usage

**Main Command:** `/stats` or `/drstats`

### 🛠️ Management Commands

| Command | Description |
| :--- | :--- |
| `/stats view <stat> <player>` | View the value of a specific statistic for a player. |
| `/stats edit <stat> <player> <amount>` | Set a statistic to a specific value. |
| `/stats invsee <player>` | Open and edit a player's inventory (Live). |
| `/stats echest <player>` | Open and edit a player's Ender Chest (Live). |

### ⚠️ Reset Commands

| Command | Action |
| :--- | :--- |
| `/stats reset <stat> <target>` | Resets a specific stat (e.g., `deaths`). |
| `/stats reset inventory <target>` | Wipes the inventory. |
| `/stats reset echest <target>` | Wipes the Ender Chest. |
| `/stats reset * <target>` | **Full Wipe:** Resets Stats, Inv, and EChest. |

**Target Arguments:**

  * `<playername>`: Specific player.
  * `*`: All **Online** players.
  * `**`: All **Offline & Online** players (Entire Database).

### ♻️ Backup & Restore

| Command | Description |
| :--- | :--- |
| `/stats rollback <stat/inventory/echest/*> <player>` | Opens the **Rollback GUI** to browse and restore backups. |
| `/stats undo` | Reverts the last manual reset or edit command. |

-----

## 🔒 Permissions

You can grant `drstats.admin` for full access, or granular permissions:

  * `drstats.admin` - Full access to the plugin.
  * `drstats.view` - Allow viewing stats.
  * `drstats.edit` - Allow editing stats.
  * `drstats.reset` - Allow resetting data.
  * `drstats.rollback` - Allow access to the Rollback GUI.
  * `drstats.invsee` - Allow use of invsee.
  * `drstats.echest` - Allow use of echest.
  * `drstats.undo` - Allow using the undo command.

-----

## 📌 Compatibility & Info

  * **Minecraft Versions:** 1.20.2 - 1.21.x
  * **Software:** Paper, Spigot, Purpur.
  * **Metrics:** Uses bStats to collect anonymous usage data.

-----

*Found a bug? Have a suggestion? Join our Discord or open an issue on GitHub\!*
