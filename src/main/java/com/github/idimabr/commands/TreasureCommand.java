package com.github.idimabr.commands;

import com.github.idimabr.TreasureHunt;
import com.github.idimabr.controllers.TreasureController;
import com.github.idimabr.models.PendingTreasure;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class TreasureCommand implements CommandExecutor {

    private final TreasureController controller;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can execute this command.");
            return true;
        }

        if(!player.hasPermission("treasure.admin")) {
            player.sendMessage("§cYou do not have permission to execute this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "completed" -> handleCompleted(player, args);
            case "list" -> handleList(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cUse /treasure create <id> <command>");
            return;
        }

        if(controller.getTreasures().containsKey(args[1])) {
            player.sendMessage("§cA treasure with this ID already exists.");
            return;
        }

        String id = args[1];
        String command = String.join(" ", List.of(args).subList(2, args.length));
        player.sendMessage("§aSuccess! Now click on a block to set the treasure location.");
        controller.addPendingTreasure(player.getUniqueId(), new PendingTreasure(id, command));
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUse /treasure delete <id>");
            return;
        }

        String id = args[1];

        if (!controller.getTreasures().containsKey(id)) {
            player.sendMessage("§cTreasure not found.");
            return;
        }

        controller.removeTreasure(id);
        player.sendMessage("§aTreasure '" + id + "' deleted successfully.");
    }

    private void handleCompleted(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUse /treasure completed <id>");
            return;
        }

        String id = args[1];

        if (!controller.getTreasures().containsKey(id)) {
            player.sendMessage("§cTreasure not found.");
            return;
        }

        controller.getPlayersFoundTreasureAsync(id).thenAccept(uuids -> {
            if (uuids.isEmpty()) {
                player.sendMessage("§cNothing to show here.");
                return;
            }

            player.sendMessage("§aPlayers who found '" + id + "':");
            for (UUID p : uuids) {
                player.sendMessage("§f - " + Bukkit.getOfflinePlayer(p).getName());
            }
        });
    }

    private void handleList(Player player) {
        if (controller.getTreasures().isEmpty()) {
            player.sendMessage("§cNo treasures registered.");
            return;
        }

        player.sendMessage("§aRegistered treasures:");
        for (String id : controller.getTreasures().keySet()) {
            player.sendMessage("§f - " + id);
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§a=== Treasure Commands ===");
        player.sendMessage("§f/treasure create <id> <command> - Create a treasure");
        player.sendMessage("§f/treasure delete <id> - Delete a treasure");
        player.sendMessage("§f/treasure completed <id> - List players who found a treasure");
        player.sendMessage("§f/treasure list - List all treasures");
    }
}