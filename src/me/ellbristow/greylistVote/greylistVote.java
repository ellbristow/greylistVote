package me.ellbristow.greylistVote;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class greylistVote extends JavaPlugin {
	
	public static greylistVote plugin;
	public final Logger logger = Logger.getLogger("Minecraft");
	public final greyBlockListener blockListener = new greyBlockListener(this);
	public final greyPlayerListener loginListener = new greyPlayerListener(this);
	protected FileConfiguration config;
	public FileConfiguration usersConfig = null;
	private File usersFile = null;
	
	@Override
	public void onDisable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info("[" + pdfFile.getName() + "] is now disabled.");
	}

	@Override
	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		this.logger.info("[" + pdfFile.getName() + "] version " + pdfFile.getVersion() + " is enabled.");
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_IGNITE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_LOGIN, loginListener, Event.Priority.Normal, this);
		this.config = this.getConfig();
		this.config.set("required_votes", this.config.getInt("required_votes"));
		this.saveConfig();
		this.usersConfig = this.getUsersConfig();
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (commandLabel.equalsIgnoreCase("glv")) {
			if (args.length == 0) {
				PluginDescriptionFile pdfFile = this.getDescription();
				sender.sendMessage(ChatColor.GOLD + pdfFile.getName() + " version " + pdfFile.getVersion() + " by " + pdfFile.getAuthors());
				sender.sendMessage(ChatColor.GOLD + "Commands: {optional} [required]");
				sender.sendMessage(ChatColor.GOLD + "  /glv " + ChatColor.GRAY + ": View all GreylistVote commands");
				sender.sendMessage(ChatColor.GOLD + "  /greylist [player] " + ChatColor.GRAY + "Increase [player]s reputation");
				sender.sendMessage(ChatColor.GOLD + "  /gl [player] " + ChatColor.GRAY + "Same as /greylist");
				sender.sendMessage(ChatColor.GOLD + "  /griefer [player] " + ChatColor.GRAY + "Decrease [player]s reputation");
				sender.sendMessage(ChatColor.GOLD + "  /votelist {player} " + ChatColor.GRAY + "View your (or {player}s) reputation");
				sender.sendMessage(ChatColor.GOLD + "  /glvlist {player} " + ChatColor.GRAY + "Same as /votelist");
				if (sender.hasPermission("greylistvote.admin")) {
					sender.sendMessage(ChatColor.GOLD + "Admin Commands:");
					sender.sendMessage(ChatColor.GOLD + "  /glv setrep [req. votes] " + ChatColor.GRAY + ": Set required reputation");
					sender.sendMessage(ChatColor.GOLD + "  /clearvotes [player] " + ChatColor.GRAY + ": Remove all 'Server' votes");
				}
				return true;
			}
			else if (args.length == 2) {
				if (!sender.hasPermission("greylistvote.admin")) {
					sender.sendMessage(ChatColor.RED + "You do not have permission to do this!");
					return false;
				}
				int reqVotes = config.getInt("required_votes", 2);
				if (!args[0].equalsIgnoreCase("setrep")) {
					sender.sendMessage(ChatColor.RED + "Command not recognised!");
					return false;
				}
				try {
					reqVotes = Integer.parseInt(args[1]);
				}
				catch(NumberFormatException nfe) {
					// Failed. Number not an integer
					sender.sendMessage(ChatColor.RED + "[req. votes] must be a number!" );
					return false;
				}
				this.config.set("required_votes", reqVotes);
				this.saveConfig();
				sender.sendMessage(ChatColor.GOLD + "Reputation requirement now set to " + ChatColor.WHITE + args[1]);
				sender.sendMessage(ChatColor.GOLD + "Player approval will not be updated until they receive their next vote ro login.");
				return true;
			}
			return false;
		}
		else if (commandLabel.equalsIgnoreCase("clearserver")) {
			if (!sender.hasPermission("greylistvote.admin")) {
				sender.sendMessage(ChatColor.RED + "You do not have permission to do this!");
				return false;
			}
			if (args.length == 0) {
				sender.sendMessage(ChatColor.RED + "You must specify a player!");
				return false;
			}
			Player target = (Player) getServer().getOfflinePlayer(args[0]);
			if (target == null) {
				sender.sendMessage(ChatColor.RED + "Player " + ChatColor.WHITE + args[0] + ChatColor.RED + "not found!");
				return true;
			}
			String griefList = this.usersConfig.getString(target.getName().toLowerCase() + ".griefer", null);
			String voteList = this.usersConfig.getString(target.getName().toLowerCase() + ".votes", null);
			String newVoteList = null;
			String newGriefList = null;
			String[] voteArray = null;
			String[] griefArray = null;
			if (griefList == null && voteList == null) {
				sender.sendMessage(ChatColor.RED + "Player " + ChatColor.WHITE + args[0] + ChatColor.RED + "does not have any votes!");
				return true;
			}
			if (voteList != null) {
				voteArray = voteList.split(",");
				for (String vote : voteArray) {
					if (!vote.equals("Server")) {
						newVoteList += "," + vote;
					}
				}
				if (newVoteList != null) {
					newVoteList = newVoteList.replaceFirst(",","");
				}
				usersConfig.set(target.getName().toLowerCase() + ".votes", newVoteList);
			}
			if (griefList != null) {
				griefArray = griefList.split(",");
				for (String vote : griefArray) {
					if (!vote.equals("Server")) {
						newGriefList += "," + vote;
					}
				}
				if (newGriefList != null) {
					newGriefList = newGriefList.replaceFirst(",","");
				}
				usersConfig.set(target.getName().toLowerCase() + ".griefer", newGriefList);
			}
			saveUsersConfig();
			int rep = 0;
			int reqVotes = config.getInt("required_votes");
			voteArray = null;
			griefArray = null;
			if (newVoteList != null) {
				voteArray = voteList.split(",");
				for (@SuppressWarnings("unused") String vote : voteArray) {
					rep++;
				}
			}
			if (griefList != null) {
				griefArray = griefList.split(",");
				for (@SuppressWarnings("unused") String vote : griefArray) {
					rep--;
				}
			}
			sender.sendMessage(ChatColor.GOLD + "'Server' votes removed from " + ChatColor.WHITE + target.getName());
			target.sendMessage(ChatColor.GOLD + "Your 'Server' greylist/black-ball votes removed!");
			if (rep >= reqVotes && !target.hasPermission("greylistvote.approved")) {
				setApproved(target);
			}
			else if (rep < reqVotes && target.hasPermission("greylistvote.approved")) {
				setGriefer(target);
			}
		}
		else if (commandLabel.equalsIgnoreCase("greylist") || commandLabel.equalsIgnoreCase("gl")) {
			if (args.length != 1) {
				// No player specified or too many arguments
				return false;
			}
			else {
				Player target = getServer().getOfflinePlayer(args[0]).getPlayer();
				if (target == null) {
					// Player not online
					sender.sendMessage(args[0] + ChatColor.RED + " not found!");
					return false;
				}
				int reqVotes = this.config.getInt("required_votes");
				String griefList = this.usersConfig.getString(target.getName().toLowerCase() + ".griefer", null);
				String voteList = this.usersConfig.getString(target.getName().toLowerCase() + ".votes", null);
				String[] voteArray = null;
				String[] griefArray = null;
				if (voteList != null) {
					voteArray = voteList.split(",");
				}
				else {
					voteList = "";
				}
				if (griefList != null) {
					griefArray = griefList.split(",");
				}
				else {
					griefList = "";
				}
				if (!(sender instanceof Player)) {
					// Voter is the console
					this.usersConfig.set(target.getName().toLowerCase() + ".votes", "Server");
					this.usersConfig.set(target.getName().toLowerCase() + ".griefer", null);
					this.setApproved(target);
					this.saveUsersConfig();
					sender.sendMessage(args[0] + ChatColor.GOLD + " has been greylisted!");
					return true;
				}
				if (sender.getName().equalsIgnoreCase(target.getName())) {
					// Player voting for self
					sender.sendMessage(ChatColor.RED + "You cannot vote for yourself!");
					return true;
				}
				boolean found = false;
				if (voteArray != null) {
					for (String vote : voteArray) {
						if (vote.equalsIgnoreCase(sender.getName())) {
							found = true;
						}
					}
				}
				if (found) {
					// Voter has already voted for this target player
					sender.sendMessage(ChatColor.RED + "You have already voted for " + ChatColor.WHITE + target.getName());
					return true;
				}
				sender.sendMessage(ChatColor.GOLD + "Your greylist vote for " + ChatColor.WHITE + target.getName() + ChatColor.GOLD + " has been accepted!");
				Player[] onlinePlayers = getServer().getOnlinePlayers();
				for (Player chatPlayer : onlinePlayers) {
					if (chatPlayer.getName() != target.getName() && chatPlayer.getName() != sender.getName()) {
						chatPlayer.sendMessage(sender.getName() + ChatColor.GOLD + " voted for " + ChatColor.WHITE + target.getName() + ChatColor.GOLD + " to be greylisted!");
					}
					else if (chatPlayer.getName() != sender.getName()) {
						chatPlayer.sendMessage(sender.getName() + ChatColor.GOLD + " voted for you to be greylisted!");
					}
				}
				if (voteList.equals("")) {
					this.usersConfig.set(target.getName().toLowerCase() + ".votes", sender.getName());
				}
				else {
					this.usersConfig.set(target.getName().toLowerCase() + ".votes", voteList + "," + sender.getName());
				}
				int rep = 0;
				if (voteArray != null) {
					rep += voteArray.length + 1;
				}
				if (griefArray != null) {
					rep -= griefArray.length;
				}
				if (rep >= reqVotes && !target.hasPermission("greylistvote.approved")) {
					// Enough votes received
					this.setApproved(target);
				}
				this.saveUsersConfig();
				return true;
			}
		}
		else if (commandLabel.equalsIgnoreCase("griefer")) {
			if (args.length != 1) {
				// No player specified or too many arguments
				return false;
			}
			else {
				Player target = getServer().getOfflinePlayer(args[0]).getPlayer();
				if (target == null) {
					// Player not online
					sender.sendMessage(args[0] + ChatColor.RED + " not found!");
					return false;
				}
				int reqVotes = this.config.getInt("required_votes");
				String griefList = this.usersConfig.getString(target.getName().toLowerCase() + ".griefer", null);
				String voteList = this.usersConfig.getString(target.getName().toLowerCase() + ".votes", null);
				String[] voteArray = null;
				String[] griefArray = null;
				if (voteList != null) {
					voteArray = voteList.split(",");
				}
				else {
					voteList = "";
				}
				if (griefList != null) {
					griefArray = griefList.split(",");
				}
				else {
					griefList = "";
				}
				if (!(sender instanceof Player)) {
					// Voter is the console
					this.usersConfig.set(target.getName().toLowerCase() + ".griefer", "Server");
					this.usersConfig.set(target.getName().toLowerCase() + ".votes", null);
					this.setGriefer(target);
					this.saveUsersConfig();
					sender.sendMessage(args[0] + ChatColor.GOLD + " has been " + ChatColor.DARK_GRAY +"Black-Balled!");
					return true;
				}
				if (sender.getName() == target.getName()) {
					// Player voting for self
					sender.sendMessage(ChatColor.RED + "You cannot vote for yourself!");
					return true;
				}
				boolean found = false;
				if (griefArray != null) {
					for (String vote : griefArray) {
						if (vote.equalsIgnoreCase(sender.getName())) {
							found = true;
						}
					}
				}
				if (found) {
					// Voter has already voted for this target player
					sender.sendMessage(ChatColor.RED + "You have already voted for " + ChatColor.WHITE + target.getName());
					return true;
				}
				sender.sendMessage(ChatColor.GOLD + "Your griefer vote for " + ChatColor.WHITE + target.getName() + ChatColor.GOLD + " has been accepted!");
				Player[] onlinePlayers = getServer().getOnlinePlayers();
				for (Player chatPlayer : onlinePlayers) {
					if (chatPlayer.getName() != target.getName() && chatPlayer.getName() != sender.getName()) {
						chatPlayer.sendMessage(sender.getName() + ChatColor.GOLD + " voted for " + ChatColor.WHITE + target.getName() + ChatColor.GOLD + " to be " + ChatColor.DARK_GRAY + "black-balled" + ChatColor.GOLD + " for griefing!");
					}
					else if (chatPlayer.getName() != sender.getName()) {
						chatPlayer.sendMessage(sender.getName() + ChatColor.GOLD + " voted for you to be " + ChatColor.DARK_GRAY + " black-balled" + ChatColor.GOLD + " for " + ChatColor.RED + "griefing" + ChatColor.GOLD + "!");
					}
				}
				if (griefList.equals("")) {
					this.usersConfig.set(target.getName().toLowerCase() + ".griefer", sender.getName());
				}
				else {
					this.usersConfig.set(target.getName().toLowerCase() + ".griefer", voteList + "," + sender.getName());
				}
				int rep = 0;
				if (voteArray != null) {
					rep += voteArray.length;
				}
				if (griefArray != null) {
					rep -= griefArray.length + 1;
				}
				if (rep < reqVotes && target.hasPermission("greylistvote.approved")) {
					// Enough votes received
					this.setGriefer(target);
				}
				this.saveUsersConfig();
				return true;
			}
		}
		else if (commandLabel.equalsIgnoreCase("votelist") || commandLabel.equalsIgnoreCase("glvlist")) {
			if (args.length == 0) {
				String voteList = this.usersConfig.getString(sender.getName().toLowerCase() + ".votes", null);
				String griefList = this.usersConfig.getString(sender.getName().toLowerCase() + ".griefer", null);
				if (voteList == null && griefList == null) {
					sender.sendMessage(ChatColor.GOLD + "You have not received any votes.");
					sender.sendMessage(ChatColor.GOLD + "Current Reputation: " + ChatColor.WHITE + "0");
				}
				else {
					sender.sendMessage(ChatColor.GOLD + "You have received votes from:");
					int reputation = 0;
					int reqVotes = config.getInt("required_votes");
					boolean serverVote = false;
					String[] voteArray = null;
					String[] griefArray = null;
					if (voteList != null) {
						voteArray = voteList.split(",");
						if (voteArray.length != 0) {
							String votes = ChatColor.GREEN + "  Approvals: " + ChatColor.GOLD;
							for (String vote : voteArray) {
								votes = votes + vote + " ";
								if (vote.equals("Server")) {
									serverVote = true;
								}
								reputation ++;
							}
							if (serverVote) {
								reputation = reqVotes;
							}
							sender.sendMessage(votes);
						}
					}
					if (griefList != null) {
						griefArray = griefList.split(",");
						if (griefArray.length != 0) {
							String votes = ChatColor.DARK_GRAY + "  Black-Balls: " + ChatColor.GOLD;
							serverVote = false;
							for (String vote : griefArray) {
								votes = votes + vote + " ";
								if (vote.equals("Server")) {
									serverVote = true;
								}
								reputation--;
							}
							if (serverVote) {
								reputation = -1;
							}
							sender.sendMessage(votes);
						}
					}
					String repText = "";
					if (reputation >= reqVotes) {
						repText = " " + ChatColor.GREEN + reputation;
					}
					else {
						repText = " " + ChatColor.RED + reputation;
					}
					sender.sendMessage(ChatColor.GOLD + "Current Reputation:" + repText);
					sender.sendMessage(ChatColor.GOLD + "Required Reputation: " + ChatColor.WHITE + reqVotes);
				}
				return true;
			}
			else {
				OfflinePlayer checktarget = getServer().getOfflinePlayer(args[0]);
				String DN = null;
				String target = null;
				if (checktarget.isOnline()) {
					target = checktarget.getPlayer().getName();
					DN = checktarget.getPlayer().getDisplayName();
				}
				else {
					if (checktarget != null) {
						target = checktarget.getName();
						DN = checktarget.getName();
					}
				}
				if (target == null) {
					// Player not found
					sender.sendMessage(args[0] + ChatColor.RED + " not found!");
					return false;
				}
				String voteList = this.usersConfig.getString(target.toLowerCase() + ".votes", null);
				String griefList = this.usersConfig.getString(target.toLowerCase() + ".griefer", null);
				if (voteList == null && griefList == null) {
					sender.sendMessage(DN + ChatColor.GOLD + " has not received any votes.");
					sender.sendMessage(ChatColor.GOLD + "Current Reputation: " + ChatColor.WHITE + "0");
				}
				else {
					sender.sendMessage(DN + ChatColor.GOLD + " has received votes from:");
					int reputation = 0;
					int reqVotes = config.getInt("required_votes");
					boolean serverVote = false;
					String[] voteArray = null;
					String[] griefArray = null;
					if (voteList != null) {
						voteArray = voteList.split(",");
						if (voteArray.length != 0) {
							String votes = ChatColor.GREEN + "  Approvals: " + ChatColor.GOLD;
							for (String vote : voteArray) {
								votes = votes + vote + " ";
								if (vote.equals("Server")) {
									serverVote = true;
								}
								reputation ++;
							}
							if (serverVote) {
								reputation = reqVotes;
							}
							sender.sendMessage(votes);
						}
					}
					if (griefList != null) {
						griefArray = griefList.split(",");
						if (griefArray.length != 0) {
							String votes = ChatColor.DARK_GRAY + "  Black-Balls: " + ChatColor.GOLD;
							serverVote = false;
							for (String vote : griefArray) {
								votes = votes + vote + " ";
								if (vote.equals("Server")) {
									serverVote = true;
								}
								reputation--;
							}
							if (serverVote) {
								reputation = -1;
							}
							sender.sendMessage(votes);
						}
					}
					String repText = "";
					if (reputation >= reqVotes) {
						repText = " " + ChatColor.GREEN + reputation;
					}
					else {
						repText = " " + ChatColor.RED + reputation;
					}
					sender.sendMessage(ChatColor.GOLD + "Current Reputation:" + repText);
					sender.sendMessage(ChatColor.GOLD + "Required Reputation: " + ChatColor.WHITE + reqVotes);
				}
				return true;
			}
		}
		return false;
	}
	
	public void setApproved(Player target) {
		PermissionAttachment attachment = target.addAttachment(this);
		attachment.setPermission("greylistvote.approved", true);
		Player[] onlinePlayers = getServer().getOnlinePlayers();
		for (Player chatPlayer : onlinePlayers) {
			if (chatPlayer.getName() != target.getName()) {
				chatPlayer.sendMessage(target.getName() + ChatColor.GOLD + " has been greylisted!");
			}
			else {
				chatPlayer.sendMessage("You have been greylisted! Go forth and buildify!");
			}
		}
	}
	
	public void setGriefer(Player target) {
		PermissionAttachment attachment = target.addAttachment(this);
		attachment.setPermission("greylistvote.approved", false);
		Player[] onlinePlayers = getServer().getOnlinePlayers();
		for (Player chatPlayer : onlinePlayers) {
			if (chatPlayer.getName() != target.getName()) {
				chatPlayer.sendMessage(target.getName() + ChatColor.GOLD + " has been " + ChatColor.DARK_GRAY + "black-balled" + ChatColor.GOLD + " for " + ChatColor.RED + " griefing" + ChatColor.GOLD + "!");
			}
			else {
				chatPlayer.sendMessage(ChatColor.RED + "You have been " + ChatColor.DARK_GRAY + "black-balled" + ChatColor.RED + " for griefing!");
			}
		}
	}
	
	public void loadUsersConfig() {
		if (this.usersFile == null) {
			this.usersFile = new File(getDataFolder(),"users.yml");
		}
		this.usersConfig = YamlConfiguration.loadConfiguration(this.usersFile);
	}
	
	public FileConfiguration getUsersConfig() {
		if (this.usersConfig == null) {
			this.loadUsersConfig();
		}
		return this.usersConfig;
	}
	
	public void saveUsersConfig() {
		if (this.usersConfig == null || this.usersFile == null) {
			return;
		}
		try {
			this.usersConfig.save(this.usersFile);
		} catch (IOException ex) {
			this.logger.log(Level.SEVERE, "Could not save " + this.usersFile, ex );
		}
	}
}
