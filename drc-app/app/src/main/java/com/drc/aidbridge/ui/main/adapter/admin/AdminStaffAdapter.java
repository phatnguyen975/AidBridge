package com.drc.aidbridge.ui.main.adapter.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.ItemAdminStaffBinding;
import com.drc.aidbridge.domain.model.admin.Staff;

import java.util.ArrayList;
import java.util.List;

public class AdminStaffAdapter extends RecyclerView.Adapter<AdminStaffAdapter.StaffViewHolder> {

    private final List<Staff> items = new ArrayList<>();

    @NonNull
    @Override
    public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemAdminStaffBinding binding = ItemAdminStaffBinding.inflate(inflater, parent, false);
        return new StaffViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StaffViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<Staff> staffList) {
        items.clear();
        if (staffList != null) {
            items.addAll(staffList);
        }
        notifyDataSetChanged();
    }

    static class StaffViewHolder extends RecyclerView.ViewHolder {

        private final ItemAdminStaffBinding binding;

        StaffViewHolder(@NonNull ItemAdminStaffBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Staff staff) {
            binding.textStaffName.setText(resolveText(
                    staff.getFullName(),
                    binding.getRoot().getContext().getString(R.string.admin_staff_name_fallback)
            ));
            binding.textStaffContact.setText(resolveContact(staff));
            binding.textStaffHub.setText(binding.getRoot().getContext().getString(
                    R.string.admin_staff_hub_format,
                    resolveText(
                            staff.getHubName(),
                            binding.getRoot().getContext().getString(R.string.admin_staff_hub_fallback)
                    )
            ));
        }

        private String resolveContact(Staff staff) {
            String email = resolveText(staff.getEmail(), "");
            String phone = resolveText(staff.getPhoneNumber(), "");
            if (!email.isEmpty() && !phone.isEmpty()) {
                return email + " | " + phone;
            }
            if (!email.isEmpty()) {
                return email;
            }
            if (!phone.isEmpty()) {
                return phone;
            }
            return binding.getRoot().getContext().getString(R.string.admin_staff_contact_fallback);
        }

        private String resolveText(String value, String fallback) {
            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }
            return value.trim();
        }
    }
}
