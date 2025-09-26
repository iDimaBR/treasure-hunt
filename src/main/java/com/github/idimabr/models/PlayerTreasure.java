package com.github.idimabr.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;
import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class PlayerTreasure {

    private final UUID playerUuid;
    private final String treasureId;
    private final Instant foundAt;
}