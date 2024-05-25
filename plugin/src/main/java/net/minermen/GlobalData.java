package net.minermen;


import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
import java.util.Scanner;


public class GlobalData extends JavaPlugin {

    private File customConfigFile;
    private FileConfiguration customConfig;

    private String filepath;
    private String objectiveName;
    private String playername = "underminerman";

    // private int storedScore = 0;
    ScoreboardStorage scoreboard = new ScoreboardStorage(objectiveName);

    @Override
    public void onEnable() {
        createCustomConfig();

        filepath = this.getCustomConfig().getString("directory_path");
        objectiveName = this.getCustomConfig().getString("objectives");

        int storedScore = getScoreFromFile();
        scoreboard.setScore(playername, storedScore);
        setPlayerScore(storedScore);

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
        // Your code to run every tick
        int currentScore = getPlayerScore(playername);
        int sharedScore = getScoreFromFile();

        int storedScore = scoreboard.getPlayerScore(playername);
        
        if (currentScore != storedScore) {
            storedScore = currentScore;
            getLogger().info("value changed");
            writeScoreToFile(Integer.toString(currentScore));
        }
        else if (sharedScore != storedScore) {
            storedScore = sharedScore;

            getLogger().info("value changed from storage");
            setPlayerScore(storedScore);
        }

        scoreboard.setScore(playername, storedScore);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("send")) {
            getLogger().info("command used");

            if (sender instanceof Player) {
                // Player player = (Player) sender;
                // player.sendMessage("Hello, " + player.getName() + "!");

                // getLogger().info(player.getName());
                getLogger().info((Integer.toString(getPlayerScore("underminerman"))));
            }
        }
        return false;
    }

    private int getPlayerScore(String playerName) {
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
        return 0;
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

    private void writeScoreToFile(String data) {
        try {
            FileWriter myWriter = new FileWriter(filepath);
            myWriter.write(data);
            myWriter.close();
            // System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            getLogger().warning("An fileWrite error occurred.");
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