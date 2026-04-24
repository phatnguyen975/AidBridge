package com.drc.aidbridge.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.drc.aidbridge.data.local.entity.PendingSosLocationUpdateEntity;

@Dao
public interface PendingSosLocationUpdateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PendingSosLocationUpdateEntity entity);

    @Query("SELECT * FROM pending_sos_location_updates WHERE sosId = :sosId LIMIT 1")
    @Nullable
    PendingSosLocationUpdateEntity findBySosId(String sosId);

    @Query("DELETE FROM pending_sos_location_updates WHERE sosId = :sosId")
    void deleteBySosId(String sosId);

    @Query("DELETE FROM pending_sos_location_updates WHERE sosId = :sosId AND updatedAtMillis = :updatedAtMillis")
    int deleteIfUnchanged(String sosId, long updatedAtMillis);
}
