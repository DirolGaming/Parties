package com.alessiodp.parties.commands;

import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.alessiodp.parties.Parties;
import com.alessiodp.parties.configuration.Messages;
import com.alessiodp.parties.configuration.Variables;
import com.alessiodp.parties.handlers.LogHandler;
import com.alessiodp.parties.objects.Party;
import com.alessiodp.parties.objects.Rank;
import com.alessiodp.parties.objects.ThePlayer;
import com.alessiodp.parties.utils.CommandInterface;
import com.alessiodp.parties.utils.PartiesPermissions;

public class CommandPrefix implements CommandInterface {
	private Parties plugin;

	public CommandPrefix(Parties parties) {
		plugin = parties;
	}

	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args) {
		Player p = (Player) sender;
		ThePlayer tp = plugin.getPlayerHandler().getThePlayer(p);
		/*
		 * Checks
		 */
		if (!p.hasPermission(PartiesPermissions.PREFIX.toString())) {
			tp.sendMessage(Messages.nopermission.replace("%permission%", PartiesPermissions.PREFIX.toString()));
			return true;
		}
		if (!tp.haveParty()) {
			tp.sendMessage(Messages.noparty);
			return true;
		}
		Party party = plugin.getPartyHandler().loadParty(tp.getPartyName());
		if (party == null) {
			tp.sendMessage(Messages.noparty);
			return true;
		}
		Rank r = plugin.getPartyHandler().searchRank(tp.getRank());
		if(r != null){
			if(!r.havePermission(PartiesPermissions.PRIVATE_EDIT_PREFIX.toString())){
				Rank rr = plugin.getPartyHandler().searchUpRank(tp.getRank(), PartiesPermissions.PRIVATE_EDIT_PREFIX.toString());
				if(rr!=null)
					tp.sendMessage(Messages.nopermission_party.replace("%rank%", rr.getName()));
				else
					tp.sendMessage(Messages.nopermission.replace("%permission%", PartiesPermissions.PRIVATE_EDIT_PREFIX.toString()));
				return true;
			}
		}
		if (args.length < 2) {
			tp.sendMessage(Messages.prefix_wrongcmd);
			return true;
		}
		/*
		 * 
		 * 
		 * 
		 */
		
		if (args[1].equalsIgnoreCase(Variables.tag_custom_removeword) && args.length == 2) {
			party.setPrefix("");
			party.updateParty();

			party.sendBroadcastParty(p, Messages.prefix_removed);
			party.sendSpyMessage(p, Messages.prefix_removed);
			plugin.getPartyHandler().tag_refresh(party);
			
			LogHandler.log(2, p.getName() + "[" + p.getUniqueId() + "] removed prefix of the party " + party.getName());
			return true;
		}

		StringBuilder sb = new StringBuilder();
		for(int word=1;word<args.length;word++){
			if(sb.length() > 1)
				sb.append(" ");
			sb.append(args[word]);
		}
		if (!Pattern.compile(Variables.tag_custom_allowedchars).matcher(sb.toString()).matches()
				|| (sb.toString().length() > Variables.tag_custom_maxlength)
				|| (sb.toString().length() < Variables.tag_custom_minlength)) {
			tp.sendMessage(Messages.prefix_invalidchars);
			return true;
		}
		for (String cen : Variables.tag_custom_censor) {
			if (sb.toString().toLowerCase().contains(cen.toLowerCase())) {
				tp.sendMessage(Messages.prefix_censored);
				return true;
			}
		}

		if(Variables.vault_enable && Variables.vault_prefix_price > 0 && !p.hasPermission(PartiesPermissions.ADMIN_VAULTBYPASS.toString())){
			if(Variables.vault_confirm_enable){
				if(tp.getLastCommand() != null && ((boolean)tp.getLastCommand()[2]) == true){
					if(plugin.getEconomy().getBalance(Bukkit.getOfflinePlayer(p.getUniqueId())) >= Variables.vault_prefix_price){
						plugin.getEconomy().withdrawPlayer(Bukkit.getOfflinePlayer(p.getUniqueId()), Variables.vault_prefix_price);
						tp.putLastCommand(null);
					} else {
						tp.sendMessage(Messages.vault_prefix_nomoney.replace("%price%", Double.toString(Variables.vault_prefix_price)));
						tp.putLastCommand(null);
						return true;
					}
				} else {
					String c = commandLabel;
					for(String s : args)
						c = c.concat(" " + s);
					tp.putLastCommand(new Object[]{System.currentTimeMillis(), c, false});
					tp.sendMessage(Messages.vault_confirm_warnonbuy.replace("%cmd%", args[0]).replace("%price%", Double.toString(Variables.vault_prefix_price)));
					return true;
				}
			} else {
				if (plugin.getEconomy().getBalance(Bukkit.getOfflinePlayer(p.getUniqueId())) >= Variables.vault_prefix_price) {
			        plugin.getEconomy().withdrawPlayer(Bukkit.getOfflinePlayer(p.getUniqueId()), Variables.vault_prefix_price);
				} else {
					tp.sendMessage(Messages.vault_prefix_nomoney.replace("%price%", Double.toString(Variables.vault_prefix_price)));
					return true;
				}
			}
		}
		
		party.setPrefix(sb.toString());
		party.updateParty();
		plugin.getPartyHandler().tag_refresh(party);
		
		party.sendBroadcastParty(p, Messages.prefix_changed.replace("%prefix%", sb.toString()).replace("%price%", Double.toString(Variables.vault_prefix_price)));
		party.sendSpyMessage(p, Messages.prefix_changed.replace("%prefix%", sb.toString()).replace("%price%", Double.toString(Variables.vault_prefix_price)));
		
		LogHandler.log(2, p.getName() + "[" + p.getUniqueId() + "] setted prefix of the party " + party.getName());
		return true;
	}
}
