package com.github.idimabr.storage.adapter;

import com.github.idimabr.models.Treasure;
import com.henryfabio.sqlprovider.executor.adapter.SQLResultAdapter;
import com.henryfabio.sqlprovider.executor.result.SimpleResultSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class TreasureAdapter implements SQLResultAdapter<Treasure> {

    @Override
    public Treasure adaptResult(SimpleResultSet rs) {
        String id = rs.get("id");
        String command = rs.get("command");
        String worldName = rs.get("world");
        World world = Bukkit.getWorld(worldName);

        int x = ((Number) rs.get("x")).intValue();
        int y = ((Number) rs.get("y")).intValue();
        int z = ((Number) rs.get("z")).intValue();

        Location location = new Location(world, x, y, z);

        return new Treasure(id, command, location);
    }
}
