package com.drc.aidbridge.ui.main.viewmodel.admin;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.drc.aidbridge.R;
import com.drc.aidbridge.ui.base.BaseViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class AdminHubDetailViewModel extends BaseViewModel {

    private final MutableLiveData<List<InventoryCategory>> inventoryCategories = new MutableLiveData<>(
            new ArrayList<>());

    @Inject
    public AdminHubDetailViewModel() {
        // TODO: Tích hợp API Backend qua UseCase để lấy dữ liệu tồn kho thực tế của
        // trạm theo thời gian thực
    }

    public LiveData<List<InventoryCategory>> getInventoryCategories() {
        return inventoryCategories;
    }

    public void loadMockInventory() {
        List<InventoryCategory> categories = new ArrayList<>();

        categories.add(new InventoryCategory(
                R.string.admin_hub_category_medicine,
                R.drawable.ic_admin_category_medicine,
                140,
                Arrays.asList(
                        new InventoryItem(R.string.admin_hub_item_medicine_fever, 52, R.string.admin_hub_unit_strip),
                        new InventoryItem(R.string.admin_hub_item_medicine_digestive, 41,
                                R.string.admin_hub_unit_strip),
                        new InventoryItem(R.string.admin_hub_item_medicine_bandage, 66,
                                R.string.admin_hub_unit_piece))));

        categories.add(new InventoryCategory(
                R.string.admin_hub_category_clothes,
                R.drawable.ic_admin_category_clothes,
                230,
                Arrays.asList(
                        new InventoryItem(R.string.admin_hub_item_clothes_set, 60, R.string.admin_hub_unit_set),
                        new InventoryItem(R.string.admin_hub_item_clothes_blanket, 58, R.string.admin_hub_unit_piece),
                        new InventoryItem(R.string.admin_hub_item_clothes_raincoat, 44,
                                R.string.admin_hub_unit_piece))));

        categories.add(new InventoryCategory(
                R.string.admin_hub_category_food,
                R.drawable.ic_admin_category_food,
                180,
                Arrays.asList(
                        new InventoryItem(R.string.admin_hub_item_food_rice, 120, R.string.admin_hub_unit_kg),
                        new InventoryItem(R.string.admin_hub_item_food_noodle, 38, R.string.admin_hub_unit_carton),
                        new InventoryItem(R.string.admin_hub_item_food_canned, 220, R.string.admin_hub_unit_box))));

        categories.add(new InventoryCategory(
                R.string.admin_hub_category_water,
                R.drawable.ic_admin_category_water,
                150,
                Arrays.asList(
                        new InventoryItem(R.string.admin_hub_item_water_bottle, 42, R.string.admin_hub_unit_carton),
                        new InventoryItem(R.string.admin_hub_item_water_milk, 36, R.string.admin_hub_unit_carton),
                        new InventoryItem(R.string.admin_hub_item_water_electrolyte, 25,
                                R.string.admin_hub_unit_carton))));

        categories.add(new InventoryCategory(
                R.string.admin_hub_category_other,
                R.drawable.ic_admin_category_other,
                45,
                Arrays.asList(
                        new InventoryItem(R.string.admin_hub_item_other_diaper, 28, R.string.admin_hub_unit_piece))));

        inventoryCategories.setValue(categories);
    }

    public static class InventoryCategory {
        @StringRes
        public final int nameResId;
        @DrawableRes
        public final int iconResId;
        public final int minimumUnits;
        @NonNull
        public final List<InventoryItem> items;

        public InventoryCategory(@StringRes int nameResId,
                @DrawableRes int iconResId,
                int minimumUnits,
                @NonNull List<InventoryItem> items) {
            this.nameResId = nameResId;
            this.iconResId = iconResId;
            this.minimumUnits = minimumUnits;
            this.items = items;
        }

        public int totalQuantity() {
            int sum = 0;
            for (InventoryItem item : items) {
                sum += item.quantity;
            }
            return sum;
        }

        public boolean isEnough() {
            return totalQuantity() >= minimumUnits;
        }
    }

    public static class InventoryItem {
        @StringRes
        public final int nameResId;
        public final int quantity;
        @StringRes
        public final int unitResId;

        public InventoryItem(@StringRes int nameResId, int quantity, @StringRes int unitResId) {
            this.nameResId = nameResId;
            this.quantity = quantity;
            this.unitResId = unitResId;
        }
    }
}
