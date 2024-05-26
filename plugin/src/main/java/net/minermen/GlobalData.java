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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Set;
import java.util.HashSet;
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
    private String objectiveName;
    // private String playername = "underminerman";

    // private int storedScore = 0;
    ScoreboardStorage scoreboard = new ScoreboardStorage(objectiveName);

    JSONObject sharedStorageJson;

    @Override
    public void onEnable() {
        createCustomConfig();

        filepath = this.getCustomConfig().getString("directory_path");
        objectiveName = this.getCustomConfig().getString("objectives");

        // int storedScore = getScoreFromFile();
        // scoreboard.setScore(playername, storedScore);
        // setPlayerScore(storedScore);

        getSharedStorage();
        saveSharedStorage();

        startTick();

        getLogger().info("GlobalData plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GlobalData plugin has been disabled.");
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
        boolean valueChanged = false;

        getSharedStorage();

        Set<String> currentPlayers = getPlayers();
        Set<String> storedPlayers = scoreboard.getPlayers();
        Set<String> sharedPlayers = getSharedPlayers();


        Set<String> players = Stream.concat(Stream.concat(currentPlayers.stream(), storedPlayers.stream()),sharedPlayers.stream())
            .collect(Collectors.toSet());

        for (String player : players) {
            Integer currentScore = getPlayerScore(player);
            Integer storedScore = scoreboard.getPlayerScore(player);
            Integer sharedScore = getSharedScore(player);

            if (Utils.areNotEqual(storedScore, currentScore)) {
                // getLogger().info(storedScore + ": " + currentScore);
                if (currentScore == null) {
                    scoreboard.resetScore(player);
                }
                else {
                    storedScore = currentScore;

                    scoreboard.setScore(player, storedScore);
                }

                getLogger().info("value changed");

                valueChanged = true;
            }
            else if (Utils.areNotEqual(storedScore, sharedScore)) {
                if (sharedScore == null) {
                    resetScore(player);
                    scoreboard.resetScore(player);
                }
                else {
                    // getLogger().info(storedScore + ", " + sharedScore);
                    storedScore = sharedScore;

                    scoreboard.setScore(player, storedScore);

                    setPlayerScore(player,storedScore);
                }

                getLogger().info("value changed server");
            }
        }

        if (valueChanged) {writeSharedStorage();}
        
        // if (currentScore != storedScore) {
        //     storedScore = currentScore;
        //     getLogger().info("value changed");
        //     writeScoreToFile(Integer.toString(currentScore));
        // }
        // else if (sharedScore != storedScore) {
        //     storedScore = sharedScore;

        //     getLogger().info("value changed from storage");
        //     setPlayerScore(storedScore);
        // }

        // scoreboard.setScore(playername, storedScore);
    }

    private Set<String> getPlayers() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getMainScoreboard();

        return board.getEntries();
    }

    private Integer getPlayerScore(String playerName) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            Objective objective = board.getObjective(objectiveName);
            if (objective != null) {
                Score score = objective.getScore(playerName);
                if (! score.isScoreSet()) return null;
                return score.getScore();
            } else {
                getLogger().warning("Objective 'global' not found!");
            }
        } else {
            getLogger().warning("ScoreboardManager is null!");
        }
        return null;
    }

    private void setPlayerScore(String playerName, int scoreValue) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            Objective objective = board.getObjective(objectiveName);
            if (objective == null) {
                objective = board.registerNewObjective(objectiveName, Criteria.DUMMY, Component.text(objectiveName));
            }
            Score score = objective.getScore(playerName);
            score.setScore(scoreValue);
        } else {
            getLogger().warning("ScoreboardManager is null!");
        }
    }

    private void resetScore(String playerName) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            Objective objective = board.getObjective(objectiveName);
            if (objective == null) {
                objective = board.registerNewObjective(objectiveName, Criteria.DUMMY, Component.text(objectiveName));
            }
            Score score = objective.getScore(playerName);
            score.resetScore();;
        } else {
            getLogger().warning("ScoreboardManager is null!");
        }
    }

    @SuppressWarnings("unchecked")
    private void writeSharedStorage() {
        JSONObject scoresDetails = new JSONObject();
        
        JSONArray objectiveNames = new JSONArray();
        objectiveNames.add(objectiveName);
        scoresDetails.put("objective_names",objectiveNames);

        JSONObject board = new JSONObject();
        for (String player : scoreboard.getPlayers()) {
            board.put(player,scoreboard.getPlayerScore(player));
        }

        JSONObject objectives = new JSONObject();

        objectives.put(objectiveName,board);

        scoresDetails.put("objectives",objectives);

        try (FileWriter file = new FileWriter(filepath)) {
            //We can write any JSONArray or JSONObject instance to the file
            file.write(scoresDetails.toJSONString()); 
            file.flush();
 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Integer getSharedScore(String playername) {
        JSONObject objectives = (JSONObject) sharedStorageJson.get("objectives");
        if (objectives == null) {
            return null;
        }
        else {
            JSONObject board = (JSONObject) objectives.get(objectiveName);

            if (board == null) {
                return null;
            }

            else {
                Long s = (Long) board.get(playername);
                return s != null ? s.intValue() : null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getSharedPlayers() {
        JSONObject board = ((JSONObject) ((JSONObject) sharedStorageJson.get("objectives")).get(objectiveName));

        return board.keySet();
    }

    private void getSharedStorage() {

        JSONParser jsonParser = new JSONParser();
         
        try (FileReader reader = new FileReader(filepath))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
 
            sharedStorageJson = (JSONObject) obj;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void saveSharedStorage() {
        Set<String> currentPlayers = getPlayers();
        Set<String> sharedPlayers = getSharedPlayers();

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            Objective objective = board.getObjective(objectiveName);
            if (objective == null) {
                objective = board.registerNewObjective(objectiveName, Criteria.DUMMY, Component.text(objectiveName));
            }
            // Score score = objective.getScore(playerName);
            // score.setScore(scoreValue);

            for (String player : sharedPlayers) { // update all the new scores from the shared storage on startup
                Score score = objective.getScore(player);
                score.setScore(getSharedScore(player));

                scoreboard.setScore(player, getSharedScore(player));
            }

            Set<String> differenceSet = new HashSet<String>(currentPlayers);
            differenceSet.removeAll(sharedPlayers);
            for (String player : differenceSet) { // remove scores that don't exist in storage
                Score score = objective.getScore(player);
                score.resetScore();
            }
        } else {
            getLogger().warning("ScoreboardManager is null!");
        }
    }
}