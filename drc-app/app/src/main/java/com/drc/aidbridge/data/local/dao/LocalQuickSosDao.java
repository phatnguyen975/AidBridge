package com.drc.aidbridge.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.drc.aidbridge.data.local.entity.LocalQuickSosEntity;

import java.util.List;

@Dao
public interface LocalQuickSosDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LocalQuickSosEntity entity);

    @Query("SELECT * FROM local_quick_sos WHERE client_request_id = :clientRequestId LIMIT 1")
    LocalQuickSosEntity findByClientRequestId(String clientRequestId);

    @Query("SELECT * FROM local_quick_sos WHERE status IN (:statuses) ORDER BY created_at_millis ASC LIMIT :limit")
    List<LocalQuickSosEntity> getByStatuses(List<String> statuses, int limit);

    @Query("UPDATE local_quick_sos SET status = :status, updated_at_millis = :updatedAtMillis, last_error = :lastError WHERE client_request_id = :clientRequestId")
    void updateStatus(String clientRequestId, String status, long updatedAtMillis, @Nullable String lastError);

    @Query("UPDATE local_quick_sos SET status = :status, retry_count = retry_count + 1, updated_at_millis = :updatedAtMillis, last_error = :lastError WHERE client_request_id = :clientRequestId")
    void markFailed(String clientRequestId, String status, long updatedAtMillis, @Nullable String lastError);

    @Query("UPDATE local_quick_sos SET status = 'SYNCED', server_sos_id = :serverSosId, updated_at_millis = :updatedAtMillis, last_error = NULL WHERE client_request_id = :clientRequestId")
    void markSynced(String clientRequestId, @Nullable String serverSosId, long updatedAtMillis);

    @Query("SELECT COUNT(*) FROM local_quick_sos WHERE status IN (:statuses)")
    int countByStatuses(List<String> statuses);
}
