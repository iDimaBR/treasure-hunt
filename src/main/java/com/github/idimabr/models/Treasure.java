package com.github.idimabr.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;

@AllArgsConstructor
@Getter
public class Treasure {

    private final String id;
    private final String command;
    private final Location location;

}
