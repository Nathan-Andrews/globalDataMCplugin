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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
    private Set<String> objectives;
    // private String playername = "underminerman";

    // private int storedScore = 0;
    private Hashtable<String,ScoreboardStorage> scoreboards;

    JSONObject sharedStorageJson;

    @Override
    public void onEnable() {
        createCustomConfig();

        filepath = this.getCustomConfig().getString("directory_path");
        String filename = this.getCustomConfig().getString("storage_filename");
        objectivePattern = this.getCustomConfig().getString("objectives");

        if (filepath == null || filename.equals("")) {
            getLogger().severe("Set a filepath for storage in the config file: ./plugins/GlobalData/config.yml");

            disablePlugin();
            return;
        }

        filepath = Paths.get(filepath, "GlobalData", filename).toString();

        System.out.println(filepath);

        objectives = new HashSet<String>();

        scoreboards = new Hashtable<String,ScoreboardStorage>();

        getSharedStorage();

        getTrackedObjectives();

        for (String objective : objectives) {
            ScoreboardStorage scoreboard = new ScoreboardStorage(objective);

            scoreboards.put(objective,scoreboard);
        }

        // int storedScore = getScoreFromFile();
        // scoreboard.setScore(playername, storedScore);
        // setPlayerScore(storedScore);

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
        boolean valueChanged = false;

        getTrackedObjectives();

        getSharedStorage();

        for (String objectiveName : objectives) {
            ScoreboardStorage scoreboard = scoreboards.get(objectiveName);
            if (scoreboard == null) {
                scoreboard = new ScoreboardStorage(objectiveName);
                scoreboards.put(objectiveName,scoreboard);
            }

            Set<String> currentPlayers = getPlayers(objectiveName);
            Set<String> storedPlayers = scoreboard.getPlayers();
            Set<String> sharedPlayers = getSharedPlayers(objectiveName);
    
    
            Set<String> players = Stream.concat(Stream.concat(currentPlayers.stream(), storedPlayers.stream()),sharedPlayers.stream())
                .collect(Collectors.toSet());
    
            for (String player : players) {
                Integer currentScore = getPlayerScore(objectiveName,player);
                Integer storedScore = scoreboard.getPlayerScore(player);
                Integer sharedScore = getSharedScore(objectiveName,player);
    
                if (Utils.areNotEqual(storedScore, currentScore)) {
                    // getLogger().info(storedScore + ": " + currentScore);
                    if (currentScore == null) {
                        scoreboard.resetScore(player);
                    }
                    else {
                        storedScore = currentScore;
    
                        scoreboard.setScore(player, storedScore);
                    }
    
                    // getLogger().info("value changed");
    
                    valueChanged = true;
                }
                else if (Utils.areNotEqual(storedScore, sharedScore)) {
                    if (sharedScore == null) {
                        resetScore(objectiveName,player);
                        scoreboard.resetScore(player);
                    }
                    else {
                        // getLogger().info(storedScore + ", " + sharedScore);
                        storedScore = sharedScore;
    
                        scoreboard.setScore(player, storedScore);
    
                        setPlayerScore(objectiveName,player,storedScore);
                    }
    
                    // getLogger().info("value changed server");
                }
            }
        }

        if (valueChanged) {writeSharedStorage();}
    }

    private Set<String> getPlayers(String objectiveName) {
        Set<String> players = new HashSet<>();

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getMainScoreboard();
        Objective objective = board.getObjective(objectiveName);

        if (objective != null) {
            // Get all entries (players/teams) that have scores in this objective
            for (String entry : board.getEntries()) {
                Score score = objective.getScore(entry);
                if (score.isScoreSet()) {
                    players.add(entry);
                }
            }
        }

        return players;
    }

    private Integer getPlayerScore(String objectiveName, String playerName) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            Objective objective = board.getObjective(objectiveName);
            if (objective != null) {
                Score score = objective.getScore(playerName);
                if (! score.isScoreSet()) return null;
                return score.getScore();
            } else {
                getLogger().warning(String.format("Objective %1$s not found!",objectiveName));
            }
        } else {
            getLogger().warning("ScoreboardManager is null!");
        }
        return null;
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

    private void getTrackedObjectives() {
        objectives.removeAll(objectives);

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            // objectives.removeAll(objectives);
            for (Objective objective : board.getObjectives()) {
                objectives.add(objective.getName());
            }
        } else {
            getLogger().warning("ScoreboardManager is null!");
        }

        for (String objectiveName : getSharedObjectiveNames()) {
            objectives.add(objectiveName);
        }

        Pattern pattern = Pattern.compile(objectivePattern, Pattern.CASE_INSENSITIVE);

        objectives = objectives.stream()
            .filter(pattern.asPredicate())
            .collect(Collectors.toSet());
    }

    private void resetScore(String objectiveName,String playerName) {
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

    @SuppressWarnings("unchecked")
    private void writeSharedStorage() {
        JSONObject scoresDetails = new JSONObject();
        JSONObject objectives = new JSONObject();
        JSONArray objectiveNames = new JSONArray();

        for (String objectiveName : this.objectives) {
            ScoreboardStorage scoreboard = scoreboards.get(objectiveName);
            if (scoreboard == null) {
                scoreboard = new ScoreboardStorage(objectiveName);
            }
            objectiveNames.add(objectiveName);

            JSONObject board = new JSONObject();
            for (String player : scoreboard.getPlayers()) {
                board.put(player,scoreboard.getPlayerScore(player));
            }

            objectives.put(objectiveName,board);
        }

        scoresDetails.put("objective_names",objectiveNames);

        scoresDetails.put("objectives",objectives);

        try {
            Path path = Paths.get(filepath);

            Files.createDirectories(path.getParent());

            try (FileWriter file = new FileWriter(filepath)) {
                //We can write any JSONArray or JSONObject instance to the file
                file.write(scoresDetails.toJSONString()); 
                file.flush();
     
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Integer getSharedScore(String objectiveName, String playername) {
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
    private Set<String> getSharedPlayers(String objectiveName) {
        if ((JSONObject) sharedStorageJson.get("objectives") != null) {
            JSONObject board = ((JSONObject) ((JSONObject) sharedStorageJson.get("objectives")).get(objectiveName));

            if (board != null) {
                return board.keySet();
            }
        }

        return new HashSet<String>();
    }

    private void getSharedStorage() {

        JSONParser jsonParser = new JSONParser();

        sharedStorageJson = new JSONObject();

        try (FileReader reader = new FileReader(filepath))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
 
            sharedStorageJson = (JSONObject) obj;
        }
        catch (FileNotFoundException e) {
            // e.printStackTrace();
        } catch (IOException e) {
            // e.printStackTrace();
        } catch (ParseException e) {
            // e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> getSharedObjectiveNames() {
        Set<String> objectiveNames = new HashSet<String>();

        if ((JSONArray) sharedStorageJson.get("objective_names") == null) return objectiveNames;

        for (String s : ((Stream<String>) ((JSONArray) sharedStorageJson.get("objective_names")).stream()).collect(Collectors.toSet())) {
            objectiveNames.add(s);
        }

        return objectiveNames;
    }

    private void saveSharedStorage() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard board = manager.getMainScoreboard();
            for (String objectiveName : objectives) {
                Set<String> currentPlayers = getPlayers(objectiveName);
                Set<String> sharedPlayers = getSharedPlayers(objectiveName);


                Objective objective = board.getObjective(objectiveName);
                if (objective == null) {
                    objective = board.registerNewObjective(objectiveName, Criteria.DUMMY, Component.text(objectiveName));
                    // objective = board.registerNewObjective(objectiveName, "dummy", Component.text(objectiveName)); // 1.18.1 method
                }
                // Score score = objective.getScore(playerName);
                // score.setScore(scoreValue);

                ScoreboardStorage scoreboard = scoreboards.get(objectiveName);

                for (String player : sharedPlayers) { // update all the new scores from the shared storage on startup
                    Score score = objective.getScore(player);
                    Integer s = getSharedScore(objectiveName,player);

                    if (s != null) {
                        score.setScore(getSharedScore(objectiveName,player));
                        scoreboard.setScore(player, getSharedScore(objectiveName,player));
                    }
                }

                if (getSharedObjectiveNames().contains(objectiveName)) {
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