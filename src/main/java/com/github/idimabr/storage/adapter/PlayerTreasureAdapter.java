package com.github.idimabr.storage.adapter;

import com.github.idimabr.models.PlayerTreasure;
import com.henryfabio.sqlprovider.executor.adapter.SQLResultAdapter;
import com.henryfabio.sqlprovider.executor.result.SimpleResultSet;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class PlayerTreasureAdapter implements SQLResultAdapter<PlayerTreasure> {

    @Override
    public PlayerTreasure adaptResult(SimpleResultSet rs) {
        UUID playerUuid = UUID.fromString(rs.get("uuid"));
        String treasureId = rs.get("treasure_id");
        long foundAtMillis = ((Number) rs.get("found_at")).longValue();
        Instant foundAt = Instant.ofEpochMilli(foundAtMillis);

        return new PlayerTreasure(playerUuid, treasureId, foundAt);
    }
}
