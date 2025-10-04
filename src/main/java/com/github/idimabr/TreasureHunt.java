package com.github.idimabr;

import com.github.idimabr.commands.TreasureCommand;
import com.github.idimabr.controllers.TreasureController;
import com.github.idimabr.listeners.TreasureListener;
import com.github.idimabr.storage.TreasureRepository;
import com.github.idimabr.utils.ConfigUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TreasureHunt extends JavaPlugin {

    @Getter
    private static TreasureHunt plugin;
    private TreasureController controller;
    private TreasureRepository repository;
    private ConfigUtil config;

    private final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);

    @Override
    public void onLoad() {
        this.config = new ConfigUtil(this, "config.yml");
    }

    @Override
    public void onEnable() {
        plugin = this;
        loadStorage();
        loadControllers();
        loadCommands();
        loadListeners();
    }

    @Override
    public void onDisable() {
        dbExecutor.shutdownNow();
    }

    private void loadControllers(){
        this.controller = new TreasureController(this, repository);
    }

    private void loadCommands(){
        this.getCommand("treasure").setExecutor(new TreasureCommand(controller));
    }

    private void loadListeners(){
        final PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new TreasureListener(controller), this);
    }

    private void loadStorage() {
        final String address = config.getString("Database.address");
        final String database = config.getString("Database.database");
        final String jdbcUrl = "jdbc:mysql://" + address + "/" + database + "?useSSL=false&serverTimezone=UTC";
        final String username = config.getString("Database.username");
        final String password = config.getString("Database.password");
        this.repository = new TreasureRepository(this, jdbcUrl, username, password);
        repository.testConnection().thenAccept(connected -> {
            if (connected) {
                getLogger().info("Database connected successfully!");
                repository.createTables().thenRun(() -> {
                    getLogger().info("Tables created!");
                    controller.loadTreasuresFromDBAsync();
                });
            } else {
                getLogger().severe("Could not connect to the database! Please check your config.");
                getServer().getPluginManager().disablePlugin(this);
            }
        }).exceptionally(ex -> {
            getLogger().severe("Failed to create tables: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    public ExecutorService getExecutor() {
        return dbExecutor;
    }
}
