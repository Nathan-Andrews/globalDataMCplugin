package net.minermen;

import java.util.HashMap;
import java.util.Set;

public class ScoreboardStorage {
    private String objective;
    private HashMap<String,Integer> scores = new HashMap<String,Integer>();

    ScoreboardStorage(String objective) {
        this.objective = objective;
    }

    public String getObjective() {
        return objective;
    }

    public Integer getPlayerScore(String playername) {
        return scores.get(playername);
    }

    public Boolean playerExists(String playername) {
        return scores.containsKey(playername);
    }

    public Set<String> getPlayers() {
        return scores.keySet();
    }

    public void setScore(String playername, int score) {
        scores.put(playername, Integer.valueOf(score));
    }

    public void resetScore(String playername) {
        scores.remove(playername);
    }
}
