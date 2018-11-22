package com.swifteh.GAL;

import com.google.common.collect.ListMultimap;
import com.vexsoftware.votifier.model.Vote;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.bukkit.entity.Player;

public class VoteAPI {
    public VoteAPI() {
    }

    public static int getVoteTotal(String name) {
        return GAL.p.voteTotals.containsKey(name.toLowerCase()) ? (Integer)GAL.p.voteTotals.get(name.toLowerCase()) : 0;
    }

    public static int getVoteTotal(Player player) {
        return getVoteTotal(player.getName());
    }

    public static long getLastVoteTime(String name) {
        return GAL.p.lastVoted.containsKey(name.toLowerCase()) ? (Long)GAL.p.lastVoted.get(name.toLowerCase()) : 0L;
    }

    public static long getLastVoteTime(Player player) {
        return getLastVoteTime(player.getName());
    }

    public static void nameChange(String from, String to) {
        from = from.replaceAll("[^a-zA-Z0-9_\\-]", "");
        to = to.replaceAll("[^a-zA-Z0-9_\\-]", "");
        final String oldUser = from.substring(0, Math.min(from.length(), 16));
        final String newUser = to.substring(0, Math.min(to.length(), 16));
        GAL.p.getServer().getScheduler().runTaskAsynchronously(GAL.p, new Runnable() {
            public void run() {
                List<GALReward> playerQueue = new ArrayList();
                ListMultimap var2 = GAL.p.queuedVotes;
                Iterator i;
                synchronized(GAL.p.queuedVotes) {
                    i = GAL.p.queuedVotes.entries().iterator();

                    while(i.hasNext()) {
                        Entry<VoteType, GALReward> entry = (Entry)i.next();
                        if (((GALReward)entry.getValue()).vote.getUsername().equalsIgnoreCase(oldUser)) {
                            playerQueue.add((GALReward)entry.getValue());
                            i.remove();
                        }
                    }
                }

                GAL.p.db.modifyQuery("DELETE FROM `" + GAL.p.dbPrefix + "GALQueue` WHERE LOWER(`IGN`) = '" + oldUser.toLowerCase() + "'");
                i = playerQueue.iterator();

                while(i.hasNext()) {
                    GALReward reward = (GALReward)i.next();
                    Vote vote = reward.vote;
                    vote.setUsername(newUser);
                    GALReward newReward = new GALReward(reward.type, vote.getServiceName(), vote, true);
                    GAL.p.db.modifyQuery("INSERT INTO `" + GAL.p.dbPrefix + "GALQueue` (`IGN`,`service`,`timestamp`,`ip`) VALUES ('" + newUser.toLowerCase() + "','" + vote.getServiceName() + "','" + vote.getTimeStamp() + "','" + vote.getAddress() + "');");
                    GAL.p.queuedVotes.put(newReward.type, newReward);
                }

                int votes = 0;
                if (GAL.p.voteTotals.containsKey(oldUser.toLowerCase())) {
                    votes = (Integer)GAL.p.voteTotals.get(oldUser.toLowerCase());
                }

                if (votes > 0) {
                    GAL.p.db.setVotes(oldUser, 0, false);
                    GAL.p.voteTotals.put(oldUser.toLowerCase(), 0);
                    GAL.p.db.setVotes(newUser, 0, false);
                    GAL.p.voteTotals.put(newUser.toLowerCase(), votes);
                }

            }
        });
    }
}

