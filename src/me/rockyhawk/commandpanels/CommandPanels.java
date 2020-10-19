package me.rockyhawk.commandpanels;

import com.Ben12345rocks.VotingPlugin.UserManager.UserManager;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.clip.placeholderapi.PlaceholderAPI;
import me.realized.tokenmanager.api.TokenManager;
import me.rockyhawk.commandpanels.classresources.*;
import me.rockyhawk.commandpanels.commands.*;
import me.rockyhawk.commandpanels.completetabs.CpTabComplete;
import me.rockyhawk.commandpanels.customcommands.Commandpanelcustom;
import me.rockyhawk.commandpanels.generatepanels.Commandpanelsgenerate;
import me.rockyhawk.commandpanels.generatepanels.GenUtils;
import me.rockyhawk.commandpanels.generatepanels.TabCompleteGenerate;
import me.rockyhawk.commandpanels.ingameeditor.CpIngameEditCommand;
import me.rockyhawk.commandpanels.ingameeditor.CpTabCompleteIngame;
import me.rockyhawk.commandpanels.ingameeditor.EditorUserInput;
import me.rockyhawk.commandpanels.ingameeditor.EditorUtils;

import me.rockyhawk.commandpanels.ioclasses.Sequence_1_13;
import me.rockyhawk.commandpanels.ioclasses.Sequence_1_14;

import me.rockyhawk.commandpanels.legacy.LegacyVersion;
import me.rockyhawk.commandpanels.legacy.PlayerHeads;
import me.rockyhawk.commandpanels.openpanelsmanager.OpenGUI;
import me.rockyhawk.commandpanels.openpanelsmanager.OpenPanelsLoader;
import me.rockyhawk.commandpanels.openpanelsmanager.UtilsPanelsLoader;
import me.rockyhawk.commandpanels.openwithitem.SwapItemEvent;
import me.rockyhawk.commandpanels.openwithitem.UtilsOpenWithItem;
import me.rockyhawk.commandpanels.panelblocks.BlocksTabComplete;
import me.rockyhawk.commandpanels.panelblocks.Commandpanelblocks;
import me.rockyhawk.commandpanels.panelblocks.PanelBlockOnClick;
import me.rockyhawk.commandpanels.interactives.CommandpanelUserInput;
import me.rockyhawk.commandpanels.interactives.Commandpanelrefresher;
import me.rockyhawk.commandpanels.updater.Updater;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandPanels extends JavaPlugin {
    public YamlConfiguration config;
    public Economy econ = null;
    public boolean update = false;
    public boolean debug = false;
    public boolean openWithItem = false; //this will be true if there is a panel with open-with-item

    public List<Player> generateMode = new ArrayList<>(); //players that are currently in generate mode
    public List<String[]> userInputStrings = new ArrayList<>();
    public List<String[]> editorInputStrings = new ArrayList<>();
    public List<String> panelFiles = new ArrayList<>(); //names of all the files in the panels folder including extension
    public List<String[]> panelNames = new ArrayList<>(); //this will return something like {"mainMenuPanel","4"} which means the 4 is for panelFiles.get(4). So you know which file it is for

    //get alternate classes
    public CommandTags commandTags = new CommandTags(this);
    public OpenEditorGuis editorGuis = new OpenEditorGuis(this);
    public ExecuteOpenVoids openVoids = new ExecuteOpenVoids(this);
    public ItemCreation itemCreate = new ItemCreation(this);
    public GetCustomHeads customHeads = new GetCustomHeads(this);
    public Updater updater = new Updater(this);
    public PlayerHeads getHeads = new PlayerHeads(this);
    public LegacyVersion legacy = new LegacyVersion(this);
    public OpenPanelsLoader openPanels = new OpenPanelsLoader(this);
    public OpenGUI createGUI = new OpenGUI(this);

    public File panelsf;
    public YamlConfiguration blockConfig; //where panel block locations are stored

    public CommandPanels() {
        this.panelsf = new File(this.getDataFolder() + File.separator + "panels");
        this.blockConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder() + File.separator + "blocks.yml"));
    }

    public void onEnable() {
        Bukkit.getLogger().info("[CommandPanels] RockyHawk's CommandPanels v" + this.getDescription().getVersion() + " Plugin Loading...");

        this.config = YamlConfiguration.loadConfiguration(new File(this.getDataFolder() + File.separator + "config.yml"));
        //save the config.yml file
        File configFile = new File(this.getDataFolder() + File.separator + "config.yml");
        if (!configFile.exists()) {
            //generate a new config file from internal resources
            try {
                FileConfiguration configFileConfiguration = YamlConfiguration.loadConfiguration(getReaderFromStream(this.getResource("config.yml")));
                configFileConfiguration.save(configFile);
                this.config = YamlConfiguration.loadConfiguration(new File(this.getDataFolder() + File.separator + "config.yml"));
            } catch (IOException var11) {
                Bukkit.getConsoleSender().sendMessage("[CommandPanels]" + ChatColor.RED + " WARNING: Could not save the config file!");
            }
        }else{
            //check if the config file has any missing elements
            try {
                YamlConfiguration configFileConfiguration = YamlConfiguration.loadConfiguration(getReaderFromStream(this.getResource("config.yml")));
                this.config.addDefaults(configFileConfiguration);
                this.config.options().copyDefaults(true);
                this.config.save(new File(this.getDataFolder() + File.separator + "config.yml"));
            } catch (IOException var10) {
                Bukkit.getConsoleSender().sendMessage("[CommandPanels]" + ChatColor.RED + " WARNING: Could not save the config file!");
            }
        }

        //setup class files
        this.setupEconomy();
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        new Metrics(this);
        Objects.requireNonNull(this.getCommand("commandpanel")).setExecutor(new Commandpanel(this));
        Objects.requireNonNull(this.getCommand("commandpanel")).setTabCompleter(new CpTabComplete(this));
        Objects.requireNonNull(this.getCommand("commandpanelgenerate")).setTabCompleter(new TabCompleteGenerate(this));
        Objects.requireNonNull(this.getCommand("commandpanelgenerate")).setExecutor(new Commandpanelsgenerate(this));
        Objects.requireNonNull(this.getCommand("commandpanelreload")).setExecutor(new Commandpanelsreload(this));
        Objects.requireNonNull(this.getCommand("commandpaneldebug")).setExecutor(new Commandpanelsdebug(this));
        Objects.requireNonNull(this.getCommand("commandpanelclose")).setExecutor(new Commandpanelclose(this));
        Objects.requireNonNull(this.getCommand("commandpanelversion")).setExecutor(new Commandpanelversion(this));
        Objects.requireNonNull(this.getCommand("commandpaneladdons")).setExecutor(new Commandpanelresources(this));
        Objects.requireNonNull(this.getCommand("commandpanellist")).setExecutor(new Commandpanelslist(this));
        this.getServer().getPluginManager().registerEvents(new Utils(this), this);
        this.getServer().getPluginManager().registerEvents(new UtilsPanelsLoader(this), this);
        this.getServer().getPluginManager().registerEvents(new GenUtils(this), this);
        this.getServer().getPluginManager().registerEvents(new CommandpanelUserInput(this), this);

        //if refresh-panels set to false, don't load this
        if(Objects.requireNonNull(config.getString("config.refresh-panels")).equalsIgnoreCase("true")){
            this.getServer().getPluginManager().registerEvents(new Commandpanelrefresher(this), this);
        }

        //if custom-commands set to false, don't load this
        if(Objects.requireNonNull(config.getString("config.custom-commands")).equalsIgnoreCase("true")){
            this.getServer().getPluginManager().registerEvents(new Commandpanelcustom(this), this);
        }

        //if hotbar-items set to false, don't load this
        if(Objects.requireNonNull(config.getString("config.hotbar-items")).equalsIgnoreCase("true")){
            this.getServer().getPluginManager().registerEvents(new UtilsOpenWithItem(this), this);
        }

        //if ingame-editor set to false, don't load this
        if(Objects.requireNonNull(config.getString("config.ingame-editor")).equalsIgnoreCase("true")){
            Objects.requireNonNull(this.getCommand("commandpaneledit")).setTabCompleter(new CpTabCompleteIngame(this));
            Objects.requireNonNull(this.getCommand("commandpaneledit")).setExecutor(new CpIngameEditCommand(this));
            this.getServer().getPluginManager().registerEvents(new EditorUtils(this), this);
            this.getServer().getPluginManager().registerEvents(new EditorUserInput(this), this);
        }

        //if panel-blocks set to false, don't load this
        if(Objects.requireNonNull(config.getString("config.panel-blocks")).equalsIgnoreCase("true")){
            Objects.requireNonNull(this.getCommand("commandpanelblock")).setExecutor(new Commandpanelblocks(this));
            Objects.requireNonNull(this.getCommand("commandpanelblock")).setTabCompleter(new BlocksTabComplete(this));
            this.getServer().getPluginManager().registerEvents(new PanelBlockOnClick(this), this);
        }

        //if 1.8 don't use this
        if (!Bukkit.getVersion().contains("1.8")) {
            this.getServer().getPluginManager().registerEvents(new SwapItemEvent(this), this);
        }

        //save the example.yml file
        if (!this.panelsf.exists() || Objects.requireNonNull(this.panelsf.list()).length == 0) {
            try {
                FileConfiguration exampleFileConfiguration;
                if(legacy.isLegacy()){
                    exampleFileConfiguration = YamlConfiguration.loadConfiguration(getReaderFromStream(this.getResource("exampleLegacy.yml")));
                }else {
                    exampleFileConfiguration = YamlConfiguration.loadConfiguration(getReaderFromStream(this.getResource("example.yml")));
                }
                exampleFileConfiguration.save(new File(this.panelsf + File.separator + "example.yml"));
            } catch (IOException var11) {
                Bukkit.getConsoleSender().sendMessage("[CommandPanels]" + ChatColor.RED + " WARNING: Could not save the example file!");
            }
        }

        if (Objects.requireNonNull(this.config.getString("config.update-notifications")).equalsIgnoreCase("true")) {
            updater.githubNewUpdate(true);
        }

        //load panelFiles
        reloadPanelFiles();

        Bukkit.getLogger().info("[CommandPanels] RockyHawk's CommandPanels v" + this.getDescription().getVersion() + " Plugin Loaded!");
    }

    public void onDisable() {
        Bukkit.getLogger().info("RockyHawk's CommandPanels Plugin Disabled, aww man.");
    }

    public void setName(ItemStack renamed, String customName, List<String> lore, Player p, Boolean usePlaceholders, Boolean useColours) {
        try {
            ItemMeta renamedMeta = renamed.getItemMeta();
            //set cp placeholders
            if(usePlaceholders){
                customName = papiNoColour(p,customName);
            }
            if(useColours){
                customName = papi(customName);
            }

            assert renamedMeta != null;
            renamedMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            if (customName != null) {
                renamedMeta.setDisplayName(customName);
            }

            List<String> clore;
            if (lore != null) {
                if(usePlaceholders && useColours){
                    clore = papi(p, lore, true);
                }else if(usePlaceholders){
                    clore = papiNoColour(p, lore);
                }else if(useColours){
                    clore = papi(p, lore, false);
                }else{
                    clore = lore;
                }
                renamedMeta.setLore(clore);
            }
            renamed.setItemMeta(renamedMeta);
        } catch (Exception ignored) {
        }

    }

    private void setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
        } else {
            RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp == null) {
            } else {
                this.econ = (Economy) rsp.getProvider();
            }
        }
    }

    public boolean checkPanels(YamlConfiguration temp) {
        try {
            return temp.contains("panels");
        } catch (Exception var3) {
            return false;
        }
    }

    //regular string papi
    public String papi(Player p, String setpapi) {
        try {
            setpapi = setCpPlaceholders(p,setpapi);
            if (this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                OfflinePlayer offp = getServer().getOfflinePlayer(p.getUniqueId());
                setpapi = PlaceholderAPI.setPlaceholders(offp, setpapi);
            }
            setpapi = translateHexColorCodes(ChatColor.translateAlternateColorCodes('&', setpapi));
            return setpapi;
        }catch(NullPointerException e){
            return setpapi;
        }
    }

    //string papi with no colours
    public String papiNoColour(Player p, String setpapi) {
        try {
            setpapi = setCpPlaceholders(p,setpapi);
            if (this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                OfflinePlayer offp = getServer().getOfflinePlayer(p.getUniqueId());
                setpapi = PlaceholderAPI.setPlaceholders(offp, setpapi);
            }
            return setpapi;
        }catch(NullPointerException e){
            return setpapi;
        }
    }

    //regular string papi, but only colours so Player doesn't need to be there
    public String papi(String setpapi) {
        try {
            setpapi = translateHexColorCodes(ChatColor.translateAlternateColorCodes('&', setpapi));
            return setpapi;
        }catch(NullPointerException e){
            return setpapi;
        }
    }

    //papi except if it is a String List
    public List<String> papi(Player p, List<String> setpapi, boolean placeholders) {
        try {
            if(placeholders) {
                int tempInt = 0;
                for (String temp : setpapi) {
                    setpapi.set(tempInt, setCpPlaceholders(p, temp));
                    tempInt += 1;
                }
                if (this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    OfflinePlayer offp = getServer().getOfflinePlayer(p.getUniqueId());
                    setpapi = PlaceholderAPI.setPlaceholders(offp, setpapi);
                }
            }
        }catch(Exception ignore){
            //this will be ignored as it is probably a null
            return null;
        }
        int tempInt = 0;
        //change colour
        for(String temp : setpapi){
            try {
                setpapi.set(tempInt, translateHexColorCodes(ChatColor.translateAlternateColorCodes('&', temp)));
            }catch(NullPointerException ignore){
            }
            tempInt += 1;
        }
        return setpapi;
    }

    //papi except if it is a String List
    public List<String> papiNoColour(Player p, List<String> setpapi) {
        try {
            int tempInt = 0;
            for (String temp : setpapi) {
                setpapi.set(tempInt, setCpPlaceholders(p, temp));
                tempInt += 1;
            }
            if (this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                OfflinePlayer offp = getServer().getOfflinePlayer(p.getUniqueId());
                setpapi = PlaceholderAPI.setPlaceholders(offp, setpapi);
            }
        }catch(Exception ignore){
            //this will be ignored as it is probably a null
            return null;
        }
        return setpapi;
    }

    public String setCpPlaceholders(Player p, String str) {
        String tag = config.getString("config.format.tag") + " ";
        //replace nodes with PlaceHolders
        str = str.replaceAll("%cp-player-displayname%", p.getDisplayName());
        str = str.replaceAll("%cp-player-name%", p.getName());
        str = str.replaceAll("%cp-player-world%", p.getWorld().getName());
        str = str.replaceAll("%cp-player-x%", String.valueOf(Math.round(p.getLocation().getX())));
        str = str.replaceAll("%cp-player-y%", String.valueOf(Math.round(p.getLocation().getY())));
        str = str.replaceAll("%cp-player-z%", String.valueOf(Math.round(p.getLocation().getZ())));
        str = str.replaceAll("%cp-online-players%", Integer.toString(Bukkit.getServer().getOnlinePlayers().size()));
        //placeholder to check for server availability %cp-server-IP:PORT%
        while (str.contains("%cp-server-")) {
            int start = str.indexOf("%cp-server-");
            int end = str.indexOf("%", str.indexOf("%cp-server-")+1);
            String ip_port = str.substring(start, end).replace("%cp-server-", "").replace("%","");
            Socket s = new Socket();
            try {
                s.connect(new InetSocketAddress(ip_port.split(":")[0], Integer.parseInt(ip_port.split(":")[1])), config.getInt("config.server-ping-timeout"));
                str = str.replace(str.substring(start, end) + "%", papi(p, "true"));
                s.close();
            }catch (IOException ex){
                str = str.replace(str.substring(start, end) + "%", papi(p, "false"));
            }
        }
        //does %cp-random-MIN,MAX%
        while (str.contains("%cp-random-")) {
            int start = str.indexOf("%cp-random-");
            int end = str.indexOf("%", str.indexOf("%cp-random-")+1);
            String min_max = str.substring(start, end).replace("%cp-random-", "").replace("%","");
            int min = Integer.parseInt(min_max.split(",")[0]);
            int max = Integer.parseInt(min_max.split(",")[1]);
            str = str.replace(str.substring(start, end) + "%", String.valueOf(getRandomNumberInRange(min,max)));
        }
        while (str.contains("%cp-player-online-")) {
            int start = str.indexOf("%cp-player-online-");
            int end = str.indexOf("-find%",str.indexOf("%cp-player-online-")+1);
            String playerLocation = str.substring(start, end).replace("%cp-player-online-", "");
            Player[] playerFind = Bukkit.getOnlinePlayers().toArray(new Player[Bukkit.getOnlinePlayers().size()]);
            if (Integer.parseInt(playerLocation) > playerFind.length) {
                str = str.replace(str.substring(start, end) + "-find%", papi(p,Objects.requireNonNull(config.getString("config.format.offline"))));
            } else {
                str = str.replace(str.substring(start, end) + "-find%", playerFind[Integer.parseInt(playerLocation) - 1].getName());
            }
        }
        try {
            if (econ != null) {
                str = str.replaceAll("%cp-player-balance%", String.valueOf(Math.round(econ.getBalance(p))));
            }
        } catch (Exception place) {
            //skip
        }
        if (this.getServer().getPluginManager().isPluginEnabled("TokenManager")) {
            TokenManager api = (TokenManager) Bukkit.getServer().getPluginManager().getPlugin("TokenManager");
            assert api != null;
            str = str.replaceAll("%cp-tokenmanager-balance%", Long.toString(api.getTokens(p).orElse(0)));
        }
        if (this.getServer().getPluginManager().isPluginEnabled("VotingPlugin")) {
            str = str.replaceAll("%cp-votingplugin-points%", String.valueOf(UserManager.getInstance().getVotingPluginUser(p).getPoints()));
        }
        if (str.contains("%cp-player-input%")) {
            for (String[] key : userInputStrings) {
                if (key[0].equals(p.getName())) {
                    userInputStrings.add(new String[]{p.getName(), str});
                    return "commandpanels:commandpanelclose";
                }
            }
            userInputStrings.add(new String[]{p.getName(), str});
            List<String> inputMessages = new ArrayList<String>(config.getStringList("config.input-message"));
            for (String temp : inputMessages) {
                temp = temp.replaceAll("%cp-args%", Objects.requireNonNull(config.getString("config.input-cancel")));
                temp = temp.replaceAll("%cp-tag%", tag);
                p.sendMessage(papi(p, temp));
            }
            str = "commandpanels:commandpanelclose";
        }
        //end nodes with PlaceHolders
        return str;
    }

    //check for duplicate panel names
    public boolean checkDuplicatePanel(CommandSender sender){
        ArrayList<String> apanels = new ArrayList<>();
        for(String[] panelName : panelNames){
            apanels.add(panelName[0]);
        }

        //names is a list of the titles for the Panels
        Set<String> oset = new HashSet<String>(apanels);
        if (oset.size() < apanels.size()) {
            //there are duplicate panel names
            ArrayList<String> opanelsTemp = new ArrayList<String>();
            for(String tempName : apanels){
                if(opanelsTemp.contains(tempName)){
                    sender.sendMessage("[CommandPanels]" + ChatColor.RED + " Error duplicate panel name: " + tempName);
                    return false;
                }
                opanelsTemp.add(tempName);
            }
            return false;
        }
        return true;
    }

    //look through all files in all folders
    public void fileNamesFromDirectory(File directory) {
        for (String fileName : Objects.requireNonNull(directory.list())) {
            if(new File(directory + File.separator + fileName).isDirectory()){
                fileNamesFromDirectory(new File(directory + File.separator + fileName));
                continue;
            }
            int ind = fileName.lastIndexOf(".");
            if(!fileName.substring(ind).equalsIgnoreCase(".yml") && !fileName.substring(ind).equalsIgnoreCase(".yaml")){
                continue;
            }
            //check before adding the file to commandpanels
            if(!checkPanels(YamlConfiguration.loadConfiguration(new File(directory + File.separator + fileName)))){
                this.getServer().getConsoleSender().sendMessage("[CommandPanels]" + ChatColor.RED + " Error in: " + fileName);
                continue;
            }
            panelFiles.add((directory + File.separator + fileName).replace(panelsf.toString() + File.separator,""));
            for (String tempName : Objects.requireNonNull(YamlConfiguration.loadConfiguration(new File(directory + File.separator + fileName)).getConfigurationSection("panels")).getKeys(false)) {
                panelNames.add(new String[]{tempName, Integer.toString(panelFiles.size()-1)});
                if(YamlConfiguration.loadConfiguration(new File(directory + File.separator + fileName)).contains("panels." + tempName + ".open-with-item")) {
                    openWithItem = true;
                }
            }
        }
    }

    public void reloadPanelFiles() {
        panelFiles.clear();
        panelNames.clear();
        openWithItem = false;
        //load panel files
        fileNamesFromDirectory(panelsf);
    }

    public void debug(Exception e) {
        if (debug) {
            e.printStackTrace();
        }
    }

    public void helpMessage(CommandSender p) {
        String tag = config.getString("config.format.tag") + " ";
        p.sendMessage(papi( tag + ChatColor.GREEN + "Commands:"));
        p.sendMessage(ChatColor.GOLD + "/cp <panel> [player:item] [player] " + ChatColor.WHITE + "Open a command panel.");
        if (p.hasPermission("commandpanel.reload")) {
            p.sendMessage(ChatColor.GOLD + "/cpr " + ChatColor.WHITE + "Reloads plugin config.");
        }
        if (p.hasPermission("commandpanel.addons")) {
            p.sendMessage(ChatColor.GOLD + "/cpa " + ChatColor.WHITE + "View downloadable addons for CommandPanels.");
        }
        p.sendMessage(ChatColor.GOLD + "/cpc " + ChatColor.WHITE + "Close current GUI.");
        if (p.hasPermission("commandpanel.generate")) {
            p.sendMessage(ChatColor.GOLD + "/cpg <rows> " + ChatColor.WHITE + "Generate GUI from popup menu.");
        }
        if (p.hasPermission("commandpanel.version")) {
            p.sendMessage(ChatColor.GOLD + "/cpv " + ChatColor.WHITE + "Display the current version.");
        }
        if (p.hasPermission("commandpanel.edit")) {
            p.sendMessage(ChatColor.GOLD + "/cpe [panel] " + ChatColor.WHITE + "Edit a panel with the Panel Editor.");
        }
        if (p.hasPermission("commandpanel.list")) {
            p.sendMessage(ChatColor.GOLD + "/cpl " + ChatColor.WHITE + "Lists the currently loaded panels.");
        }
        if (p.hasPermission("commandpanel.debug")) {
            p.sendMessage(ChatColor.GOLD + "/cpd " + ChatColor.WHITE + "Enable and Disable debug mode globally.");
        }
        if (p.hasPermission("commandpanel.block.add")) {
            p.sendMessage(ChatColor.GOLD + "/cpb add <panel> " + ChatColor.WHITE + "Add panel to a block being looked at.");
        }
        if (p.hasPermission("commandpanel.block.remove")) {
            p.sendMessage(ChatColor.GOLD + "/cpb remove " + ChatColor.WHITE + "Removes any panel assigned to a block looked at.");
        }
        if (p.hasPermission("commandpanel.block.list")) {
            p.sendMessage(ChatColor.GOLD + "/cpb list " + ChatColor.WHITE + "List blocks that will open panels.");
        }
    }

    public final Map<String, Color> colourCodes = new HashMap<String, Color>() {{
        put("AQUA", Color.AQUA);
        put("BLUE", Color.BLUE);
        put("GRAY", Color.GRAY);
        put("GREEN", Color.GREEN);
        put("RED", Color.RED);
        put("WHITE", Color.WHITE);
        put("BLACK", Color.BLACK);
        put("FUCHSIA", Color.FUCHSIA);
        put("LIME", Color.LIME);
        put("MAROON", Color.MAROON);
        put("NAVY", Color.NAVY);
        put("OLIVE", Color.OLIVE);
        put("ORANGE", Color.ORANGE);
        put("PURPLE", Color.PURPLE);
        put("SILVER", Color.SILVER);
        put("TEAL", Color.TEAL);
        put("YELLOW", Color.YELLOW);
    }};

    public Reader getReaderFromStream(InputStream initialStream) throws IOException {
        //this reads the encrypted resource files in the jar file
        if(Bukkit.getVersion().contains("1.13") || legacy.isLegacy()){
            return new Sequence_1_13(this).getReaderFromStream(initialStream);
        }else{
            return new Sequence_1_14(this).getReaderFromStream(initialStream);
        }
    }

    public int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    //used to translate hex colours into ChatColors
    public String translateHexColorCodes(String message) {
        final Pattern hexPattern = Pattern.compile("#" + "([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.COLOR_CHAR + "x"
                    + ChatColor.COLOR_CHAR + group.charAt(0) + ChatColor.COLOR_CHAR + group.charAt(1)
                    + ChatColor.COLOR_CHAR + group.charAt(2) + ChatColor.COLOR_CHAR + group.charAt(3)
                    + ChatColor.COLOR_CHAR + group.charAt(4) + ChatColor.COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }
}