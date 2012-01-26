package me.ellbristow.greylistVote;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class greyBlockListener implements Listener {
	
	public static greylistVote plugin;
	
	public greyBlockListener(greylistVote instance) {
		plugin = instance;
	}
	
	@EventHandler (priority = EventPriority.NORMAL)
	public void onBlockPlace (BlockPlaceEvent event) {
		Player player = event.getPlayer();
		if (player != null && !player.hasPermission("greylistvote.approved") && !player.hasPermission("greylistvote.build")) {
			player.sendMessage(ChatColor.RED + "Your reputation is too low to place blocks!");
			if (event.getBlockPlaced().getTypeId() == 323) {
				ItemStack items = new ItemStack(Material.SIGN, event.getPlayer().getItemInHand().getAmount() + 1);
				event.getPlayer().setItemInHand(items);
			}
			event.setCancelled(true);
		}
	}
	
	@EventHandler (priority = EventPriority.NORMAL)
	public void onBlockBreak (BlockBreakEvent event) {
		Player player = event.getPlayer();
		if (player != null && !player.hasPermission("greylistvote.approved") && !player.hasPermission("greylistvote.build")) {
			player.sendMessage(ChatColor.RED + "Your reputation is too low to destroy blocks!");
			Block block = event.getBlock();
			event.setCancelled(true);
			if (block.getTypeId() == 63 || block.getTypeId() == 68) {
				// Block is a sign
				Sign sign = (Sign) block.getState();
				sign.setLine(1, sign.getLine(1));
				sign.update();
			}
		}
	}
	
	@EventHandler (priority = EventPriority.NORMAL)
	public void onBlockIgnite (BlockIgniteEvent event) {
		Player player = event.getPlayer();
		if (player != null && !player.hasPermission("greylistvote.approved") && !player.hasPermission("greylistvote.build")) {
			player.sendMessage(ChatColor.RED + "People with low reputation shouldn't play with fire!");
			event.setCancelled(true);
		}
	}
}