package com.drc.aidbridge.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.drc.aidbridge.data.local.entity.GatewayPendingSmsEntity;

import java.util.List;

@Dao
public interface GatewayPendingSmsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(GatewayPendingSmsEntity entity);

    @Query("SELECT * FROM gateway_pending_sms WHERE client_request_id = :clientRequestId LIMIT 1")
    GatewayPendingSmsEntity findByClientRequestId(String clientRequestId);

    @Query("SELECT * FROM gateway_pending_sms WHERE status IN (:statuses) ORDER BY received_at_gateway_millis ASC LIMIT :limit")
    List<GatewayPendingSmsEntity> getByStatuses(List<String> statuses, int limit);

    @Query("UPDATE gateway_pending_sms SET status = :status, updated_at_millis = :updatedAtMillis, last_error = :lastError WHERE client_request_id = :clientRequestId")
    void updateStatus(String clientRequestId, String status, long updatedAtMillis, @Nullable String lastError);

    @Query("UPDATE gateway_pending_sms SET status = :status, retry_count = retry_count + 1, updated_at_millis = :updatedAtMillis, last_error = :lastError WHERE client_request_id = :clientRequestId")
    void markFailed(String clientRequestId, String status, long updatedAtMillis, @Nullable String lastError);

    @Query("UPDATE gateway_pending_sms SET status = 'FORWARDED', server_sos_id = :serverSosId, updated_at_millis = :updatedAtMillis, last_error = NULL WHERE client_request_id = :clientRequestId")
    void markForwarded(String clientRequestId, @Nullable String serverSosId, long updatedAtMillis);

    @Query("SELECT COUNT(*) FROM gateway_pending_sms")
    int countAll();

    @Query("SELECT COUNT(*) FROM gateway_pending_sms WHERE status IN (:statuses)")
    int countByStatuses(List<String> statuses);

    @Query("SELECT last_error FROM gateway_pending_sms WHERE last_error IS NOT NULL AND last_error != '' ORDER BY updated_at_millis DESC LIMIT 1")
    String getLatestError();
}
