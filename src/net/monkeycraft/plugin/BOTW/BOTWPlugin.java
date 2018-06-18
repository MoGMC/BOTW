package net.monkeycraft.plugin.BOTW;

import com.google.common.collect.Lists;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class BOTWPlugin extends JavaPlugin implements Listener {

    private final HashMap<String, List<String>> messages = new HashMap<>();
    private FileConfiguration config;
    private FileConfiguration submissions;
    private HashMap<UUID, Location> builds = new HashMap<>();
    private String botwWinnerOne, botwWinnerTwo;
    private Location tpLocation;
    private int buildListThreshold;
    private List<List<String>> buildListMessages = new ArrayList<>();
    private boolean buildListWasUpdated = true;

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        config = getConfig();
        File submissionsFile = new File(getDataFolder(), "submissions.yml");
        if(!submissionsFile.exists()) {
            getServer().getLogger().severe("Missing submissions.yml. Shutting down.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        submissions = YamlConfiguration.loadConfiguration(submissionsFile);

        // load builds
        getServer().getLogger().info("Loading build submissions...");
        try {
            for (String uuid : submissions.getConfigurationSection("builds").getKeys(false)) {
                ConfigurationSection sector = submissions.getConfigurationSection("builds." + uuid);
                Location loc = new Location(
                        getServer().getWorld(sector.getString("world")),
                        sector.getInt("x"),
                        sector.getInt("y"),
                        sector.getInt("z")
                );
                builds.put(UUID.fromString(uuid), loc);
            }
            getServer().getLogger().info("Done!");
        } catch (NullPointerException e) {
            getServer().getLogger().info("No build submissions were found. Skipping...");
        }

        // Load all config stuff
        messages.put("botwMessage", config.getStringList("messages.botw-message"));
        messages.put("tpMessage", config.getStringList("messages.tp-message"));
        messages.put("submitConfirmation", config.getStringList("messages.submit-confirmation"));
        botwWinnerOne = config.getString("winning-info.botw-player1");
        botwWinnerTwo = config.getString("winning-info.botw-player2");
        tpLocation = new Location(
                getServer().getWorld(config.getString("winning-info.location.world")),
                config.getDouble("winning-info.location.x"),
                config.getDouble("winning-info.location.y"),
                config.getDouble("winning-info.location.z"));
        buildListThreshold = config.getInt("submission-list-threshold");
    }

    @Override
    public void onDisable() {

        getServer().getLogger().info("Saving build submissions...");

        // This part is purely for submissions
        for (HashMap.Entry<UUID, Location> entry : builds.entrySet()) {
            submissions.set("builds." + entry.getKey().toString() + ".world", entry.getValue().getWorld().getName());
            submissions.set("builds." + entry.getKey().toString() + ".x", (int) entry.getValue().getX());
            submissions.set("builds." + entry.getKey().toString() + ".y", (int) entry.getValue().getY());
            submissions.set("builds." + entry.getKey().toString() + ".z", (int) entry.getValue().getZ());
        } // </submissions>

        // Store all config stuff
        config.set("winning-info.botw-player1", botwWinnerOne);
        config.set("winning-info.botw-player2", botwWinnerTwo);
        config.set("winning-info.location.x", (int) tpLocation.getX());
        config.set("winning-info.location.y", (int) tpLocation.getY());
        config.set("winning-info.location.z", (int) tpLocation.getZ());

        saveConfig();
        getServer().getLogger().info("Done!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player && args.length > 0) {
            if (args[0].equalsIgnoreCase("tp") && sender.hasPermission("botw.user")) return tp((Player) sender);
            if (args[0].equalsIgnoreCase("settp") && sender.hasPermission("botw.admin")) return setTp((Player) sender);
            if (args[0].equalsIgnoreCase("submit") && sender.hasPermission("botw.user"))
                return submitBuild((Player) sender);
            if (args[0].equalsIgnoreCase("check") && args.length > 1 && sender.hasPermission("botw.admin"))
                return tp((Player) sender, args[1]);
        }

        if (args.length > 0) {
            try {
                if (args[0].equalsIgnoreCase("list") && sender.hasPermission("botw.admin")) {
                    if (args.length > 1) return showBuildList(sender, Integer.parseInt(args[1]));
                    else return showBuildList(sender, 0);
                }
            } catch (NumberFormatException e) {
                return showBuildList(sender, 0);
            }

            if (args[0].equalsIgnoreCase("clear") && sender.hasPermission("botw.admin")) return clearBuilds(sender);
            if (args[0].equalsIgnoreCase("setwinners") && args.length > 1 && sender.hasPermission("botw.admin"))
                return setWinners(sender, args[1], args[2]);
        }

        return !sender.hasPermission("botw.user") || sendBotwMessage(sender);
    }

    /**
     * Set winners of build of the week
     *
     * @param sender    Sender of command (Can be done through console)
     * @param winnerOne First winner. Just playername is plenty.
     * @param winnerTwo Second winner. Just playername is plenty.
     * @return Command status (usually true)
     */
    private boolean setWinners(CommandSender sender, String winnerOne, String winnerTwo) {
        botwWinnerOne = winnerOne;
        botwWinnerTwo = winnerTwo;
        sender.sendMessage((winnerTwo == null) ?
                ChatColor.BLUE + "Set the current winner to " + winnerOne + "." :
                ChatColor.BLUE + "Set the current two winners to " + winnerOne + " and " + winnerTwo + ".");
        return true;
    }

    /**
     * Clear current build submissions
     *
     * @param sender Sender of command (Can be done through console)
     * @return Command status (usually true)
     */
    private boolean clearBuilds(CommandSender sender) {
        builds = new HashMap<>();
        sender.sendMessage(ChatColor.RED + "Submissions were cleared!");
        buildListWasUpdated = true;
        return true;
    }

    /**
     * Send build of the week message
     *
     * @param sender Sender of command (Can be done through console)
     * @return Command status (usually true)
     */
    private boolean sendBotwMessage(CommandSender sender) {
        List<String> botwMessage = messages.get("botwMessage");
        if (botwMessage == null || botwMessage.isEmpty()) {
            sender.sendMessage("Something went wrong! Tell a server admin they forgot to specify botw-message.");
            return true;
        }
        for (String s : botwMessage) {
            sender.sendMessage(s.replace("$Player1$", botwWinnerOne).replace("$Player2$", botwWinnerTwo));
        }
        return true;
    }

    /**
     * Teleport player to winner location set through {@link BOTWPlugin#setTp(Player)}
     *
     * @param player Sender of the command (Player to teleport)
     * @return Command status (usually true)
     */
    private boolean tp(Player player) {
        try {
            player.teleport(tpLocation);
        } catch (NullPointerException e) {
            player.sendMessage(ChatColor.BLUE + "Nobody has won yet! Try again later.");
            return true;
        }
        List<String> botwMessage = messages.get("tpMessage");
        if (botwMessage == null || botwMessage.isEmpty()) {
            player.sendMessage("Something went wrong! Tell a server admin they forgot to specify tp-message.");
            return true;
        }
        for (String s : botwMessage) {
            player.sendMessage(s.replace("$Player1$", botwWinnerOne).replace("$Player2$", botwWinnerTwo));
        }
        return true;
    }

    /**
     * Teleport player to location of submission
     *
     * @param sender     Sender of the command (player to teleport)
     * @param playername Player with concerning submission
     * @return Command status (usually true)
     */
    private boolean tp(Player sender, String playername) {
        if (playername == null) {
            sender.sendMessage(ChatColor.RED + "Please provide a playername.");
            return true;
        }
        try {
            sender.teleport(builds.get(getServer().getPlayer(playername).getUniqueId()));
            sender.sendMessage(ChatColor.BLUE + "Teleported to submission of " + playername + ".");
        } catch (NullPointerException e) {
            sender.sendMessage(ChatColor.RED + "That's not a known player!");
        }
        return true;
    }

    /**
     * Set the current winning tp position
     *
     * @param player Sender of the command. Coordinates of this player will be used
     * @return Command status (usually true)
     */
    private boolean setTp(Player player) {
        Location l = player.getLocation();
        tpLocation = l;
        player.sendMessage(ChatColor.BLUE + "Tp location set to: x=" + (int) l.getX() + ", y=" + (int) l.getY() + ", z=" + (int) l.getZ());
        // We cast coordinates to int to prevent 10000 numbers getting into chat, nobody cares about that much detail
        config.set("winning-info.location.world", tpLocation.getWorld().getName());
        // We set the config here because getWorld doesn't work in onDisable as that part of the server is already unloaded.
        // This command won't be called 100000 times anyway.
        return true;
    }

    /**
     * Submit a build
     *
     * @param player Sender of the command. Coordinates of this player will be used
     * @return Command status (usually true)
     */
    private boolean submitBuild(Player player) {
        builds.put(player.getUniqueId(), player.getLocation());
        buildListWasUpdated = true;
        List<String> message = messages.get("submitConfirmation");
        if (message == null || message.isEmpty()) {
            player.sendMessage("Your submission has been received.");
            player.sendMessage("Please inform a server admin about an issue in botw config.");
            return true;
        }
        for (String s : message) {
            player.sendMessage(s);
        }
        return true;
    }

    /**
     * Show build list
     *
     * @param player Sender of the command. Can be done through console
     * @param page   Optional. Page to show. Default is page 1.
     * @return Command status (usually true)
     */
    @SuppressWarnings("ConstantConditions") // Suppress warning because we do null check and recursive call
    private boolean showBuildList(CommandSender player, int page) { // Integer object because nullable
        if (page <= 0) page = 1;
        if (buildListWasUpdated) { // We have this to prevent unnecessary memory usage in case of a lot of builds
            buildListMessages = new ArrayList<>();
            List<String> _messages = new ArrayList<>();

            for (Map.Entry<UUID, Location> entry : builds.entrySet()) { // This loops through a HashMap as if it were a list
                String playername = getServer().getPlayer(entry.getKey()).getName();
                _messages.add("§c- " + playername); // For example: - Ploffie
            }
            if (_messages.size() > 10)
                buildListMessages = Lists.partition(_messages, buildListThreshold); // Partition list into many at threshold
            else buildListMessages.add(_messages);
            buildListWasUpdated = false; // Don't do this again unless an extra build was submitted
        }

        if (page > buildListMessages.size()) page = buildListMessages.size();

        player.sendMessage("§9Submitted builds [" + page + "/" + buildListMessages.size() + "]");
        for (String s : buildListMessages.get(page - 1)) {
            if (s == null) break;
            player.sendMessage(s);
        }
        return true;
    }

}
