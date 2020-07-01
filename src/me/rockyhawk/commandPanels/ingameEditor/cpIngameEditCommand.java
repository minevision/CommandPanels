package me.rockyhawk.commandPanels.ingameEditor;

import me.clip.placeholderapi.PlaceholderAPI;
import me.rockyhawk.commandPanels.commandpanels;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.io.File;
import java.util.*;

public class cpIngameEditCommand implements CommandExecutor {
    commandpanels plugin;

    public cpIngameEditCommand(commandpanels pl) {
        this.plugin = pl;
    }

    @EventHandler
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String tag = plugin.config.getString("config.format.tag") + " ";
        if(!sender.hasPermission("commandpanel.edit")){
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + plugin.config.getString("config.format.perms")));
            return true;
        }
        if(plugin.config.getString("config.ingame-editor").equalsIgnoreCase("false")){
            //this will cancel every /cpe command if ingame-editor is set to false
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + ChatColor.RED + "Editor disabled!"));
            return true;
        }
        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', tag + ChatColor.RED + "Please execute command as a Player!"));
            return true;
        }
        File panelsf = new File(plugin.getDataFolder() + File.separator + "panels");
        ArrayList<String> filenames = new ArrayList<String>(Arrays.asList(panelsf.list()));
        File panelscf = new File(plugin.getDataFolder() + File.separator + "panels" + File.separator + "example.yml"); //cf == correct file
        YamlConfiguration cf; //this is the file to use for any panel.* requests
        String panels = "";
        ArrayList<String> apanels = new ArrayList<String>(); //all panels from all files (titles of panels)
        ArrayList<String> opanels = new ArrayList<String>(); //all panels from all files (raw names of panels)
        String tpanels; //tpanels is the temp to check through the files
        //below is going to go through the files and find the right one
        if (args.length != 0) { //check to make sure the person hasn't just left it empty
            for (int f = 0; filenames.size() > f; f++) { //will loop through all the files in folder
                String key;
                YamlConfiguration temp;
                tpanels = "";
                temp = YamlConfiguration.loadConfiguration(new File(panelsf + File.separator + filenames.get(f)));
                if(!plugin.checkPanels(temp)){
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + plugin.config.getString("config.format.error") + ": File with no Panels found!"));
                    return true;
                }
                for (Iterator var10 = temp.getConfigurationSection("panels").getKeys(false).iterator(); var10.hasNext(); tpanels = tpanels + key + " ") {
                    key = (String) var10.next();
                    apanels.add(temp.getString("panels." + key + ".title"));
                    opanels.add(key);
                }
                tpanels = tpanels.trim();
                //check if the requested panel is in the file
                boolean nfound = true;
                for (int i = 0; tpanels.split("\\s").length - 1 >= i; ++i) {
                    if (args[0].equalsIgnoreCase(tpanels.split("\\s")[i])) {
                        tpanels = tpanels.split("\\s")[i];
                        nfound = false;
                    }
                }
                //if nfound is true it was not found
                if(!nfound){
                    panels = tpanels;
                    panelscf = new File(panelsf + File.separator + filenames.get(f));
                }
            }
            panels = panels.trim();
        }
        cf = YamlConfiguration.loadConfiguration(panelscf);
        //below will start the command, once it got the right file and panel
        if (cmd.getName().equalsIgnoreCase("cpe") || cmd.getName().equalsIgnoreCase("commandpaneledit") || cmd.getName().equalsIgnoreCase("cpanele")) {
            Player p = (Player) sender;
            //names is a list of the titles for the Panels
            Set<String> oset = new HashSet<String>(opanels);
            if (oset.size() < opanels.size()) {
                //there are duplicate panel names
                if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + PlaceholderAPI.setPlaceholders(p, plugin.config.getString("config.format.error") + " panels: You cannot have duplicate panel names!")));
                } else {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + plugin.config.getString("config.format.error") + " panels: You cannot have duplicate panel names!"));
                }
                if(plugin.debug){
                    ArrayList<String> opanelsTemp = new ArrayList<String>();
                    for(String tempName : opanels){
                        if(opanelsTemp.contains(tempName)){
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + ChatColor.RED + " The duplicate panel is: " + tempName));
                            return true;
                        }
                        opanelsTemp.add(tempName);
                    }
                }
                return true;
            }
            Set<String> set = new HashSet<String>(apanels);
            if (set.size() < apanels.size()) {
                //there are duplicate panel names
                if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + PlaceholderAPI.setPlaceholders(p, plugin.config.getString("config.format.error") + " title: You cannot have duplicate title names!")));
                } else {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + plugin.config.getString("config.format.error") + " title: You cannot have duplicate title names!"));
                }
                if(plugin.debug){
                    ArrayList<String> apanelsTemp = new ArrayList<String>();
                    for(String tempName : apanels){
                        if(apanelsTemp.contains(tempName)){
                            p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + ChatColor.RED + " The duplicate title is: " + tempName));
                            return true;
                        }
                        apanelsTemp.add(tempName);
                    }
                }
                return true;
            }
            if (args.length == 0) {
                plugin.openEditorGui(p,0);
                return true;
            }
            if (args.length == 1) {
                boolean nfound = true;

                for (int i = 0; panels.split("\\s").length - 1 >= i; ++i) {
                    if (args[0].equalsIgnoreCase(panels.split("\\s")[i])) {
                        panels = panels.split("\\s")[i];
                        nfound = false;
                    }
                }
                if (nfound) {
                    //if the panel was not found in the message
                    p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + plugin.papi(p, plugin.config.getString("config.format.nopanel"))));
                    return true;
                }else if (!checkconfig(panels, p, cf)) {
                    //if the config is missing an element (message will be sent to user via the public boolean)
                    return true;
                }
                //open editor window here
                plugin.openGui(panels, p, cf,3,0);
                return true;
            }
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + ChatColor.RED + "Usage: /cpe <panel>"));
        return true;
    }
    boolean checkconfig(String panels, Player p, YamlConfiguration pconfig) {
        //if it is missing a section specified it will return false
        String tag = plugin.config.getString("config.format.tag") + " ";
        if(!pconfig.contains("panels." + panels)) {
            if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + PlaceholderAPI.setPlaceholders(p, plugin.config.getString("config.format.nopanel"))));
            } else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + plugin.config.getString("config.format.nopanel")));
            }
            return false;
        }
        if(!pconfig.contains("panels." + panels + ".perm")) {
            if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + PlaceholderAPI.setPlaceholders(p, plugin.config.getString("config.format.error") + " perm: Missing config section!")));
            } else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + plugin.config.getString("config.format.error") + " perm: Missing config section!"));
            }
            return false;
        }
        if(!pconfig.contains("panels." + panels + ".rows")) {
            if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + PlaceholderAPI.setPlaceholders(p, plugin.config.getString("config.format.error") + " rows: Missing config section!")));
            } else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + plugin.config.getString("config.format.error") + " rows: Missing config section!"));
            }
            return false;
        }
        if(!pconfig.contains("panels." + panels + ".title")) {
            if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + PlaceholderAPI.setPlaceholders(p, plugin.config.getString("config.format.error") + " title: Missing config section!")));
            } else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + plugin.config.getString("config.format.error") + " title: Missing config section!"));
            }
            return false;
        }
        if(!pconfig.contains("panels." + panels + ".item")) {
            if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + PlaceholderAPI.setPlaceholders(p, plugin.config.getString("config.format.error") + " item: Missing config section!")));
            } else {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&',tag + plugin.config.getString("config.format.error") + " item: Missing config section!"));
            }
            return false;
        }
        return true;
    }
}