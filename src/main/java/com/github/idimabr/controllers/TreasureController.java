package com.github.idimabr.controllers;

import com.github.idimabr.models.PendingTreasure;
import com.github.idimabr.models.PlayerTreasure;
import com.github.idimabr.models.Treasure;
import com.github.idimabr.storage.dao.TreasureRepository;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

public class TreasureController {

    private final TreasureRepository repository;

    @Getter
    private final Map<String, Treasure> treasures = new HashMap<>();
    private final Map<UUID, Set<String>> playerTreasures = new HashMap<>();
    private final Map<UUID, PendingTreasure> pendingTreasures = new HashMap<>();

    public TreasureController(TreasureRepository repository) {
        this.repository = repository;
        loadTreasuresFromDB();
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

    public void loadTreasuresFromDB() {
        treasures.clear();
        List<Treasure> list = new ArrayList<>(repository.loadTreasures());
        for (Treasure t : list) {
            treasures.put(t.getId(), t);
        }
    }

    public void loadPlayerTreasures(Player player) {
        Set<PlayerTreasure> set = repository.loadPlayerTreasures(player.getUniqueId());
        Set<String> ids = new HashSet<>();
        for (PlayerTreasure pt : set) {
            ids.add(pt.getTreasureId());
        }
        playerTreasures.put(player.getUniqueId(), ids);
    }

    public boolean hasFoundTreasure(Player player, String treasureId) {
        return repository.hasPlayerFoundTreasure(player.getUniqueId(), treasureId);
    }

    public void claimTreasure(Player player, String treasureId) {
        if (!treasures.containsKey(treasureId)) return;
        if (hasFoundTreasure(player, treasureId)) return;

        playerTreasures.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(treasureId);

        PlayerTreasure pt = new PlayerTreasure(player.getUniqueId(), treasureId, Instant.now());
        repository.insertPlayerTreasure(pt);

        Treasure treasure = treasures.get(treasureId);
        String command = treasure.getCommand().replace("%player%", player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }


    public void addTreasure(Treasure treasure) {
        treasures.put(treasure.getId(), treasure);
        repository.saveTreasure(treasure);
    }


    public void removeTreasure(String treasureId) {
        treasures.remove(treasureId);
        repository.delete(treasureId);
        for (Set<String> set : playerTreasures.values()) {
            set.remove(treasureId);
        }
    }

    public List<UUID> getPlayersFoundTreasure(String treasureId) {
        return repository.loadTreasureFound(treasureId);
    }

    public Optional<Treasure> getTreasureByLocation(Location location) {
        return treasures.values().stream()
                .filter(t -> t.getLocation().equals(location))
                .findFirst();
    }
}
