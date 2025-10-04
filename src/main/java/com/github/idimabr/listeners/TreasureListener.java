package com.github.idimabr.listeners;

import com.github.idimabr.TreasureHunt;
import com.github.idimabr.controllers.TreasureController;
import com.github.idimabr.models.PendingTreasure;
import com.github.idimabr.models.Treasure;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

@AllArgsConstructor
public class TreasureListener implements Listener {

    private final TreasureController controller;

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!event.getHand().name().equalsIgnoreCase("HAND")) return;

        final Location loc = block.getLocation();
        PendingTreasure pending = controller.removePendingTreasure(player.getUniqueId());
        if (pending != null) {
            String id = pending.id();
            String command = pending.command();

            Treasure treasure = new Treasure(id, command, loc);
            controller.addTreasure(treasure);

            player.sendMessage("§aTreasure '" + id + "' created successfully!");
            event.setCancelled(true);
            return;
        }

        controller.getTreasureByLocation(loc).ifPresent(treasure -> {
            controller.hasFoundTreasureAsync(player, treasure.getId()).thenAccept(found -> {
                Bukkit.getScheduler().runTask(TreasureHunt.getPlugin(), () -> {
                    if (found) {
                        player.sendMessage("§cYou have already found this treasure!");
                    } else {
                        controller.claimTreasureAsync(player, treasure.getId());
                        player.sendMessage("§aYEAH! You found the treasure '" + treasure.getId() + "'!");
                    }
                });
            });
        });

        event.setCancelled(true);
    }
}