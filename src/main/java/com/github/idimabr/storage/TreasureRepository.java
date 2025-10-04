package com.github.idimabr.storage;

import com.github.idimabr.TreasureHunt;
import com.github.idimabr.models.PlayerTreasure;
import com.github.idimabr.models.Treasure;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class TreasureRepository {

    private final TreasureHunt plugin;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    public CompletableFuture<Boolean> testConnection() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return conn.isValid(2);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }, plugin.getExecutor());
    }

    public CompletableFuture<Void> createTables() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS treasures (" +
                                "id VARCHAR(50) PRIMARY KEY NOT NULL," +
                                "command TEXT NOT NULL," +
                                "world VARCHAR(50) NOT NULL," +
                                "x INT NOT NULL," +
                                "y INT NOT NULL," +
                                "z INT NOT NULL" +
                                ");"
                );

                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS player_treasures (" +
                                "uuid CHAR(36) NOT NULL," +
                                "treasure_id VARCHAR(50) NOT NULL," +
                                "found_at BIGINT NOT NULL," +
                                "PRIMARY KEY(uuid, treasure_id)," +
                                "FOREIGN KEY (treasure_id) REFERENCES treasures(id) ON DELETE CASCADE" +
                                ");"
                );

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, plugin.getExecutor());
    }

    // ================= TREASURES =================
    public CompletableFuture<Void> saveTreasureAsync(Treasure treasure) {
        return CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO treasures (id, command, world, x, y, z) VALUES (?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE command = VALUES(command), world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z)";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setString(1, treasure.getId());
                ps.setString(2, treasure.getCommand());
                ps.setString(3, treasure.getLocation().getWorld().getName());
                ps.setInt(4, treasure.getLocation().getBlockX());
                ps.setInt(5, treasure.getLocation().getBlockY());
                ps.setInt(6, treasure.getLocation().getBlockZ());

                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, plugin.getExecutor());
    }

    public CompletableFuture<Set<Treasure>> loadTreasuresAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Set<Treasure> treasures = new HashSet<>();
            String query = "SELECT * FROM treasures";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String id = rs.getString("id");
                    String command = rs.getString("command");
                    String worldName = rs.getString("world");
                    World world = Bukkit.getWorld(worldName);
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    Location loc = new Location(world, x, y, z);

                    treasures.add(new Treasure(id, command, loc));
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            return treasures;
        }, plugin.getExecutor());
    }

    public CompletableFuture<Void> deleteAsync(String id) {
        return CompletableFuture.runAsync(() -> {
            String query = "DELETE FROM treasures WHERE id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, plugin.getExecutor());
    }

    // ================= PLAYER TREASURES =================
    public CompletableFuture<Boolean> hasPlayerFoundTreasureAsync(UUID playerUuid, String treasureId) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT 1 FROM player_treasures WHERE uuid = ? AND treasure_id = ? LIMIT 1";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, treasureId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, plugin.getExecutor());
    }

    public CompletableFuture<Void> insertPlayerTreasureAsync(PlayerTreasure playerTreasure) {
        return CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO player_treasures (uuid, treasure_id, found_at) VALUES (?,?,?) " +
                    "ON DUPLICATE KEY UPDATE found_at = VALUES(found_at)";
            long foundAtMillis = playerTreasure.getFoundAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, playerTreasure.getPlayerUuid().toString());
                ps.setString(2, playerTreasure.getTreasureId());
                ps.setLong(3, foundAtMillis);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, plugin.getExecutor());
    }

    public CompletableFuture<Set<PlayerTreasure>> loadPlayerTreasuresAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Set<PlayerTreasure> treasures = new HashSet<>();
            String query = "SELECT * FROM player_treasures WHERE uuid = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String treasureId = rs.getString("treasure_id");
                        Instant foundAt = Instant.ofEpochMilli(rs.getLong("found_at"));
                        treasures.add(new PlayerTreasure(uuid, treasureId, foundAt));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return treasures;
        }, plugin.getExecutor());
    }

    public CompletableFuture<List<UUID>> loadTreasureFoundAsync(String treasureId) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> players = new ArrayList<>();
            String query = "SELECT uuid FROM player_treasures WHERE treasure_id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, treasureId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        players.add(UUID.fromString(rs.getString("uuid")));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return players;
        }, plugin.getExecutor());
    }
}
