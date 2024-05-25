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
import java.io.FileWriter;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.Scanner;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class GlobalData extends JavaPlugin {

    private File customConfigFile;
    private FileConfiguration customConfig;

    private String filepath;
    private String objectiveName;
    // private String playername = "underminerman";

    // private int storedScore = 0;
    ScoreboardStorage scoreboard = new ScoreboardStorage(objectiveName);

    @Override
    public void onEnable() {
        createCustomConfig();

        filepath = this.getCustomConfig().getString("directory_path");
        objectiveName = this.getCustomConfig().getString("objectives");

        // int storedScore = getScoreFromFile();
        // scoreboard.setScore(playername, storedScore);
        // setPlayerScore(storedScore);

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
        /* User Edit:
            Instead of the above Try/Catch, you can also use
            YamlConfiguration.loadConfiguration(customConfigFile)
        */
    }

    

    private void tick() {

        Set<String> currentPlayers = getPlayers();
        Set<String> storedPlayers = scoreboard.getPlayers();

        Set<String> players = Stream.concat(currentPlayers.stream(), storedPlayers.stream())
            .collect(Collectors.toSet());

        for (String player : players) {
            Integer currentScore = getPlayerScore(player);
            Integer storedScore = scoreboard.getPlayerScore(player);

            if (storedScore != currentScore) {
                if (currentScore == null) {
                    scoreboard.resetScore(player);
                }
                else {
                    storedScore = currentScore;

                    scoreboard.setScore(player, storedScore);
                }

                getLogger().info("value changed");
            }
        }

        writeScoreToFile();
        
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
                return score.getScore();
            } else {
                getLogger().warning("Objective 'global' not found!");
            }
        } else {
            getLogger().warning("ScoreboardManager is null!");
        }
        return null;
    }

    private void setPlayerScore(int scoreValue) {
        String playerName = "underminerman";
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

    @SuppressWarnings("unchecked")
    private void writeScoreToFile() {
        JSONObject scoresDetails = new JSONObject();
        
        JSONArray objectiveNames = new JSONArray();
        objectiveNames.add(objectiveName);
        scoresDetails.put("objective_names",objectiveNames);

        JSONObject board = new JSONObject();
        for (String player : getPlayers()) {
            board.put(player,getPlayerScore(player));
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

    private int getScoreFromFile() {
        try {
            File myObj = new File(filepath);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            myReader.close();
            return(Integer.parseInt(data));
        }
        myReader.close();
        } catch (FileNotFoundException e) {
            getLogger().warning("An fileRead occurred.");
            e.printStackTrace();
        }

        return(0);
    }
}