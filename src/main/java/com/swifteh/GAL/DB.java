package com.swifteh.GAL;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Table;
import com.vexsoftware.votifier.model.Vote;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class DB {
    public Logger log;
    public GAL plugin;

    public DB(GAL plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public Connection getConnection() {
        Connection connection;
        String url;
        if (this.plugin.dbMode.equalsIgnoreCase("mysql")) {
            try {
                connection = null;
                url = "jdbc:mysql://" + this.plugin.dbHost + ":" + this.plugin.dbPort + "/" + this.plugin.dbName + "?autoReconnect=true";
                connection = DriverManager.getConnection(url, this.plugin.dbUser, this.plugin.dbPass);
                return connection;
            } catch (SQLException var3) {
                ;
            }
        } else {
            try {
                connection = null;
                url = "jdbc:sqlite:" + this.plugin.getDataFolder().getAbsolutePath() + File.separator + this.plugin.dbFile;
                connection = DriverManager.getConnection(url);
                return connection;
            } catch (SQLException var4) {
                ;
            }
        }

        return null;
    }

    public boolean initConnection() {
        Connection connection;
        if (this.plugin.dbMode.equalsIgnoreCase("mysql")) {
            try {
                connection = null;
                Class.forName("com.mysql.jdbc.Driver");
                String url = "jdbc:mysql://" + this.plugin.dbHost + ":" + this.plugin.dbPort + "?autoReconnect=true";
                connection = DriverManager.getConnection(url, this.plugin.dbUser, this.plugin.dbPass);
                this.log.info("Connection established!");
                List<String> list = new ArrayList();
                Statement st = connection.createStatement();
                DatabaseMetaData meta = connection.getMetaData();
                ResultSet rs = meta.getCatalogs();

                while(rs.next()) {
                    String listofDatabases = rs.getString("TABLE_CAT");
                    list.add(listofDatabases);
                }

                if (list.contains(this.plugin.dbName)) {
                    this.log.info("Found database: " + this.plugin.dbName);
                } else {
                    this.log.info("Database: " + this.plugin.dbName + " was not found, attempting to create.");
                    st.executeUpdate("CREATE DATABASE " + this.plugin.dbName);
                    this.log.info("Successfully created " + this.plugin.dbName);
                }

                rs.close();
                connection.setCatalog(this.plugin.dbName);
                this.log.info("Using database: " + this.plugin.dbName);
                connection.close();
                return true;
            } catch (ClassNotFoundException var10) {
                this.log.severe("JDBC driver not found!");
            } catch (SQLException var11) {
                ;
            }
        } else {
            try {
                connection = null;
                Class.forName("org.sqlite.JDBC");
                File folder = new File(this.plugin.getDataFolder().getAbsolutePath());
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String url = "jdbc:sqlite:" + this.plugin.getDataFolder().getAbsolutePath() + File.separator + this.plugin.dbFile;
                connection = DriverManager.getConnection(url);
                this.log.info("Connection established!");
                connection.close();
                return true;
            } catch (ClassNotFoundException var8) {
                this.log.severe("JDBC driver not found!");
            } catch (SQLException var9) {
                ;
            }
        }

        this.log.severe("############################################");
        this.log.severe("SQL connection failed.  Please check your");
        this.log.severe("database configuration in config.yml");
        this.log.severe("");
        this.log.severe("Type \"/gal reload\" to reload the config");
        this.log.severe("############################################");
        return false;
    }

    public void createTables() {
        String query;
        if (this.plugin.dbMode.equalsIgnoreCase("mysql")) {
            if (!this.tableExists(this.plugin.dbPrefix + "GALTotals")) {
                this.modifyQuery("CREATE TABLE `" + this.plugin.dbPrefix + "GALTotals` (`IGN` varchar(16) NOT NULL, `votes` int(10) DEFAULT 0, `lastvoted` BIGINT(16) DEFAULT 0, PRIMARY KEY (`IGN`));");
            } else {
                query = "SELECT `lastvoted` FROM `" + this.plugin.dbPrefix + "GALTotals` LIMIT 1;";
                if (!this.checkQuery(query)) {
                    this.modifyQuery("ALTER TABLE `" + this.plugin.dbPrefix + "GALTotals` ADD  `lastvoted` BIGINT(16) DEFAULT 0 AFTER `votes`;");
                }
            }

            if (!this.tableExists(this.plugin.dbPrefix + "GALQueue")) {
                this.modifyQuery("CREATE TABLE `" + this.plugin.dbPrefix + "GALQueue` (" + "`IGN` varchar(16) NOT NULL,`service` varchar(64), `timestamp` varchar(32), `ip` varchar(32));");
            }
        } else {
            if (!this.tableExists(this.plugin.dbPrefix + "GALTotals")) {
                query = "CREATE TABLE `" + this.plugin.dbPrefix + "GALTotals` (`IGN` VARCHAR UNIQUE, `votes` INTEGER DEFAULT 0, `lastvoted` INTEGER DEFAULT 0);";
                this.modifyQuery(query);
            } else {
                query = "SELECT `lastvoted` FROM `" + this.plugin.dbPrefix + "GALTotals` LIMIT 1;";
                if (!this.checkQuery(query)) {
                    this.modifyQuery("ALTER TABLE `" + this.plugin.dbPrefix + "GALTotals` ADD `lastvoted` INTEGER DEFAULT 0;");
                }
            }

            if (!this.tableExists(this.plugin.dbPrefix + "GALQueue")) {
                query = "CREATE TABLE `" + this.plugin.dbPrefix + "GALQueue` (`IGN` VARCHAR, `service` VARCHAR, `timestamp` VARCHAR, `ip` VARCHAR);";
                this.modifyQuery(query);
            }
        }

    }

    public boolean tableExists(String table) {
        Connection connection = this.getConnection();
        if (connection == null) {
            return false;
        } else {
            try {
                DatabaseMetaData dmd = connection.getMetaData();
                ResultSet rs = dmd.getTables((String)null, (String)null, table, (String[])null);
                if (rs.next()) {
                    rs.close();
                    connection.close();
                    return true;
                } else {
                    rs.close();
                    connection.close();
                    return false;
                }
            } catch (Exception var6) {
                try {
                    connection.close();
                } catch (SQLException var5) {
                    ;
                }

                return false;
            }
        }
    }

    public void modifyQuery(String query) {
        Connection connection = this.getConnection();
        if (connection != null) {
            try {
                Statement stmt = connection.createStatement();
                stmt.execute(query);
                stmt.close();
            } catch (SQLException var12) {
                ;
            } finally {
                try {
                    connection.close();
                } catch (SQLException var11) {
                    ;
                }

            }

        }
    }

    public boolean checkQuery(String query) {
        Connection connection = this.getConnection();
        boolean success = false;
        if (connection == null) {
            return false;
        } else {
            try {
                Statement stmt = connection.createStatement();
                stmt.executeQuery(query);
                stmt.close();
                success = true;
            } catch (SQLException var13) {
                ;
            } finally {
                try {
                    connection.close();
                } catch (SQLException var12) {
                    ;
                }

            }

            return success;
        }
    }

    public int getVotes(String player) {
        int votes = 0;
        Connection connection = this.getConnection();
        if (connection == null) {
            return votes;
        } else {
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT votes FROM " + this.plugin.dbPrefix + "GALTotals WHERE LOWER(`IGN`) = ?;");
                stmt.setString(1, player);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    votes = rs.getInt(1);
                }

                rs.close();
                stmt.close();
            } catch (SQLException var14) {
                ;
            } finally {
                try {
                    connection.close();
                } catch (SQLException var13) {
                    ;
                }

            }

            return votes;
        }
    }

    public Map<String, Integer> getVoteTop(int limit) {
        Map<String, Integer> votes = new LinkedHashMap();
        Connection connection = this.getConnection();
        if (connection == null) {
            return votes;
        } else {
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM `" + this.plugin.dbPrefix + "GALTotals` ORDER BY `votes` DESC LIMIT ?;");
                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();

                while(rs.next()) {
                    votes.put(rs.getString(1), rs.getInt(2));
                }

                rs.close();
                stmt.close();
            } catch (SQLException var14) {
                ;
            } finally {
                try {
                    connection.close();
                } catch (SQLException var13) {
                    ;
                }

            }

            return votes;
        }
    }

    public ListMultimap<VoteType, GALReward> getQueuedVotes() {
        ListMultimap<VoteType, GALReward> queuedVotes = ArrayListMultimap.create();
        Connection connection = this.getConnection();
        if (connection == null) {
            return queuedVotes;
        } else {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM " + this.plugin.dbPrefix + "GALQueue;");

                while(rs.next()) {
                    Vote vote = new Vote();
                    String player = rs.getString(1);
                    String service = rs.getString(2);
                    String time = rs.getString(3);
                    String ip = rs.getString(4);
                    vote.setUsername(player);
                    vote.setServiceName(service);
                    vote.setAddress(ip);
                    vote.setTimeStamp(time);
                    queuedVotes.put(VoteType.NORMAL, new GALReward(VoteType.NORMAL, service, vote, true));
                }

                rs.close();
                stmt.close();
            } catch (SQLException var18) {
                ;
            } finally {
                try {
                    connection.close();
                } catch (SQLException var17) {
                    ;
                }

            }

            return queuedVotes;
        }
    }

    public Table<String, Integer, Long> getTotals() {
        Table<String, Integer, Long> totals = HashBasedTable.create();
        Connection connection = this.getConnection();
        if (connection == null) {
            return totals;
        } else {
            try {
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM " + this.plugin.dbPrefix + "GALTotals;");

                while(rs.next()) {
                    totals.put(rs.getString(1), rs.getInt(2), rs.getLong(3));
                }

                rs.close();
                stmt.close();
            } catch (SQLException var13) {
                ;
            } finally {
                try {
                    connection.close();
                } catch (SQLException var12) {
                    ;
                }

            }

            return totals;
        }
    }

    public synchronized void setVotes(String player, int votes, boolean increment) {
        Long now = System.currentTimeMillis();
        Connection connection = this.getConnection();
        if (connection != null) {
            try {
                PreparedStatement stmt;
                if (this.plugin.dbMode.equalsIgnoreCase("mysql")) {
                    if (increment) {
                        stmt = connection.prepareStatement("INSERT INTO `" + this.plugin.dbPrefix + "GALTotals` (`IGN`, `votes`, `lastvoted`) VALUES (?, 1, ?) ON DUPLICATE KEY UPDATE `votes` = `votes` + 1, `lastvoted` = ?, `IGN` = ?;");
                        stmt.setString(1, player);
                        stmt.setLong(2, now);
                        stmt.setLong(3, now);
                        stmt.setString(4, player);
                        stmt.executeUpdate();
                        stmt.close();
                    } else {
                        stmt = connection.prepareStatement("INSERT INTO `" + this.plugin.dbPrefix + "GALTotals` (`IGN`, `votes`, `lastvoted`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `votes` = ?, `lastvoted` = ?, `IGN` = ?;");
                        stmt.setString(1, player);
                        stmt.setInt(2, votes);
                        stmt.setLong(3, now);
                        stmt.setInt(4, votes);
                        stmt.setLong(5, now);
                        stmt.setString(6, player);
                        stmt.executeUpdate();
                        stmt.close();
                    }
                } else if (increment) {
                    stmt = connection.prepareStatement("INSERT OR IGNORE INTO `" + this.plugin.dbPrefix + "GALTotals` (`IGN`, `votes`, `lastvoted`) VALUES (?, 0, ?);");
                    stmt.setString(1, player);
                    stmt.setLong(2, now);
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = connection.prepareStatement("UPDATE `" + this.plugin.dbPrefix + "GALTotals` SET `votes` = `votes` + 1, `lastvoted` = ?, `IGN` = ? WHERE LOWER(`IGN`) = ?;");
                    stmt.setLong(1, now);
                    stmt.setString(2, player);
                    stmt.setString(3, player.toLowerCase());
                    stmt.executeUpdate();
                    stmt.close();
                } else {
                    stmt = connection.prepareStatement("INSERT OR IGNORE INTO `" + this.plugin.dbPrefix + "GALTotals` (`IGN`, `votes`, `lastvoted`) VALUES (?, ?, ?);");
                    stmt.setString(1, player);
                    stmt.setInt(2, votes);
                    stmt.setLong(3, now);
                    stmt.executeUpdate();
                    stmt.close();
                    stmt = connection.prepareStatement("UPDATE `" + this.plugin.dbPrefix + "GALTotals` SET `votes` = ?, `lastvoted` = ?, `IGN` = ? WHERE LOWER(`IGN`) = ?;");
                    stmt.setInt(1, votes);
                    stmt.setLong(2, now);
                    stmt.setString(3, player);
                    stmt.setString(4, player.toLowerCase());
                    stmt.executeUpdate();
                    stmt.close();
                }
            } catch (SQLException var15) {
                ;
            } finally {
                try {
                    connection.close();
                } catch (SQLException var14) {
                    ;
                }

            }

        }
    }
}
