package com.github.idimabr.storage.dao;

import com.github.idimabr.TreasureHunt;
import com.github.idimabr.models.PlayerTreasure;
import com.github.idimabr.models.Treasure;
import com.github.idimabr.storage.Database;
import com.github.idimabr.storage.adapter.PlayerTreasureAdapter;
import com.github.idimabr.storage.adapter.TreasureAdapter;
import com.henryfabio.sqlprovider.connector.SQLConnector;
import com.henryfabio.sqlprovider.executor.SQLExecutor;
import lombok.RequiredArgsConstructor;

import java.time.ZoneId;
import java.util.*;

@RequiredArgsConstructor
public class TreasureRepository {

    private final TreasureHunt plugin;
    private final SQLConnector connector;

    private SQLExecutor executor() {
        return new SQLExecutor(connector);
    }

    public void createTables() {
        executor().updateQuery(
                "CREATE TABLE IF NOT EXISTS treasures (" +
                        "id VARCHAR(50) PRIMARY KEY NOT NULL," +
                        "command TEXT NOT NULL," +
                        "world VARCHAR(50) NOT NULL," +
                        "x INT NOT NULL," +
                        "y INT NOT NULL," +
                        "z INT NOT NULL" +
                        ");"
        );

        String playerTreasuresTable = Database.isSQLITE() ?
                "CREATE TABLE IF NOT EXISTS player_treasures (" +
                        "uuid CHAR(36) NOT NULL," +
                        "treasure_id VARCHAR(50) NOT NULL," +
                        "found_at BIGINT NOT NULL," +
                        "PRIMARY KEY(uuid, treasure_id)," +
                        "FOREIGN KEY (treasure_id) REFERENCES treasures(id) ON DELETE CASCADE" +
                        ");"
                :
                "CREATE TABLE IF NOT EXISTS player_treasures (" +
                        "uuid CHAR(36) NOT NULL," +
                        "treasure_id VARCHAR(50) NOT NULL," +
                        "found_at BIGINT NOT NULL," +
                        "PRIMARY KEY(uuid, treasure_id)," +
                        "CONSTRAINT fk_treasure FOREIGN KEY (treasure_id) REFERENCES treasures(id) ON DELETE CASCADE" +
                        ");";

        executor().updateQuery(playerTreasuresTable);
    }

    public boolean hasPlayerFoundTreasure(UUID playerUuid, String treasureId) {
        return executor().resultOneQuery(
                "SELECT * FROM player_treasures WHERE uuid = ? AND treasure_id = ?;",
                stmt -> {
                    stmt.set(1, playerUuid.toString());
                    stmt.set(2, treasureId);
                },
                PlayerTreasureAdapter.class
        ) != null;
    }

    public void saveTreasure(Treasure treasure) {
        String query = Database.isSQLITE() ?
                "REPLACE INTO treasures (id, command, world, x, y, z) VALUES (?,?,?,?,?,?);" :
                "INSERT INTO treasures (id, command, world, x, y, z) VALUES (?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE command = VALUES(command), world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z);";

        executor().updateQuery(query, stmt -> {
            stmt.set(1, treasure.getId());
            stmt.set(2, treasure.getCommand());
            stmt.set(3, treasure.getLocation().getWorld().getName());
            stmt.set(4, treasure.getLocation().getBlockX());
            stmt.set(5, treasure.getLocation().getBlockY());
            stmt.set(6, treasure.getLocation().getBlockZ());
        });
    }

    public Set<Treasure> loadTreasures() {
        return executor().resultManyQuery(
                "SELECT * FROM treasures;",
                stmt -> {},
                TreasureAdapter.class
        );
    }

    public void delete(String id) {
        executor().updateQuery(
                "DELETE FROM treasures WHERE id = ?;",
                stmt -> stmt.set(1, id)
        );
    }

    public void insertPlayerTreasure(PlayerTreasure playerTreasure) {
        String query = Database.isSQLITE() ?
                "REPLACE INTO player_treasures (uuid, treasure_id, found_at) VALUES (?,?,?);" :
                "INSERT INTO player_treasures (uuid, treasure_id, found_at) VALUES (?,?,?) ON DUPLICATE KEY UPDATE found_at = VALUES(found_at);";

        long foundAtMillis = playerTreasure.getFoundAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        executor().updateQuery(query, stmt -> {
            stmt.set(1, playerTreasure.getPlayerUuid().toString());
            stmt.set(2, playerTreasure.getTreasureId());
            stmt.set(3, foundAtMillis);
        });
    }

    public Set<PlayerTreasure> loadPlayerTreasures(UUID playerUuid) {
        return executor().resultManyQuery(
                "SELECT * FROM player_treasures WHERE uuid = ?;",
                stmt -> stmt.set(1, playerUuid.toString()),
                PlayerTreasureAdapter.class
        );
    }

    public List<UUID> loadTreasureFound(String treasureId) {
        Set<PlayerTreasure> list = executor().resultManyQuery(
                "SELECT * FROM player_treasures WHERE treasure_id = ?;",
                stmt -> stmt.set(1, treasureId),
                PlayerTreasureAdapter.class
        );

        if (list == null) {
            list = new HashSet<>();
        }

        List<UUID> players = new ArrayList<>();
        for (PlayerTreasure pt : list) {
            players.add(pt.getPlayerUuid());
        }
        return players;
    }
}