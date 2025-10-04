package com.github.idimabr.controllers;

import com.github.idimabr.TreasureHunt;
import com.github.idimabr.models.PendingTreasure;
import com.github.idimabr.models.PlayerTreasure;
import com.github.idimabr.models.Treasure;
import com.github.idimabr.storage.TreasureRepository;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TreasureController {

    private final TreasureRepository repository;
    private final TreasureHunt plugin;

    @Getter
    private final Map<String, Treasure> treasures = new HashMap<>();
    private final Map<UUID, Set<String>> playerTreasures = new HashMap<>();
    private final Map<UUID, PendingTreasure> pendingTreasures = new HashMap<>();

    public TreasureController(TreasureHunt plugin, TreasureRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }


    public void addPendingTreasure(UUID playerUuid, PendingTreasure pendingTreasure) {
        pendingTreasures.put(playerUuid, pendingTreasure);
    }

    public PendingTreasure removePendingTreasure(UUID playerUuid) {
        return pendingTreasures.remove(playerUuid);
    }

    public PendingTreasure getPendingTreasure(UUID playerUuid) {
        return pendingTreasures.get(playerUuid);
    }

    public void loadTreasuresFromDBAsync() {
        repository.loadTreasuresAsync().thenAccept(list -> {
            treasures.clear();
            for (Treasure t : list) {
                treasures.put(t.getId(), t);
            }
        });
    }

    public Optional<Treasure> getTreasureByLocation(Location location) {
        return treasures.values().stream()
                .filter(t -> t.getLocation().equals(location))
                .findFirst();
    }

    public void addTreasure(Treasure treasure) {
        treasures.put(treasure.getId(), treasure);
        repository.saveTreasureAsync(treasure);
    }

    public void removeTreasure(String treasureId) {
        treasures.remove(treasureId);
        repository.deleteAsync(treasureId);
        for (Set<String> set : playerTreasures.values()) {
            set.remove(treasureId);
        }
    }

    public void loadPlayerTreasuresAsync(Player player) {
        repository.loadPlayerTreasuresAsync(player.getUniqueId())
                .thenAccept(set -> {
                    Set<String> ids = new HashSet<>();
                    for (PlayerTreasure pt : set) {
                        ids.add(pt.getTreasureId());
                    }
                    playerTreasures.put(player.getUniqueId(), ids);
                });
    }

    public CompletableFuture<Boolean> hasFoundTreasureAsync(Player player, String treasureId) {
        Set<String> found = playerTreasures.get(player.getUniqueId());
        if (found != null && found.contains(treasureId)) {
            return CompletableFuture.completedFuture(true);
        }

        return repository.hasPlayerFoundTreasureAsync(player.getUniqueId(), treasureId)
                .thenApply(foundInDB -> {
                    if (foundInDB) {
                        playerTreasures.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>())
                                .add(treasureId);
                    }
                    return foundInDB;
                });
    }

    public void claimTreasureAsync(Player player, String treasureId) {
        if (!treasures.containsKey(treasureId)) return;

        hasFoundTreasureAsync(player, treasureId).thenAccept(found -> {
            if (found) return;

            playerTreasures.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(treasureId);
            PlayerTreasure pt = new PlayerTreasure(player.getUniqueId(), treasureId, Instant.now());
            repository.insertPlayerTreasureAsync(pt);

            Treasure treasure = treasures.get(treasureId);
            String command = treasure.getCommand().replace("%player%", player.getName());
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            );
        });
    }

    public CompletableFuture<List<UUID>> getPlayersFoundTreasureAsync(String treasureId) {
        return repository.loadTreasureFoundAsync(treasureId);
    }
}