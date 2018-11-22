package com.swifteh.GAL;

import com.google.common.collect.ListMultimap;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Commands implements CommandExecutor {
    private GAL plugin;

    public Commands(GAL plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName();
        String arg1;
        String arg3;
        int votes;
        String arg2;
        if (cmdName.equalsIgnoreCase("gal")) {
            arg1 = args.length > 0 ? args[0] : null;
            arg2 = args.length > 1 ? args[1] : null;
            arg3 = args.length > 2 ? args[2] : null;
            if (arg1 == null) {
                sender.sendMessage("- /gal reload | clearqueue | cleartotals | forcequeue | total <player> <total> | clear <player> | top [count] | votes <player> | broadcast <message>");
                return true;
            } else if (arg1.equalsIgnoreCase("reload")) {
                if (!sender.isOp() && !sender.hasPermission("gal.admin")) {
                    return false;
                } else {
                    this.plugin.reload();
                    sender.sendMessage("Reloaded " + this.plugin.getDescription().getFullName());
                    this.plugin.log.info("Reloaded " + this.plugin.getDescription().getFullName());
                    return true;
                }
            } else if (arg1.equalsIgnoreCase("cleartotals")) {
                if (!sender.isOp() && !sender.hasPermission("gal.admin")) {
                    return false;
                } else {
                    this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                        public void run() {
                            Commands.this.plugin.db.modifyQuery("DELETE FROM `" + Commands.this.plugin.dbPrefix + "GALTotals`;");
                        }
                    });
                    this.plugin.voteTotals.clear();
                    sender.sendMessage("Reset vote totals");
                    return true;
                }
            } else if (arg1.equalsIgnoreCase("clearqueue")) {
                if (!sender.isOp() && !sender.hasPermission("gal.admin")) {
                    return false;
                } else {
                    this.plugin.queuedVotes.clear();
                    this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                        public void run() {
                            Commands.this.plugin.db.modifyQuery("DELETE FROM `" + Commands.this.plugin.dbPrefix + "GALQueue`;");
                        }
                    });
                    this.plugin.queuedVotes.clear();
                    sender.sendMessage("Cleared vote queue");
                    return true;
                }
            } else if (arg1.equalsIgnoreCase("forcequeue")) {
                if (!sender.isOp() && !sender.hasPermission("gal.admin")) {
                    return false;
                } else {
                    this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                        public void run() {
                            if (Commands.this.plugin.queuedVotes.isEmpty()) {
                                sender.sendMessage("There are no queued votes!");
                            } else {
                                sender.sendMessage("Processing " + Commands.this.plugin.queuedVotes.size() + " votes...");
                                synchronized(Commands.this.plugin.queuedVotes) {
                                    Iterator i = Commands.this.plugin.queuedVotes.entries().iterator();

                                    while(true) {
                                        if (!i.hasNext()) {
                                            break;
                                        }

                                        Entry<VoteType, GALReward> entry = (Entry)i.next();
                                        Vote vote = ((GALReward)entry.getValue()).vote;
                                        Commands.this.plugin.log.info("Forcing queued vote for " + vote.getUsername() + " on " + vote.getServiceName());
                                        Commands.this.plugin.processReward(new GALReward((VoteType)entry.getKey(), vote.getServiceName(), vote, false));
                                        i.remove();
                                    }
                                }

                                Commands.this.plugin.db.modifyQuery("DELETE FROM `" + Commands.this.plugin.dbPrefix + "GALQueue`;");
                            }
                        }
                    });
                    return true;
                }
            } else if (arg1.equalsIgnoreCase("top")) {
                if (!sender.isOp() && !sender.hasPermission("gal.admin")) {
                    return false;
                } else {
                    votes = 10;

                    try {
                        votes = arg2 == null ? 10 : Integer.parseInt(arg2);
                    } catch (NumberFormatException var13) {
                        ;
                    }

                    this.voteTop(sender, votes);
                    return true;
                }
            } else {
                final int i;
                String user;
                if (arg1.equalsIgnoreCase("total")) {
                    if (!sender.isOp() && !sender.hasPermission("gal.admin")) {
                        return false;
                    } else if (arg3 == null) {
                        sender.sendMessage("- /gal total <player> <total>");
                        return true;
                    } else {
                        user = arg2.replaceAll("[^a-zA-Z0-9_\\-]", "");
                        user = user.substring(0, Math.min(user.length(), 16));
                        boolean var22 = false;

                        try {
                            i = Integer.parseInt(arg3);
                        } catch (NumberFormatException var14) {
                            sender.sendMessage("- /gal total <player> <total>");
                            return true;
                        }

                        final String userFinal = user;
                        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                            public void run() {
                                Commands.this.plugin.db.setVotes(userFinal, i, false);
                            }
                        });
                        this.plugin.voteTotals.put(user.toLowerCase(), i);
                        this.plugin.lastVoted.put(user.toLowerCase(), System.currentTimeMillis());
                        sender.sendMessage("Setting " + user + "'s total votes to: " + i);
                        return true;
                    }
                } else if (arg1.equalsIgnoreCase("clear")) {
                    if (!sender.isOp() && !sender.hasPermission("gal.admin")) {
                        return false;
                    } else if (arg2 == null) {
                        sender.sendMessage("- /gal clear <player>");
                        return true;
                    } else {
                        arg2 = arg2.replaceAll("[^a-zA-Z0-9_\\-]", "");
                        user = arg2.substring(0, Math.min(arg2.length(), 16));
                        final String userFinal = user;
                        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                            public void run() {
                                Commands.this.plugin.db.modifyQuery("DELETE FROM `" + Commands.this.plugin.dbPrefix + "GALQueue` WHERE LOWER(`IGN`) = '" + userFinal.toLowerCase() + "'");
                            }
                        });
                        ListMultimap var21 = this.plugin.queuedVotes;
                        synchronized(this.plugin.queuedVotes) {
                            Iterator i2 = this.plugin.queuedVotes.entries().iterator();

                            while(i2.hasNext()) {
                                Entry<VoteType, GALReward> entry = (Entry)i2.next();
                                if (((GALReward)entry.getValue()).vote.getUsername().equalsIgnoreCase(user)) {
                                    i2.remove();
                                }
                            }
                        }

                        sender.sendMessage("Clearing " + user + "'s queued votes");
                        return true;
                    }
                } else if (!arg1.equalsIgnoreCase("broadcast")) {
                    if (arg1.equalsIgnoreCase("votes")) {
                        if (!sender.isOp() && !sender.hasPermission("gal.admin")) {
                            return false;
                        } else if (arg2 == null) {
                            sender.sendMessage("- /gal votes <player>");
                            return true;
                        } else {
                            votes = 0;
                            if (this.plugin.voteTotals.containsKey(arg2.toLowerCase())) {
                                votes = (Integer)this.plugin.voteTotals.get(arg2.toLowerCase());
                            }

                            sender.sendMessage("Player: " + arg2 + " has " + votes + " votes");
                            return true;
                        }
                    } else {
                        sender.sendMessage("- /gal reload | clearqueue | cleartotals | total <player> <total> | clear <player> | top [count]");
                        return true;
                    }
                } else if (!sender.isOp() && !sender.hasPermission("gal.admin")) {
                    return false;
                } else if (args.length <= 1) {
                    return false;
                } else {
                    StringBuilder sb = new StringBuilder();

                    for(int j = 1; j < args.length; ++j) {
                        sb.append(args[j]).append(" ");
                    }

                    this.plugin.getServer().broadcastMessage(this.plugin.formatMessage(sb.toString().trim(), new Vote[0]));
                    return true;
                }
            }
        } else if (cmdName.equalsIgnoreCase("fakevote")) {
            if (!sender.isOp() && !sender.hasPermission("gal.admin")) {
                return false;
            } else {
                arg1 = args.length > 0 ? args[0] : null;
                arg2 = args.length > 1 ? args[1] : null;
                arg3 = args.length > 2 ? args[2] : null;
                if (arg1 == null) {
                    sender.sendMessage("- /fakevote <player> [servicename] [luckynumber]");
                    return true;
                } else {
                    votes = 0;
                    if (arg3 != null) {
                        try {
                            votes = Integer.parseInt(arg3);
                        } catch (NumberFormatException var15) {
                            ;
                        }
                    }

                    Vote fakeVote = new Vote();
                    fakeVote.setUsername(arg1);
                    StringBuilder service = new StringBuilder();
                    service.append(arg2 == null ? "fakeVote" : arg2);
                    if (arg3 != null) {
                        service.append("|").append(votes);
                    }

                    fakeVote.setServiceName(service.toString());
                    fakeVote.setAddress("fakeVote.local");
                    fakeVote.setTimeStamp(String.valueOf(System.currentTimeMillis()));
                    this.plugin.getServer().getPluginManager().callEvent(new VotifierEvent(fakeVote));
                    sender.sendMessage("sent fake vote!");
                    this.plugin.log.info("Sent fake vote: " + fakeVote.toString());
                    return true;
                }
            }
        } else {
            Iterator var7;
            if (cmdName.equalsIgnoreCase("vote")) {
                if (!this.plugin.voteCommand) {
                    return false;
                } else {
                    var7 = this.plugin.voteMessage.iterator();

                    while(var7.hasNext()) {
                        arg1 = (String)var7.next();
                        sender.sendMessage(this.plugin.formatMessage(arg1, sender));
                    }

                    return true;
                }
            } else if (!cmdName.equalsIgnoreCase("rewards")) {
                if (cmdName.equalsIgnoreCase("votetop")) {
                    if (!sender.isOp() && !sender.hasPermission("gal.admin") && !sender.hasPermission("gal.top")) {
                        return false;
                    } else {
                        this.voteTop(sender, 10);
                        return true;
                    }
                } else {
                    return false;
                }
            } else if (this.plugin.rewardCommand && this.plugin.cumulativeVote) {
                var7 = this.plugin.rewardHeader.iterator();

                while(var7.hasNext()) {
                    arg1 = (String)var7.next();
                    sender.sendMessage(this.plugin.formatMessage(arg1, sender));
                }

                var7 = this.plugin.galVote.get(VoteType.CUMULATIVE).iterator();

                while(var7.hasNext()) {
                    GALVote gVote = (GALVote)var7.next();
                    if (this.plugin.rewardMessages.containsKey(gVote.key)) {
                        arg3 = this.plugin.rewardFormat.replace("{TOTAL}", gVote.key).replace("{REWARD}", (CharSequence)this.plugin.rewardMessages.get(gVote.key));
                        sender.sendMessage(this.plugin.formatMessage(arg3, sender));
                    }
                }

                var7 = this.plugin.rewardFooter.iterator();

                while(var7.hasNext()) {
                    arg1 = (String)var7.next();
                    sender.sendMessage(this.plugin.formatMessage(arg1, sender));
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public void voteTop(final CommandSender sender, final int count) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
            public void run() {
                int place = 1;
                Map<String, Integer> votes = Commands.this.plugin.db.getVoteTop(count);
                Iterator var4 = Commands.this.plugin.votetopHeader.iterator();

                while(var4.hasNext()) {
                    String messagex = (String)var4.next();
                    sender.sendMessage(Commands.this.plugin.formatMessage(messagex, sender));
                }

                for(var4 = votes.entrySet().iterator(); var4.hasNext(); ++place) {
                    Entry<String, Integer> entry = (Entry)var4.next();
                    String user = (String)entry.getKey();
                    int total = (Integer)entry.getValue();
                    if (Commands.this.plugin.users.containsKey(user.toLowerCase())) {
                        user = (String)Commands.this.plugin.users.get(user.toLowerCase());
                    }

                    String message = Commands.this.plugin.votetopFormat.replace("{POSITION}", String.valueOf(place)).replace("{TOTAL}", String.valueOf(total)).replace("{username}", user);
                    sender.sendMessage(Commands.this.plugin.formatMessage(message, new Vote[0]));
                }

            }
        });
    }
}
