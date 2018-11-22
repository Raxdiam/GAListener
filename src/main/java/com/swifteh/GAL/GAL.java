package com.swifteh.GAL;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class GAL extends JavaPlugin implements Listener {
    public GAL plugin;
    public static GAL p;
    public YamlConfiguration config;
    public ListMultimap<Object, Object> galVote = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    public ListMultimap<Object, Object> queuedVotes = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
    public BlockingQueue<Vote> sqlList = new LinkedBlockingQueue();
    public ConcurrentMap<String, Integer> voteTotals = new ConcurrentHashMap();
    public ConcurrentMap<String, Long> lastVoted = new ConcurrentHashMap();
    public ConcurrentMap<String, String> users = new ConcurrentHashMap();
    public Map<String, String> rewardMessages = new HashMap();
    public Map<String, Long> lastReceived = new HashMap();
    public BukkitTask rewardQueue;
    public BukkitTask voteReminder = null;
    public List<String> voteMessage = new ArrayList(Arrays.asList("{GOLD}-----------------------------------------------------", "Vote for us every day for in game rewards and extras", "{GOLD}-----------------------------------------------------", "{AQUA}You currently have {GREEN}{votes} Votes"));
    public List<String> remindMessage = new ArrayList(Arrays.asList("{GOLD}-----------------------------------------------------", "You have not voted recently, please vote to support the server", "{GOLD}-----------------------------------------------------", "{AQUA}You currently have {GREEN}{votes} Votes"));
    public List<String> joinMessage = new ArrayList(Arrays.asList("{GOLD}-----------------------------------------------------", "Vote for us every day for in game rewards and extras", "{GOLD}-----------------------------------------------------", "{AQUA}You currently have {GREEN}{votes} Votes"));
    public SecureRandom random = new SecureRandom();
    public Logger log;
    public DB db;
    public Commands commands;
    public String dbMode = "sqlite";
    public String dbFile = "GAL.db";
    public String dbHost = "localhost";
    public int dbPort = 3306;
    public String dbUser = "root";
    public String dbPass = "";
    public String dbName = "GAL";
    public String dbPrefix = "";
    public String rewardFormat = "{GREEN}{TOTAL} Votes {GRAY}- {AQUA}{REWARD}";
    public String votetopFormat = "{POSITION}. {GREEN}{username} - {WHITE}{TOTAL}";
    public List<String> rewardHeader = Arrays.asList("{GOLD}---------------- {WHITE}[ {DARK_AQUA}Rewards{WHITE} ] {GOLD}----------------");
    public List<String> votetopHeader = Arrays.asList("{GOLD}---------------- {WHITE}[ {DARK_AQUA}Top Voters{WHITE} ] {GOLD}----------------");
    public List<String> rewardFooter = Arrays.asList("{AQUA}You currently have {GREEN}{votes} Votes");
    public List<String> blockedWorlds = new ArrayList();
    public boolean voteCommand = true;
    public boolean rewardCommand = true;
    public boolean onJoin = true;
    public boolean voteRemind = true;
    public int remindSeconds = 300;
    public int rateLimit = 10;
    public boolean luckyVote = false;
    public boolean permVote = false;
    public boolean cumulativeVote = false;
    public boolean onlineOnly = true;
    public boolean broadcastQueue = true;
    public boolean broadcastRecent = true;
    public boolean broadcastOffline = false;
    public boolean logEnabled = false;
    public boolean getOfflinePlayers = false;
    private FileWriter fw = null;

    public GAL() {
    }

    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        this.plugin = this;
        p = this;
        this.db = new DB(this);
        this.commands = new Commands(this);
        this.log = this.getLogger();
        this.reload();
        this.getCommand("gal").setExecutor(this.commands);
        this.getCommand("vote").setExecutor(this.commands);
        this.getCommand("rewards").setExecutor(this.commands);
        this.getCommand("fakevote").setExecutor(this.commands);
        this.getCommand("votetop").setExecutor(this.commands);
        if (this.getOfflinePlayers) {
            this.getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
                public void run() {
                    long startTime = System.nanoTime();
                    OfflinePlayer[] ops = GAL.this.getServer().getOfflinePlayers();
                    OfflinePlayer[] var7 = ops;
                    int var6 = ops.length;

                    for(int var5 = 0; var5 < var6; ++var5) {
                        OfflinePlayer op = var7[var5];
                        if (op.getName().length() <= 16) {
                            GAL.this.plugin.users.put(op.getName().toLowerCase(), op.getName());
                        }
                    }

                    long duration = System.nanoTime() - startTime;
                    GAL.this.log.info("Took " + duration / 1000000L + "ms to get " + ops.length + " offline players.");
                }
            });
        }

        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException var2) {
            ;
        }

        this.log.info(this.getDescription().getFullName() + " Enabled");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player p = event.getPlayer();
        this.getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            public void run() {
                int queued;
                if (!GAL.this.voteTotals.containsKey(p.getName().toLowerCase())) {
                    queued = GAL.this.db.getVotes(p.getName().toLowerCase());
                    GAL.this.plugin.log.info("Player: " + p.getName() + " has " + queued + " votes");
                    GAL.this.plugin.voteTotals.put(p.getName().toLowerCase(), queued);
                } else {
                    GAL.this.log.info("Player: " + p.getName() + " has " + GAL.this.voteTotals.get(p.getName().toLowerCase()) + " votes");
                }

                if (GAL.this.plugin.users.containsKey(p.getName().toLowerCase()) && !((String)GAL.this.plugin.users.get(p.getName().toLowerCase())).equals(p.getName())) {
                    GAL.this.plugin.users.put(p.getName().toLowerCase(), p.getName());
                    GAL.this.plugin.db.setVotes(p.getName(), (Integer)GAL.this.plugin.voteTotals.get(p.getName().toLowerCase()), false);
                }

                if (!GAL.this.blockedWorlds.contains(p.getWorld().getName())) {
                    queued = GAL.this.plugin.processQueue(p.getName());
                    if (queued > 0) {
                        GAL.this.plugin.log.info("Player: " + p.getName() + " has " + queued + " queued votes");
                    }

                }
            }
        });
        if (this.onJoin) {
            this.getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
                public void run() {
                    Iterator var2 = GAL.this.joinMessage.iterator();

                    while(var2.hasNext()) {
                        String message = (String)var2.next();
                        p.sendMessage(GAL.this.formatMessage(message, (CommandSender)p));
                    }

                }
            }, 20L);
        }

    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        if (!this.blockedWorlds.contains(p.getWorld().getName())) {
            this.processQueue(p.getName());
        }
    }

    public int processQueue(final String name) {
        List<GALReward> playerQueue = new ArrayList();
        ListMultimap var3 = this.queuedVotes;
        synchronized(this.queuedVotes) {
            Iterator i = this.queuedVotes.entries().iterator();

            while(true) {
                if (!i.hasNext()) {
                    break;
                }

                Entry<VoteType, GALReward> entry = (Entry)i.next();
                if (((GALReward)entry.getValue()).vote.getUsername().equalsIgnoreCase(name)) {
                    this.processVote(((GALReward)entry.getValue()).vote, true);
                    playerQueue.add((GALReward)entry.getValue());
                    i.remove();
                }
            }
        }

        if (!playerQueue.isEmpty()) {
            int size = playerQueue.size();
            playerQueue.clear();
            this.getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
                public void run() {
                    GAL.this.plugin.db.modifyQuery("DELETE FROM `" + GAL.this.plugin.dbPrefix + "GALQueue` WHERE LOWER(`IGN`) = '" + name.toLowerCase() + "'");
                }
            });
            return size;
        } else {
            return 0;
        }
    }

    public void reload() {
        this.galVote.clear();
        this.voteMessage.clear();
        this.remindMessage.clear();
        this.joinMessage.clear();
        this.rewardMessages.clear();
        if (this.voteReminder != null) {
            this.voteReminder.cancel();
        }
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }

        File file = new File(this.getDataFolder(), "config.yml");
        if (!file.exists()) {
            InputStream in = GAL.class.getResourceAsStream("/config.yml");
            if (in != null) {
                try {
                    FileOutputStream out = new FileOutputStream(file);
                    byte[] buffer = new byte[8192];
                    boolean var5 = false;

                    int length;
                    while((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }

                    if (in != null) {
                        in.close();
                    }

                    if (out != null) {
                        out.close();
                    }
                } catch (Exception var11) {
                    ;
                }
            }
        }

        this.config = new YamlConfiguration();
        this.config.options().pathSeparator('/');

        try {
            this.config.load(file);
        } catch (FileNotFoundException var8) {
            ;
        } catch (IOException var9) {
            ;
        } catch (InvalidConfigurationException var10) {
            this.log.severe("############################################");
            this.log.severe("Invalid config.yml, please check for errors");
            this.log.severe("at http://yaml-online-parser.appspot.com/");
            this.log.severe("");
            this.log.severe("Type \"/gal reload\" to reload the config");
            this.log.severe("############################################");
        }

        this.blockedWorlds = this.config.getStringList("blocked");
        this.voteMessage = this.config.getStringList("votemessage");
        this.remindMessage = this.config.getStringList("remindmessage");
        this.joinMessage = this.config.getStringList("joinmessage");
        this.voteCommand = this.config.getBoolean("settings/votecommand", true);
        this.rewardCommand = this.config.getBoolean("settings/rewardcommand", true);
        this.onJoin = this.config.getBoolean("settings/joinmessage", true);
        this.voteRemind = this.config.getBoolean("settings/voteremind", true);
        this.remindSeconds = this.config.getInt("settings/remindseconds", 300);
        this.rateLimit = this.config.getInt("settings/ratelimit", 10);
        this.luckyVote = this.config.getBoolean("settings/luckyvote", false);
        this.permVote = this.config.getBoolean("settings/permvote", false);
        this.cumulativeVote = this.config.getBoolean("settings/cumulative", false);
        this.onlineOnly = this.config.getBoolean("settings/onlineonly", true);
        this.getOfflinePlayers = this.config.getBoolean("settings/getofflineplayers", false);
        this.broadcastQueue = this.config.getBoolean("settings/broadcastqueue", true);
        this.broadcastRecent = this.config.getBoolean("settings/broadcastrecent", true);
        this.broadcastOffline = this.config.getBoolean("settings/broadcastoffline", false);
        this.logEnabled = this.config.getBoolean("settings/logfile", false);
        this.rewardFormat = this.config.getString("rewardformat", "{GREEN}{TOTAL} Votes {GRAY}- {AQUA}{REWARD}");
        this.votetopFormat = this.config.getString("votetopformat", "{POSITION}. {GREEN}{username} - {WHITE}{TOTAL}");
        this.rewardHeader = this.config.getStringList("rewardheader");
        this.votetopHeader = this.config.getStringList("votetopheader");
        this.rewardFooter = this.config.getStringList("rewardfooter");
        this.dbMode = this.config.getString("settings/dbMode", "sqlite");
        this.dbFile = this.config.getString("settings/dbFile", "GAL.db");
        this.dbHost = this.config.getString("settings/dbHost", "localhost");
        this.dbPort = this.config.getInt("settings/dbPort", 3306);
        this.dbUser = this.config.getString("settings/dbUser", "root");
        this.dbPass = this.config.getString("settings/dbPass", "");
        this.dbName = this.config.getString("settings/dbName", "GAL");
        this.dbPrefix = this.config.getString("settings/dbPrefix", "");
        ConfigurationSection cs = this.config.getConfigurationSection("services");
        if (cs != null) {
            Iterator i = cs.getKeys(false).iterator();

            while(i.hasNext()) {
                String serviceName = (String)i.next();
                ConfigurationSection serviceConfig = cs.getConfigurationSection(serviceName);
                if (serviceConfig != null) {
                    this.galVote.put(VoteType.NORMAL, new GALVote(serviceName, serviceConfig.getString("playermessage", ""), serviceConfig.getString("broadcast", ""), serviceConfig.getStringList("commands")));
                }
            }
        }

        ConfigurationSection permConfig;
        ConfigurationSection ps;
        Iterator i;
        String permName;
        if (this.luckyVote) {
            ps = this.config.getConfigurationSection("luckyvotes");
            if (ps != null) {
                i = ps.getKeys(false).iterator();

                while(i.hasNext()) {
                    permName = (String)i.next();
                    permConfig = ps.getConfigurationSection(permName);
                    if (permConfig != null) {
                        this.galVote.put(VoteType.LUCKY, new GALVote(permName, permConfig.getString("playermessage", ""), permConfig.getString("broadcast", ""), permConfig.getStringList("commands")));
                    }
                }
            }
        }

        ps = this.config.getConfigurationSection("perms");
        if (ps != null) {
            i = ps.getKeys(false).iterator();

            while(i.hasNext()) {
                permName = (String)i.next();
                permConfig = ps.getConfigurationSection(permName);
                if (permConfig != null) {
                    permName = permName.toLowerCase();
                    this.galVote.put(VoteType.PERMISSION, new GALVote(permName, permConfig.getString("playermessage", ""), permConfig.getString("broadcast", ""), permConfig.getStringList("commands")));
                }
            }
        }

        ConfigurationSection total = this.config.getConfigurationSection("cumulative");
        if (total != null) {
            Iterator i2 = total.getKeys(false).iterator();

            while(i2.hasNext()) {
                String totalName = (String)i2.next();
                ConfigurationSection totalConfig = total.getConfigurationSection(totalName);
                if (totalConfig != null) {
                    this.galVote.put(VoteType.CUMULATIVE, new GALVote(totalName, totalConfig.getString("playermessage", ""), totalConfig.getString("broadcast", ""), totalConfig.getStringList("commands")));
                    this.rewardMessages.put(totalName, totalConfig.getString("rewardmessage"));
                }
            }
        }

        this.getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            public void run() {
                GAL.this.db.initConnection();
                GAL.this.db.createTables();
                GAL.this.queuedVotes.clear();
                GAL.this.log.info("Loading queued votes");
                GAL.this.queuedVotes.putAll(GAL.this.db.getQueuedVotes());
                GAL.this.log.info("Loaded " + GAL.this.queuedVotes.size() + " queued votes");
                Table<String, Integer, Long> totals = GAL.this.db.getTotals();
                Iterator var3 = totals.cellSet().iterator();

                while(var3.hasNext()) {
                    Cell<String, Integer, Long> cell = (Cell)var3.next();
                    String user = (String)cell.getRowKey();
                    GAL.this.voteTotals.put(user.toLowerCase(), (Integer)cell.getColumnKey());
                    GAL.this.lastVoted.put(user.toLowerCase(), (Long)cell.getValue());
                    if (!GAL.this.users.containsKey(user.toLowerCase())) {
                        GAL.this.users.put(user.toLowerCase(), user);
                    }
                }

            }
        });
        if (this.voteRemind) {
            this.voteReminder = (new BukkitRunnable() {
                public void run() {
                    Player[] var4;
                    int var3 = (var4 = GAL.this.getServer().getOnlinePlayers().toArray(new Player[0])).length;

                    for(int var2 = 0; var2 < var3; ++var2) {
                        Player player = var4[var2];
                        String name = player.getName().toLowerCase();
                        if ((!GAL.this.lastVoted.containsKey(name) || (Long)GAL.this.lastVoted.get(name) <= System.currentTimeMillis() - 86400000L) && !player.hasPermission("gal.noremind")) {
                            Iterator var7 = GAL.this.remindMessage.iterator();

                            while(var7.hasNext()) {
                                String message = (String)var7.next();
                                player.sendMessage(GAL.this.formatMessage(message, (CommandSender)player));
                            }
                        }
                    }

                }
            }).runTaskTimer(this, 20L, (long)this.remindSeconds * 20L);
        }

    }

    @EventHandler(
            priority = EventPriority.MONITOR
    )
    public void onVotifierEvent(VotifierEvent event) {
        Vote vote = event.getVote();
        this.log(vote);
        if (vote.getUsername() != null && vote.getUsername().length() > 0) {
            if (!vote.getUsername().trim().matches("[A-Za-z0-9-_]+")) {
                this.log.warning("Vote received for invalid username: " + vote.getUsername());
                return;
            }

            String servicePair = vote.getServiceName() + "\u0000" + vote.getUsername();
            if (this.lastReceived.containsKey(servicePair) && System.currentTimeMillis() - (Long)this.lastReceived.get(servicePair) < (long)(this.rateLimit * 1000)) {
                this.log.warning("Vote received too fast (duplicate vote?) from: " + vote.getServiceName() + " for username: " + vote.getUsername());
                return;
            }

            this.lastReceived.put(servicePair, System.currentTimeMillis());
            String user = vote.getUsername().replaceAll("[^a-zA-Z0-9_\\-]", "");
            user = user.substring(0, Math.min(user.length(), 16));
            vote.setUsername(user);
            if (!vote.getAddress().equals("fakeVote.local")) {
                String service = vote.getServiceName().replaceAll("[^a-zA-Z0-9_\\.\\-]", "");
                vote.setServiceName(service);
            }

            this.processVote(vote);
        } else {
            this.log.info("Vote received on " + vote.getServiceName() + " without IGN, skipping as there is noone to reward...");
        }

    }

    public void processVote(Vote vote) {
        this.processVote(vote, false);
    }

    public void processVote(Vote vote, boolean queued) {
        String playerName = vote.getUsername();
        String serviceName = vote.getServiceName();
        Player exactPlayer = this.getServer().getPlayerExact(playerName);
        if (exactPlayer != null) {
            playerName = exactPlayer.getName();
            vote.setUsername(playerName);
        } else if (this.users.containsKey(playerName.toLowerCase())) {
            playerName = (String)this.users.get(playerName.toLowerCase());
            vote.setUsername(playerName);
        }

        if (!this.onlineOnly && exactPlayer == null) {
            this.log.info("Vote received on " + serviceName + " for Offline Player: " + playerName + ". Trying anyway.");
            this.processReward(new GALReward(VoteType.NORMAL, serviceName, vote, queued));
        } else if (exactPlayer == null) {
            this.log.info("Vote received on " + serviceName + " for Offline Player: " + playerName + ". Adding to Queue for later");
            this.addToQueue(vote);
        } else {
            this.log.info("Vote received on " + serviceName + " for Player: " + playerName);
            if (this.blockedWorlds.contains(exactPlayer.getWorld().getName())) {
                this.log.info("Player: " + exactPlayer.getName() + " is in a blocked world.  Adding to Queue for later.");
                this.addToQueue(vote);
            } else {
                if (this.permVote) {
                    Iterator var7 = this.galVote.get(VoteType.PERMISSION).iterator();

                    while(var7.hasNext()) {
                        GALVote gVote = (GALVote)var7.next();
                        if (exactPlayer.hasPermission("gal." + gVote.key)) {
                            this.processReward(new GALReward(VoteType.PERMISSION, gVote.key, vote, queued));
                            this.log.info("Player: " + exactPlayer.getName() + " has permission: gal." + gVote.key);
                            return;
                        }
                    }
                }

                this.processReward(new GALReward(VoteType.NORMAL, serviceName, vote, queued));
            }
        }
    }

    public void processReward(GALReward reward) {
        String username = reward.vote.getUsername();
        int votetotal = 0;
        if (this.plugin.voteTotals.containsKey(username.toLowerCase())) {
            votetotal = (Integer)this.plugin.voteTotals.get(username.toLowerCase());
        }

        ++votetotal;
        this.plugin.voteTotals.put(username.toLowerCase(), votetotal);
        this.plugin.lastVoted.put(username.toLowerCase(), System.currentTimeMillis());
        p.db.setVotes(username, votetotal, true);
        (new ProcessReward(this, reward, votetotal)).runTaskAsynchronously(this);
    }

    public void onDisable() {
        if (this.voteReminder != null) {
            this.voteReminder.cancel();
        }

        this.getServer().getScheduler().cancelTasks(this);
        this.closeLog();
        this.log.info(this.getDescription().getFullName() + " Disabled");
    }

    public void addToQueue(final Vote vote) {
        this.getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            public void run() {
                String player = vote.getUsername();
                String service = vote.getServiceName();
                String time = vote.getTimeStamp();
                String ip = vote.getAddress();
                GAL.this.plugin.db.modifyQuery("INSERT INTO `" + GAL.this.plugin.dbPrefix + "GALQueue` (`IGN`,`service`,`timestamp`,`ip`) VALUES ('" + player + "','" + service + "','" + time + "','" + ip + "');");
                GAL.this.plugin.queuedVotes.put(VoteType.NORMAL, new GALReward(VoteType.NORMAL, service, vote, true));
                if (GAL.this.plugin.broadcastOffline) {
                    GALVote qVote = null;
                    Iterator var7 = GAL.this.plugin.galVote.get(VoteType.NORMAL).iterator();

                    GALVote gVote;
                    while(var7.hasNext()) {
                        gVote = (GALVote)var7.next();
                        if (service.equalsIgnoreCase(gVote.key)) {
                            qVote = gVote;
                            break;
                        }
                    }

                    if (qVote == null) {
                        var7 = GAL.this.plugin.galVote.get(VoteType.NORMAL).iterator();

                        while(var7.hasNext()) {
                            gVote = (GALVote)var7.next();
                            if (gVote.key.equalsIgnoreCase("default")) {
                                qVote = gVote;
                                break;
                            }
                        }

                        if (qVote == null) {
                            return;
                        }
                    }

                    String broadcast = GAL.this.plugin.formatMessage(qVote.broadcast, vote);
                    if (broadcast.length() > 0) {
                        Player[] var10;
                        int var9 = (var10 = GAL.this.plugin.getServer().getOnlinePlayers().toArray(new Player[0])).length;

                        for(int var8 = 0; var8 < var9; ++var8) {
                            Player p = var10[var8];
                            if (GAL.this.plugin.broadcastRecent || !GAL.this.plugin.lastVoted.containsKey(p.getName().toLowerCase()) || (Long)GAL.this.plugin.lastVoted.get(p.getName().toLowerCase()) <= System.currentTimeMillis() - 86400000L) {
                                p.sendMessage(broadcast);
                            }
                        }
                    }
                }

            }
        });
    }

    public String formatMessage(String message, CommandSender sender) {
        int votes = 0;
        if (this.voteTotals.containsKey(sender.getName().toLowerCase())) {
            votes = (Integer)this.voteTotals.get(sender.getName().toLowerCase());
        }

        return this.formatMessage(message.replace("{votes}", String.valueOf(votes)));
    }

    public String formatMessage(String message, Vote... vote) {
        if (message == null) {
            return "";
        } else {
            String serviceName = "";
            String playerName = "";
            int votes = 0;
            if (vote.length > 0) {
                serviceName = vote[0].getServiceName();
                playerName = vote[0].getUsername();
                if (this.voteTotals.containsKey(playerName.toLowerCase())) {
                    votes = (Integer)this.plugin.voteTotals.get(playerName.toLowerCase());
                }
            }

            if (message.indexOf("/") == 0) {
                message = message.substring(1);
            }

            message = ChatColor.translateAlternateColorCodes('&', message);
            message = message.replace("{servicename}", serviceName).replace("{service}", serviceName).replace("{SERVICE}", serviceName).replace("{name}", playerName).replace("(name)", playerName).replace("{player}", playerName).replace("(player)", playerName).replace("{username}", playerName).replace("(username)", playerName).replace("<name>", playerName).replace("<player>", playerName).replace("<username>", playerName).replace("[name]", playerName).replace("[player]", playerName).replace("[username]", playerName).replace("{AQUA}", "§b").replace("{BLACK}", "§0").replace("{BLUE}", "§9").replace("{DARK_AQUA}", "§3").replace("{DARK_BLUE}", "§1").replace("{DARK_GRAY}", "§8").replace("{DARK_GREEN}", "§2").replace("{DARK_PURPLE}", "§5").replace("{DARK_RED}", "§4").replace("{GOLD}", "§6").replace("{GRAY}", "§7").replace("{GREEN}", "§a").replace("{LIGHT_PURPLE}", "§d").replace("{RED}", "§c").replace("{WHITE}", "§f").replace("{YELLOW}", "§e").replace("{BOLD}", "§l").replace("{ITALIC}", "§o").replace("{MAGIC}", "§k").replace("{RESET}", "§r").replace("{STRIKE}", "§m").replace("{STRIKETHROUGH}", "§m").replace("{UNDERLINE}", "§n").replace("{votes}", String.valueOf(votes));
            return message;
        }
    }

    public void log(Vote vote) {
        if (this.logEnabled && vote != null) {
            if (this.fw == null) {
                try {
                    this.fw = new FileWriter(new File(this.getDataFolder(), "vote.log"), true);
                } catch (IOException var5) {
                    this.log.log(Level.SEVERE, (String)null, var5);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append(DateFormat.getDateTimeInstance(0, 0).format(new Date())).append(" ").append(vote).append("\n");

            try {
                this.fw.write(sb.toString());
                this.fw.flush();
            } catch (IOException var4) {
                this.log.log(Level.SEVERE, (String)null, var4);
            }

        }
    }

    public void closeLog() {
        if (this.fw != null) {
            try {
                this.fw.close();
            } catch (IOException var2) {
                this.log.log(Level.SEVERE, (String)null, var2);
            }

            this.fw = null;
        }

    }
}
