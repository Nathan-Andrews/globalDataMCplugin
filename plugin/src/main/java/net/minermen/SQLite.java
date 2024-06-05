package net.minermen;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.io.File;

public class SQLite {

    // Path to the shared database directory
    private String SHARED_DATABASE_PATH;
    private String SHARED_DATABASE_FILE = "database.db";

    private String getDatabasePath() {
        // Ensure the directory exists
        File directory = new File(SHARED_DATABASE_PATH);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return "jdbc:sqlite:" + new File(SHARED_DATABASE_PATH,SHARED_DATABASE_FILE).getAbsolutePath();
    }

    public Connection connect() {
        Connection conn = null;
        try {
            // Create a connection to the database
            conn = DriverManager.getConnection(getDatabasePath());
            // System.out.println("Connection to SQLite has been established.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public void initialize(String path,String filename) {
        SHARED_DATABASE_PATH = path;
        SHARED_DATABASE_FILE = filename;

        // SQL statement for creating a new table
        String sql = "CREATE TABLE IF NOT EXISTS objectives (\n"
                + " id integer PRIMARY KEY,\n"
                + " objective text NOT NULL,\n"
                + " player text NOT NULL,\n"
                + " score integer\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            // create a new table
            stmt.execute(sql);
            System.out.println("Table has been created or already exists.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void printDatabase() {
        System.out.println("+--------------------+");
        Set<String> objectiveNames = getObjectiveNames();

        for (String objectiveName : objectiveNames) {
            Set<String> players = getPlayers(objectiveName);

            for (String player : players) {
                System.out.println("| " + objectiveName + " | " + player + " | " + getPlayerScore(objectiveName, player) + " |");
            }
        }
        System.out.println("+--------------------+");
    }

    public Set<String> getObjectiveNames() {
        String sql = "SELECT objective FROM objectives GROUP BY objective"; 

        Set<String> objectiveNames = new HashSet<String>();

        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    objectiveNames.add(rs.getString("objective"));
                }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return objectiveNames;
    }

    public Set<String> getPlayers(String objective) {
        String sql = "SELECT player FROM objectives WHERE objective = ?";

        Set<String> playerNames = new HashSet<String>();

        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, objective);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    playerNames.add(rs.getString("player"));
                }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return playerNames;
    }

    public Integer getPlayerScore(String objective, String player) {
        String sql = "SELECT score FROM objectives WHERE objective = ? AND player = ?";
        
        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, objective);
                pstmt.setString(2, player);
                ResultSet rs = pstmt.executeQuery();

                int score = rs.getInt("score");

                if (rs.wasNull()) return null;

                return score;

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

    public void setPlayerScore(String objective, String player, int score) {
        String sql;

        Integer previousScore = getPlayerScore(objective, player);
        if (previousScore == null) {
            sql = "INSERT INTO objectives(score,objective,player) VALUES (?, ?, ?)";
        }
        else {
            sql = "UPDATE objectives SET score = ? WHERE objective = ? AND player = ?";

            // if (previousScore.equals(Integer.valueOf(score))) return;
        }

        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1,score);
                pstmt.setString(2,objective);
                pstmt.setString(3,player);

                pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void resetPlayerScore(String objective, String player) {
        String sql = "DELETE FROM objectives WHERE objective = ? AND player = ?";

        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1,objective);
                pstmt.setString(2,player);
                
                pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

    }

    public void removeObjective(String objective) {
        String sql = "DELETE FROM objectives WHERE objective = ?";

        try (Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1,objective);
                
                pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}