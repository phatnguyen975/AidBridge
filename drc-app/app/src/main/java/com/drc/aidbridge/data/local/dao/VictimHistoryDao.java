package com.drc.aidbridge.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.drc.aidbridge.data.local.entity.VictimHistoryEntity;

import java.util.List;

@Dao
public interface VictimHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<VictimHistoryEntity> entities);

    @Query("SELECT * FROM victim_history_cache WHERE timeRange = :timeRange AND page = :page ORDER BY orderInPage ASC")
    List<VictimHistoryEntity> getPage(String timeRange, int page);

    @Query("DELETE FROM victim_history_cache WHERE timeRange = :timeRange")
    void clearByTimeRange(String timeRange);

    @Query("SELECT hasNextPage FROM victim_history_cache WHERE timeRange = :timeRange AND page = :page LIMIT 1")
    Integer getHasNextPage(String timeRange, int page);

    @Query("SELECT * FROM victim_history_cache ORDER BY cachedAt DESC, page ASC, orderInPage ASC")
    List<VictimHistoryEntity> getAllCached();
}
