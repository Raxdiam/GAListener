package com.swifteh.GAL;

import com.vexsoftware.votifier.model.Vote;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ProcessReward extends BukkitRunnable {
    private GAL plugin;
    private Logger log;
    private SecureRandom random = new SecureRandom();
    private GALReward reward;
    private int votetotal;

    public ProcessReward(GAL plugin, GALReward reward, int votetotal) {
        this.plugin = plugin;
        this.reward = reward;
        this.log = plugin.log;
        this.votetotal = votetotal;
    }

    public void run() {
        try {
            String username = this.reward.vote.getUsername();
            Player player = this.plugin.getServer().getPlayerExact(username);
            if (player != null) {
                username = player.getName();
            }

            GALVote vote = null;
            int lucky = 0;
            boolean isFakeVote = this.reward.vote.getAddress().equals("fakeVote.local");
            if (isFakeVote) {
                String[] service = this.reward.vote.getServiceName().split("\\|");
                if (service.length > 1) {
                    this.reward.vote.setServiceName(service[0]);

                    try {
                        lucky = Integer.parseInt(service[1]);
                    } catch (NumberFormatException var11) {
                        ;
                    }
                }
            }

            String luckyString;
            String luckString;
            GALVote gVote;
            Iterator var21;
            switch(this.reward.type.ordinal()) {
                case 1:
                case 3:
                    //GALVote gVote;
                    Iterator var8;
                    if (this.plugin.luckyVote && isFakeVote && lucky > 0) {
                        luckyString = String.valueOf(lucky);
                        var8 = this.plugin.galVote.get(VoteType.LUCKY).iterator();

                        while(var8.hasNext()) {
                            gVote = (GALVote)var8.next();

                            try {
                                if (lucky == Integer.parseInt(gVote.key)) {
                                    this.log.info("Player: " + username + " was lucky with number " + luckyString);
                                    (new ProcessReward(this.plugin, new GALReward(VoteType.LUCKY, luckyString, this.reward.vote, false), this.votetotal)).runTaskAsynchronously(this.plugin);
                                    break;
                                }
                            } catch (NumberFormatException var15) {
                                ;
                            }
                        }
                    } else if (this.plugin.luckyVote && player != null) {
                        int luckiest = 0;
                        var8 = this.plugin.galVote.get(VoteType.LUCKY).iterator();

                        while(var8.hasNext()) {
                            gVote = (GALVote)var8.next();
                            boolean var9 = false;

                            int l;
                            try {
                                l = Integer.parseInt(gVote.key);
                            } catch (NumberFormatException var14) {
                                continue;
                            }

                            if (l > 0 && l > luckiest && this.random.nextInt(l) == 0) {
                                luckiest = l;
                            }
                        }

                        if (luckiest > 0) {
                            luckString = String.valueOf(luckiest);
                            Iterator var25 = this.plugin.galVote.get(VoteType.LUCKY).iterator();

                            while(var25.hasNext()) {
                                /*GALVote */gVote = (GALVote)var25.next();

                                try {
                                    if (luckiest == Integer.parseInt(gVote.key)) {
                                        this.log.info("Player: " + username + " was lucky with number " + luckString);
                                        (new ProcessReward(this.plugin, new GALReward(VoteType.LUCKY, luckString, this.reward.vote, false), this.votetotal)).runTaskAsynchronously(this.plugin);
                                        break;
                                    }
                                } catch (NumberFormatException var13) {
                                    ;
                                }
                            }
                        }
                    }

                    if (this.plugin.cumulativeVote) {
                        var21 = this.plugin.galVote.get(VoteType.CUMULATIVE).iterator();

                        while(var21.hasNext()) {
                            gVote = (GALVote)var21.next();

                            try {
                                if (this.votetotal == Integer.parseInt(gVote.key)) {
                                    (new ProcessReward(this.plugin, new GALReward(VoteType.CUMULATIVE, gVote.key, this.reward.vote, false), this.votetotal)).runTaskAsynchronously(this.plugin);
                                    if (player != null) {
                                        this.log.info("Player: " + username + " has voted " + this.votetotal + " times");
                                    } else {
                                        this.log.info("Offline Player: " + username + " has voted " + this.votetotal + " times");
                                    }
                                    break;
                                }
                            } catch (NumberFormatException var12) {
                                ;
                            }
                        }
                    }
                case 2:
            }

            switch(this.reward.type.ordinal()) {
                case 1:
                    var21 = this.plugin.galVote.get(VoteType.NORMAL).iterator();

                    while(var21.hasNext()) {
                        gVote = (GALVote)var21.next();
                        if (this.reward.key.equalsIgnoreCase(gVote.key)) {
                            vote = gVote;
                            break;
                        }
                    }

                    if (vote == null) {
                        var21 = this.plugin.galVote.get(VoteType.NORMAL).iterator();

                        while(var21.hasNext()) {
                            gVote = (GALVote)var21.next();
                            if (gVote.key.equalsIgnoreCase("default")) {
                                vote = gVote;
                                break;
                            }
                        }

                        if (vote == null) {
                            this.log.severe("Default service not found, check your config!");
                        }
                    }
                    break;
                case 2:
                    var21 = this.plugin.galVote.get(VoteType.LUCKY).iterator();

                    while(var21.hasNext()) {
                        gVote = (GALVote)var21.next();
                        if (this.reward.key.equals(gVote.key)) {
                            vote = gVote;
                            break;
                        }
                    }

                    if (vote == null) {
                        this.log.severe("Lucky config key '" + this.reward.key + "' not found, check your config!");
                    }
                    break;
                case 3:
                    var21 = this.plugin.galVote.get(VoteType.PERMISSION).iterator();

                    while(var21.hasNext()) {
                        gVote = (GALVote)var21.next();
                        if (this.reward.key.equalsIgnoreCase(gVote.key)) {
                            vote = gVote;
                            break;
                        }
                    }

                    if (vote == null) {
                        this.log.severe("Perm config key '" + this.reward.key + "' not found, check your config!");
                    }
                    break;
                case 4:
                    var21 = this.plugin.galVote.get(VoteType.CUMULATIVE).iterator();

                    while(var21.hasNext()) {
                        gVote = (GALVote)var21.next();
                        if (this.reward.key.equals(gVote.key)) {
                            vote = gVote;
                            break;
                        }
                    }

                    if (vote == null) {
                        this.log.severe("Cumulative config key '" + this.reward.key + "' not found, check your config!");
                    }
            }

            if (vote == null) {
                return;
            }

            luckyString = this.plugin.formatMessage(vote.message, new Vote[]{this.reward.vote});
            luckString = this.plugin.formatMessage(vote.broadcast, new Vote[]{this.reward.vote});
            List<String> commands = new ArrayList();
            Iterator var10 = vote.commands.iterator();

            while(var10.hasNext()) {
                String command = (String)var10.next();
                commands.add(this.plugin.formatMessage(command, new Vote[]{this.reward.vote}));
            }

            final RewardEvent event = new RewardEvent(this.reward.type, new GALVote(this.reward.key, luckyString, luckString, commands));
            this.plugin.getServer().getScheduler().runTask(this.plugin, new Runnable() {
                public void run() {
                    ProcessReward.this.plugin.getServer().getPluginManager().callEvent(event);
                }
            });
            if (!event.isCancelled()) {
                vote = event.getVote();
                (new RewardTask(this.plugin, vote, this.reward)).runTaskAsynchronously(this.plugin);
            }
        } catch (Exception var16) {
            var16.printStackTrace();
        }

    }
}

