package net.minermen;

import java.util.HashMap;
import java.util.Set;

public class ScoreboardStorage {
    private String objective;
    private HashMap<String,Integer> scores = new HashMap<String,Integer>();

    ScoreboardStorage(String objective) {
        this.objective = objective;
    }

    ScoreboardStorage(ScoreboardStorage other) { // copy constructor
        this.objective = other.objective;

        for (String player : other.getPlayers()) {
            this.setScore(player, other.getPlayerScore(player));
        }
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

    public void setScore(String playername, Integer score) {

        scores.put(playername, Integer.valueOf(score));
    }

    public void resetScore(String playername) {
        scores.remove(playername);
    }
}
