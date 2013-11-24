package ca.wacos.nametagedit;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

/**
 * This class is responsible for handling the /ne command.
 * 
 * @author Levi Webb
 *
 */
class NametagCommand implements CommandExecutor {

	private ArrayList<String> updateTasks = new ArrayList<String>();
	
	/**
	 * onCommand method for the plugin.
	 * 
	 * @param sender the command sender
	 * @param cmd the executed command
	 * @param label the command label
	 * @param args an array of {@link String} objects for the command arguments
	 * @see {@link org.bukkit.command.CommandExecutor}
	 */
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player senderPlayer = null;
		if (sender instanceof Player) {
			senderPlayer = (Player) sender;
		}

		if (cmd.getName().equalsIgnoreCase("ne")) {
			if (senderPlayer != null) {
				if (!senderPlayer.hasPermission("NametagEdit.use")) {
					sender.sendMessage("§cYou don't have permission to use this plugin.");
					return true;
				}
			}
			if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
				if (senderPlayer != null) {
					if (!senderPlayer.hasPermission("NametagEdit.reload")) {
						sender.sendMessage("§cYou don't have permission to reload this plugin.");
						return true;
					}
				}
				NametagEdit.plugin.load();
				sender.sendMessage("§eReloaded group nodes and players.");
				return true;
			}
			if (args.length >= 1 && args[0].equalsIgnoreCase("update")) {
                return update(sender);
			}
			if (args.length >= 1 && args[0].equalsIgnoreCase("confirm")) {
				if (senderPlayer != null) {
					if (!senderPlayer.isOp()) {
						sender.sendMessage("§cOnly operators can update this plugin.");
						return true;
					}
				}
				return download(sender);
			}
			if (args.length >= 2) {
				String operation = args[0];
				String text = NametagUtils.trim(NametagUtils.getValue(getText(args)));
				String target = args[1];

				if (senderPlayer != null) {
					Player tp = Bukkit.getPlayer(target);
					if (tp != null && senderPlayer != tp) {
						if (!senderPlayer.hasPermission("NametagEdit.useall")) {
							sender.sendMessage("§cYou can only edit your own nametag.");
							return true;
						}
					}
					else if (!target.equalsIgnoreCase(senderPlayer.getName())) {
						if (!senderPlayer.hasPermission("NametagEdit.useall")) {
							sender.sendMessage("§cYou can only edit your own nametag.");
							return true;
						}
					}
				}

				if (operation.equalsIgnoreCase("prefix") || operation.equalsIgnoreCase("suffix")) {
					Player targetPlayer;


					targetPlayer = Bukkit.getPlayer(target);

					if (text.isEmpty()) {
						sender.sendMessage("§eNo " + operation.toLowerCase() + " given!");
						return true;
					}

					if (targetPlayer != null) {
						if (PlayerLoader.getPlayer(targetPlayer.getName()) == null) {
                            NametagManager.clear(targetPlayer.getName());
						}
					}

					String prefix = "";
					String suffix = "";
                    NametagChangeEvent.NametagChangeReason reason = null;
					if (operation.equalsIgnoreCase("prefix")) {
						prefix = NametagUtils.formatColors(text);
                        reason = NametagChangeEvent.NametagChangeReason.SET_PREFIX;
                    }
					else if (operation.equalsIgnoreCase("suffix")) {
						suffix = NametagUtils.formatColors(text);
                        reason = NametagChangeEvent.NametagChangeReason.SET_SUFFIX;
                    }

					if (targetPlayer != null)
                        setNametagSoft(targetPlayer.getName(), prefix, suffix, reason);
					if (targetPlayer != null)
                        PlayerLoader.update(targetPlayer.getName(), prefix, suffix);
					else
						PlayerLoader.update(target, prefix, suffix);
					if (targetPlayer != null)
						sender.sendMessage("§eSet " + targetPlayer.getName() + "\'s " + operation.toLowerCase() + " to \'" + text + "\'.");
					else
						sender.sendMessage("§eSet " + target + "\'s " + operation.toLowerCase() + " to \'" + text + "\'.");
				}
				else if (operation.equalsIgnoreCase("clear")) {
					Player targetPlayer;


					targetPlayer = Bukkit.getPlayer(target);
					if (targetPlayer != null)
						sender.sendMessage("§eReset " + targetPlayer.getName() + "\'s prefix and suffix.");
					else
						sender.sendMessage("§eReset " + target + "\'s prefix and suffix.");
					if (targetPlayer != null)
                        NametagManager.clear(targetPlayer.getName());
					if (targetPlayer != null)
						PlayerLoader.removePlayer(targetPlayer.getName(), null);
					else
						PlayerLoader.removePlayer(target, null);

					if (targetPlayer != null)
						for (String key : NametagEdit.groups.keySet().toArray(new String[NametagEdit.groups.keySet().size()])) {
							if (targetPlayer.hasPermission(key)) {
								String prefix = NametagEdit.groups.get(key).get("prefix");
								String suffix = NametagEdit.groups.get(key).get("suffix");
								if (prefix != null)
									prefix = NametagUtils.formatColors(prefix);
								if (suffix != null)
									suffix = NametagUtils.formatColors(suffix);
                                setNametagHard(targetPlayer.getName(), prefix, suffix, NametagChangeEvent.NametagChangeReason.GROUP_NODE);

								break;
							}
						}
				}
				else {
					sender.sendMessage("§eUnknown operation \'" + operation + "\', type §a/ne§e for help.");
					return true;
				}
			}
			else {
				sender.sendMessage("§e§nNametagEdit v" + NametagEdit.plugin.getDescription().getVersion() + " command usage:");
				sender.sendMessage("");
				sender.sendMessage("§a/ne prefix [player] [text]§e - sets a player's prefix");
				sender.sendMessage("§a/ne suffix [player] [text]§e - sets a player's suffix");
				sender.sendMessage("§a/ne clear [player]§e - clears both a player's prefix and suffix.");
				if (sender instanceof Player && ((Player) sender).hasPermission("NametagEdit.reload") || !(sender instanceof Player))
					sender.sendMessage("§a/ne reload§e - reloads the configs");
				if (sender instanceof Player && ((Player) sender).isOp() || !(sender instanceof Player))
					sender.sendMessage("§a/ne update§e - check for updates");
			}
		}
		return true;
	}
	/**
	 * Combines the given array of {@link String} objects into a single (@link String}
	 *
	 * @param args  the (@link String} array to combine
	 * @return  the combined string
	 */
	private String getText(String[] args) {
		String rv = "";
		for (int t = 2; t < args.length; t++) {
			if (t == args.length - 1)
				rv += args[t];
			else
				rv += args[t] + " ";
		}
		return rv;
	}
	/**
	 * Executes an update check from the given {@link org.bukkit.command.CommandSender} and if an update exists, add a task to the current list of update tasks.
	 *
	 * @param sender the {@link org.bukkit.command.CommandSender} to execute from
	 * @return true
	 * @see ca.wacos.nametagedit.Updater#manuallyCheckForUpdates(org.bukkit.command.CommandSender)
	 */
	private boolean update(CommandSender sender) {

		String name;

		if (sender instanceof Player) {
			name = sender.getName();
		}
		else {
			name = "^";
		}
		for (String key : updateTasks.toArray(new String[updateTasks.size()])) {
			if (key.equals(name)) {
				while (updateTasks.remove(key)) {}
				break;
			}
		}
		if (Updater.manuallyCheckForUpdates(sender)) {
			updateTasks.add(name);
		}
		return true;
	}
	/**
	 * Triggers a plugin update if the given {@link org.bukkit.command.CommandSender} has created an update task previously by calling {@link #update(org.bukkit.command.CommandSender)}
	 *
	 * @param sender the {@link org.bukkit.command.CommandSender} to execute from
	 * @return true
	 * @see ca.wacos.nametagedit.Updater#downloadUpdate(org.bukkit.command.CommandSender)
	 */
	private boolean download(CommandSender sender) {

		String name;
		if (sender instanceof Player) {
			name = ((Player) sender).getName();
		}
		else {
			name = "^";
		}

        for (String key : updateTasks.toArray(new String[updateTasks.size()])) {
			if (key.equals(name)) {
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (p.isOp()) {
						p.sendMessage("[§aNametagEdit§f] §ePlugin is updating...");
					}
				}
				updateTasks.clear();
				Updater.downloadUpdate(sender);
				return true;
			}
		}
		sender.sendMessage("§eNothing to confirm!");
		return true;
	}

    /**
     * Sets a player's nametag with the given information and additional reason.
     *
     * @param player the player whose nametag to set
     * @param prefix the prefix to set
     * @param suffix the suffix to set
     * @param reason the reason for setting the nametag
     */
    static void setNametagHard(String player, String prefix, String suffix, NametagChangeEvent.NametagChangeReason reason) {
        NametagChangeEvent e = new NametagChangeEvent(player, NametagAPI.getPrefix(player), NametagAPI.getSuffix(player), prefix, suffix, NametagChangeEvent.NametagChangeType.HARD, reason);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if (!e.isCancelled())
            NametagManager.overlap(player, prefix, suffix);
    }

    /**
     * Sets a player's nametag with the given information and additional reason.
     *
     * @param player the player whose nametag to set
     * @param prefix the prefix to set
     * @param suffix the suffix to set
     * @param reason the reason for setting the nametag
     */
    static void setNametagSoft(String player, String prefix, String suffix, NametagChangeEvent.NametagChangeReason reason) {
        NametagChangeEvent e = new NametagChangeEvent(player, NametagAPI.getPrefix(player), NametagAPI.getSuffix(player), prefix, suffix, NametagChangeEvent.NametagChangeType.SOFT, reason);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if (!e.isCancelled())
            NametagManager.update(player, prefix, suffix);
    }

}
