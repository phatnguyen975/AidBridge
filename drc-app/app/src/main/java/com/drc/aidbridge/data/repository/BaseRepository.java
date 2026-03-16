package com.drc.aidbridge.data.repository;

import org.json.JSONObject;
import retrofit2.Response;

/**
 * BaseRepository — parent class for all repositories.
 * Provides common methods for error handling and message extraction from Retrofit responses.
 */
public abstract class BaseRepository {

    /**
     * Extract error message from Retrofit Response.
     * Prioritizes reading JSON string from errorBody() to get the correct Vietnamese error message from the Server.
     */
    protected <T> String extractHttpError(Response<T> response) {
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                JSONObject jsonObject = new JSONObject(errorJson);
                if (jsonObject.has("message")) {
                    return jsonObject.getString("message");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Fallback to response.message() if errorBody is not available or doesn't contain "message"
        String message = response.message();
        if (message == null || message.trim().isEmpty()) {
            return "Yêu cầu thất bại (Lỗi " + response.code() + ")";
        }
        return message;
    }

    /**
     * Extract error message safely from an Exception/Throwable (Network errors, timeouts...).
     */
    protected String safeMessage(Throwable t) {
        return t != null && t.getMessage() != null ? t.getMessage() : "Lỗi kết nối không xác định";
    }
}
