package com.github.idimabr;

import com.github.idimabr.commands.TreasureCommand;
import com.github.idimabr.controllers.TreasureController;
import com.github.idimabr.listeners.TreasureListener;
import com.github.idimabr.storage.Database;
import com.github.idimabr.storage.dao.TreasureRepository;
import com.github.idimabr.utils.ConfigUtil;
import com.henryfabio.sqlprovider.connector.SQLConnector;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class TreasureHunt extends JavaPlugin {

    private TreasureController controller;
    private TreasureRepository repository;
    private SQLConnector connection;
    private ConfigUtil config;

    @Override
    public void onLoad() {
        this.config = new ConfigUtil(this, "config.yml");
    }

    @Override
    public void onEnable() {
        loadStorage();
        loadControllers();
        loadCommands();
        loadListeners();
    }

    @Override
    public void onDisable() {

    }

    private void loadControllers(){
        this.controller = new TreasureController(repository);
    }

    private void loadCommands(){
        this.getCommand("treasure").setExecutor(new TreasureCommand(controller));
    }

    private void loadListeners(){
        final PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new TreasureListener(controller), this);
    }

    private void loadStorage() {
        this.connection = new Database(this).createConnector(config.getConfigurationSection("Database"));
        this.repository = new TreasureRepository(this, connection);
        this.repository.createTables();
    }
}
