package com.drc.aidbridge.domain.model.staff;

import androidx.annotation.NonNull;

public class StaffUpcomingTask {

    private final String id;
    private final String code;
    private final String name;
    private final String phone;

    public StaffUpcomingTask(String id, String code, String name, String phone) {
        this.id = safeText(id);
        this.code = safeText(code);
        this.name = safeText(name);
        this.phone = safeText(phone);
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getCode() {
        return code;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getPhone() {
        return phone;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
