package me.ellbristow.greylistVote;

import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;

public class greyPlayerListener extends PlayerListener {
	
	public static greylistVote plugin;
	public final Logger logger = Logger.getLogger("Minecraft");
	
	public greyPlayerListener (greylistVote instance) {
		plugin = instance;
	}
	
	public void onPlayerLogin (PlayerLoginEvent event) {
		Player player = event.getPlayer();
		String voteList = plugin.usersConfig.getString(player.getName().toLowerCase() + ".votes", null);
		String griefList = plugin.usersConfig.getString(player.getName().toLowerCase() + ".griefer", null);
		int rep = 0;
		int reqVotes = plugin.config.getInt("required_votes");
		boolean forceApprove = false;
		String[] voteArray = null;
		String[] griefArray = null;
		if (voteList != null) {
			voteArray = voteList.split(",");
			for (String vote : voteArray) {
				rep++;
				if (vote.equals("Server")) {
					rep = reqVotes;
					forceApprove = true;
				}
			}
		}
		if (griefList != null && !forceApprove) {
			griefArray = griefList.split(",");
			for (String vote : griefArray) {
				rep--;
				if (vote.equals("Server")) {
					rep = -1;
				}
			}
		}
		if (rep >= reqVotes && !player.hasPermission("greylistvote.approved")) {
			player.addAttachment(plugin, "greylistvote.approved", true);
		}
		else if (rep < reqVotes && player.hasPermission("greylistvote.approved")) {
			player.addAttachment(plugin, "greylistvote.approved", false);
		}
	}
}
