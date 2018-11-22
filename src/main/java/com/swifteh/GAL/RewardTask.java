package com.swifteh.GAL;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RewardTask extends BukkitRunnable {
    private GAL plugin;
    private List<String> commands = new ArrayList();
    private String message = "";
    private String broadcast = "";
    private boolean shouldBroadcast = true;
    private Player player;
    private String username = "";

    public RewardTask(GAL plugin, GALVote vote, GALReward reward) {
        this.plugin = plugin;
        this.message = vote.message;
        this.broadcast = vote.broadcast;
        this.commands = vote.commands;
        if (reward.queued) {
            this.shouldBroadcast = plugin.broadcastQueue && !plugin.broadcastOffline;
        }

        this.username = reward.vote.getUsername();
        this.player = plugin.getServer().getPlayerExact(this.username);
        if (this.player != null) {
            this.username = this.player.getName();
        }

    }

    public void run() {
        int var2;
        int var3;
        if (this.broadcast.length() > 0 && this.shouldBroadcast) {
            Player[] var4;
            var3 = (var4 = this.plugin.getServer().getOnlinePlayers().toArray(new Player[0])).length;

            for(var2 = 0; var2 < var3; ++var2) {
                Player p = var4[var2];
                if (this.plugin.broadcastRecent || p.getName().equalsIgnoreCase(this.username) || !this.plugin.lastVoted.containsKey(p.getName().toLowerCase()) || (Long)this.plugin.lastVoted.get(p.getName().toLowerCase()) <= System.currentTimeMillis() - 86400000L) {
                    String[] var8;
                    int var7 = (var8 = this.broadcast.split("\\\\n")).length;

                    for(int var6 = 0; var6 < var7; ++var6) {
                        String b = var8[var6];
                        p.sendMessage(b);
                    }
                }
            }
        }

        String m;
        if (this.message.length() > 0 && this.player != null) {
            String[] var12;
            var3 = (var12 = this.message.split("\\\\n")).length;

            for(var2 = 0; var2 < var3; ++var2) {
                m = var12[var2];
                this.player.sendMessage(m);
            }
        }

        Iterator var11 = this.commands.iterator();

        while(var11.hasNext()) {
            m = (String)var11.next();
            final String mNew = m;
            this.plugin.getServer().getScheduler().runTask(this.plugin, new Runnable() {
                public void run() {
                    RewardTask.this.plugin.getServer().dispatchCommand(RewardTask.this.plugin.getServer().getConsoleSender(), mNew);
                }
            });

            try {
                Thread.sleep(50L);
            } catch (InterruptedException var9) {
                ;
            }
        }

    }
}

