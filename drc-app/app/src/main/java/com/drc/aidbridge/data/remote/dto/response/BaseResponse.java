package com.drc.aidbridge.data.remote.dto.response;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;

public class BaseResponse<T> {

    @SerializedName("success")
    private boolean success;

    @Nullable
    @SerializedName("message")
    private String message;

    @Nullable
    @SerializedName("data")
    private T data;

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getMessage() {
        return message != null ? message : "";
    }

    @Nullable
    public T getData() {
        return data;
    }
}
