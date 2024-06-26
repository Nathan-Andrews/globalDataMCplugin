package net.minermen;


import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import net.kyori.adventure.text.Component;

import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Utils {
    public static boolean areNotEqual(Integer a, Integer b) {
        if (a == null && b == null) {
            return false; // Both are null, so they are equal
        }
        if (a == null || b == null) {
            return true; // One is null and the other is not, so they are not equal
        }
        return !a.equals(b); // Both are non-null, compare their values
    }
}


public class GlobalData extends JavaPlugin {

    private File customConfigFile;
    private FileConfiguration customConfig;

    private String filepath;
    private String objectivePattern;
    // private Set<String> objectives;
    // private String playername = "underminerman";

    // private int storedScore = 0;
    private Hashtable<String,ScoreboardStorage> storedScoreboards;
    private Hashtable<String,ScoreboardStorage> sharedScoreboards;
    private Hashtable<String,ScoreboardStorage> currentScoreboards;

    // JSONObject sharedStorageJson;

    SQLite db = new SQLite();

    @Override
    public void onEnable() {
        createCustomConfig();

        filepath = this.getCustomConfig().getString("directory_path");
        String filename = this.getCustomConfig().getString("storage_filename");
        objectivePattern = this.getCustomConfig().getString("objectives");

        if (filepath == null || filepath.equals("") || filename == null || filename.equals("")) {
            getLogger().severe("Set a filepath for storage in the config file: ./plugins/GlobalData/config.yml");

            disablePlugin();
            return;
        }

        filepath = Paths.get(filepath, "GlobalData").toString();

        System.out.println(filepath);
        
        db.initialize(filepath,filename);

        storedScoreboards = new Hashtable<String,ScoreboardStorage>();

        getCurrentScoreboard();
        getSharedScoreboards();

        Set<String> objectives = getObjectiveNames();

        for (String objective : objectives) {
            ScoreboardStorage scoreboard = new ScoreboardStorage(objective);

            storedScoreboards.put(objective,scoreboard);
        }

        saveSharedStorage();

        startTick();

        getLogger().info("GlobalData plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GlobalData plugin has been disabled.");
    }

    private void disablePlugin() {
        Bukkit.getServer().getPluginManager().disablePlugin(this);
    }

    private void startTick() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // This code will run every tick (20 times per second)
                tick();
            }
        }.runTaskTimer(this, 0L, 1L); // 0L is the initial delay, 1L is the period (1 tick)
    }

    public FileConfiguration getCustomConfig() {
        return this.customConfig;
    }

    private void createCustomConfig() {
        customConfigFile = new File(getDataFolder(), "config.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
         }

        customConfig = new YamlConfiguration();
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    

    private void tick() {
        getSharedScoreboards();
        getCurrentScoreboard();

        Set<String> objectiveNames = getObjectiveNames();

        for (String objectiveName : objectiveNames) {
            ScoreboardStorage currentScoreboard = currentScoreboards.containsKey(objectiveName) ? currentScoreboards.get(objectiveName) : new ScoreboardStorage(objectiveName);
            ScoreboardStorage storedScoreboard = storedScoreboards.get(objectiveName);
            ScoreboardStorage sharedScoreboard = sharedScoreboards.containsKey(objectiveName) ? sharedScoreboards.get(objectiveName) : new ScoreboardStorage(objectiveName);
            
            if (storedScoreboard == null) {
                if (! db.getObjectiveNames().contains(objectiveName) && currentScoreboards.containsKey(objectiveName)) {
                    storeNewObjective(objectiveName);
                }
                if (db.getObjectiveNames().contains(objectiveName) && ! currentScoreboards.containsKey(objectiveName)) {
                    registerNewObjective(objectiveName);
                }

                storedScoreboard = new ScoreboardStorage(objectiveName);
                storedScoreboards.put(objectiveName,storedScoreboard);
            }
            else {
                if (db.getObjectiveNames().contains(objectiveName) && !currentScoreboards.containsKey(objectiveName)) {
                    db.removeObjective(objectiveName);

                    storedScoreboards.remove(objectiveName);
                }
                if (!db.getObjectiveNames().contains(objectiveName) && currentScoreboards.containsKey(objectiveName)) {
                    removeObjective(objectiveName);

                    storedScoreboards.remove(objectiveName);
                }
            }

            Set<String> currentPlayers = currentScoreboard.getPlayers();
            Set<String> storedPlayers = storedScoreboard.getPlayers();
            Set<String> sharedPlayers = sharedScoreboard.getPlayers();
    
            Set<String> players = Stream.concat(Stream.concat(currentPlayers.stream(), storedPlayers.stream()),sharedPlayers.stream())
                .collect(Collectors.toSet());
    
            for (String player : players) {
                Integer currentScore = currentScoreboard.getPlayerScore(player);
                Integer storedScore = storedScoreboard.getPlayerScore(player);
                Integer sharedScore = sharedScoreboard.getPlayerScore(player);
    
                if (Utils.areNotEqual(storedScore, currentScore)) {
                    // getLogger().info(storedScore + ": " + currentScore);
                    if (currentScore == null) {
                        storedScoreboard.resetScore(player);

                        db.resetPlayerScore(objectiveName, player);
                    }
                    else {
                        storedScore = currentScore;
    
                        storedScoreboard.setScore(player, storedScore);

                        db.setPlayerScore(objectiveName, player, storedScore);
                    }
    
                    // getLogger().info("value changed");
                }
                else if (Utils.areNotEqual(storedScore, sharedScore)) {
                    if (sharedScore == null) {
                        resetPlayerScore(objectiveName,player);
                        storedScoreboard.resetScore(player);
                    }
                    else {
                        // getLogger().info(storedScore + ", " + sharedScore);
                        storedScore = sharedScore;
    
                        storedScoreboard.setScore(player, storedScore);
    
                        setPlayerScore(objectiveName,player,storedScore);
                    }
    
                    // getLogger().info("value changed server");
                }
            }
        }
    }

    private void getSharedScoreboards() {
        Set<String> objectiveNames = db.getObjectiveNames();

        sharedScoreboards = new Hashtable<String,ScoreboardStorage>();

        for (String objectiveName : objectiveNames) {
            if (! isTrackedObjective(objectiveName)) continue;

            ScoreboardStorage scoreboard = new ScoreboardStorage(objectiveName);

            Set<String> players = db.getPlayers(objectiveName);

            for (String player : players) {
                Integer s = db.getPlayerScore(objectiveName, player);

                if (s != null) scoreboard.setScore(player, s.intValue());
            }

            sharedScoreboards.put(objectiveName,scoreboard);
        }
    }

    private void getCurrentScoreboard() {
        currentScoreboards = new Hashtable<String,ScoreboardStorage>();

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            
            Set<String> objectiveNames = new HashSet<String>();

            for (Objective objective : board.getObjectives()) {
                String objectiveName = objective.getName();
                
                if (isTrackedObjective(objectiveName)) objectiveNames.add(objective.getName());
            }


            for (String objectiveName : objectiveNames) {
                Objective objective = board.getObjective(objectiveName);

                ScoreboardStorage scoreboard = new ScoreboardStorage(objectiveName);

                if (objective != null) {
                    // Get all entries (players/teams) that have scores in this objective

                    for (String entry : board.getEntries()) {
                        Score score = objective.getScore(entry);
                        if (! score.isScoreSet()) {
                            continue;
                        }

                        scoreboard.setScore(entry, score.getScore());
                    }
                }

                currentScoreboards.put(objectiveName,scoreboard);
            }
        }
        else {
            getLogger().warning("ScoreboardManager is null!");
        }
    }

    private void registerNewObjective(String objectiveName) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();

            board.registerNewObjective(objectiveName, Criteria.DUMMY, Component.text(objectiveName));
        }
    }

    private void removeObjective(String objectiveName) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            Objective objective = board.getObjective(objectiveName);

            objective.unregister();
        }
    }

    private Set<String> getObjectiveNames() {
        Set<String> storedObjectiveNames = new HashSet<String>();
        for (String objectiveName : db.getObjectiveNames()) {
            if (isTrackedObjective(objectiveName)) storedObjectiveNames.add(objectiveName);
        }

        return Stream.concat(
            Stream.concat(storedObjectiveNames.stream(),
            currentScoreboards.keySet().stream()),
            storedScoreboards.keySet().stream())
            .collect(Collectors.toSet());
    }

    private void storeNewObjective(String objectiveName) {
        db.addObjective(objectiveName);
    }

    private boolean isTrackedObjective(String objectiveName) {
        return Pattern.matches(objectivePattern,objectiveName);
    }

    private void setPlayerScore(String objectiveName, String playerName, int scoreValue) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            Objective objective = board.getObjective(objectiveName);
            if (objective == null) {
                objective = board.registerNewObjective(objectiveName, Criteria.DUMMY, Component.text(objectiveName));
                // objective = board.registerNewObjective(objectiveName, "dummy", Component.text(objectiveName)); // 1.18.1 method
            }
            Score score = objective.getScore(playerName);
            score.setScore(scoreValue);
        } else {
            getLogger().warning("ScoreboardManager is null!");
        }
    }

    private void resetPlayerScore(String objectiveName,String playerName) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            Objective objective = board.getObjective(objectiveName);
            if (objective == null) {
                objective = board.registerNewObjective(objectiveName, Criteria.DUMMY, Component.text(objectiveName));
                // objective = board.registerNewObjective(objectiveName, "dummy", Component.text(objectiveName)); // 1.18.1 method
            }
            Score score = objective.getScore(playerName);
            score.resetScore();;
        } else {
            getLogger().warning("ScoreboardManager is null!");
        }
    }
    

    private void saveSharedStorage() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();

        Set<String> objectiveNames = getObjectiveNames();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            for (String objectiveName : objectiveNames) {
                ScoreboardStorage currentScoreboard = currentScoreboards.containsKey(objectiveName) ? currentScoreboards.get(objectiveName) : new ScoreboardStorage(objectiveName);
                ScoreboardStorage sharedScoreboard = sharedScoreboards.containsKey(objectiveName) ? sharedScoreboards.get(objectiveName) : new ScoreboardStorage(objectiveName);

                Set<String> currentPlayers = currentScoreboard.getPlayers();
                Set<String> sharedPlayers = sharedScoreboard.getPlayers();


                Objective objective = board.getObjective(objectiveName);
                if (objective == null) {
                    objective = board.registerNewObjective(objectiveName, Criteria.DUMMY, Component.text(objectiveName));
                }

                ScoreboardStorage scoreboard = storedScoreboards.get(objectiveName);

                for (String player : sharedPlayers) { // update all the new scores from the shared storage on startup
                    Score score = objective.getScore(player);
                    Integer s = sharedScoreboard.getPlayerScore(player);

                    if (s != null) {
                        score.setScore(sharedScoreboard.getPlayerScore(player));
                        scoreboard.setScore(player, sharedScoreboard.getPlayerScore(player));
                    }
                }

                if (sharedScoreboards.containsKey(objectiveName)) {
                    Set<String> differenceSet = new HashSet<String>(currentPlayers);
                    differenceSet.removeAll(sharedPlayers);
                    for (String player : differenceSet) { // remove scores that don't exist in storage
                        Score score = objective.getScore(player);
                        score.resetScore();
                    }
                }
            }
        } else {
            getLogger().warning("ScoreboardManager is null!");
        }
    }
}