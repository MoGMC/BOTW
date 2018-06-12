package net.monkeycraft.plugin.BOTW;

import com.monkeygamesmc.plugin.playerdata.PlayerDataPlugin;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BOTWPlugin extends JavaPlugin {

	PlayerDataPlugin db;
	FileConfiguration config;

	HashMap<UUID, Location> builds;

	@Override
	public void onEnable () {

		getServer ().getPluginManager ().registerEvents (this, this);

		db = Bukkit.getServicesManager ().load (PlayerDataPlugin.class);

		saveDefaultConfig ();
		config = getConfig ();

		// load builds
		for (String buildData : config.getStringList ("builds")) loadBuildData (buildData);

	}

	@Override
	public void onDisable () {

		// store builds
		ArrayList<String> bs = new ArrayList<> ();

		for (UUID uuid : builds.keySet ()) bs.add (toBuildData (uuid, builds.get (uuid)));

		config.set ("builds", bs);

		saveConfig ();
	}

	@Override
	public boolean onCommand (CommandSender sender, Command command, String label, String[] args) {

		if (args.length == 0) { direct (sender); return true; }

		if (args[1].equalsIgnoreCase ("submit")) {

			config.set

		}

	}

	void direct (CommandSender sender) {

		sender.sendMessage (ChatColor.RED + "Could not find that subcommand!");

	}

	String toBuildData (UUID uuid, Location location) {

		return String.format ("%s?%s?%s?%s?%s?%s",
			uuid.toString (),
			location.getWorld ().getName (),
			location.getX (),
			location.getY (),
			location.getZ (),
			location.getYaw (),
			location.getPitch ()
		);

	}

	void loadBuildData (String buildData) {

		String[] data = buildData.split ("\\?");

		builds.put (
			UUID.fromString (data[0]),
			new Location (Bukkit.getServer ().getWorld (data[1]),
				Double.parseDouble (data[2]),
				Double.parseDouble (data[3]),
				Double.parseDouble (data[4]),
				Float.parseFloat (data[4]),
				Float.parseFloat (data[5]))
		);


	}

	void tp (Player player, UUID buildUuid) {
		player.teleport (builds.get (buildUuid));
		player.sendMessage ("Teleported to build submission of " + player.getDisplayName ());

	}

	BaseComponent[] getButton (UUID uuid) {

		return new ComponentBuilder (Bukkit.getPlayer (uuid).getDisplayName ())
			.event (new ClickEvent (ClickEvent.Action.RUN_COMMAND, String.format ("/tp ")))

	}

	void sendBuilds (int page, CommandSender.Spigot sender) {

		List<String> buildsData = config.getStringList ("builds");


	}

}
