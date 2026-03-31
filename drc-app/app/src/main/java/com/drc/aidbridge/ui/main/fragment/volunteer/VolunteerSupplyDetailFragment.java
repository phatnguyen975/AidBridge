package com.drc.aidbridge.ui.main.fragment.volunteer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.drc.aidbridge.R;
import com.drc.aidbridge.databinding.FragmentVolunteerSupplyDetailBinding;
import com.drc.aidbridge.ui.base.BaseFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class VolunteerSupplyDetailFragment extends BaseFragment<FragmentVolunteerSupplyDetailBinding> {

    @Override
    protected FragmentVolunteerSupplyDetailBinding inflateBinding(LayoutInflater inflater,
            @Nullable ViewGroup container) {
        return FragmentVolunteerSupplyDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void setupViews() {
        setupToggleLogic();
        setupClickListeners();
        // mockSupplyItemsData();
    }

    @Override
    protected void observeViewModel() {
        // TODO: Observe Supply Detail UI state from ViewModel when API is integrated.
    }

    private void setupToggleLogic() {
        // Default state: all sections are expanded.
        setSectionExpanded(binding.layoutItemsDrug, binding.ivChevronDrug, true);
        setSectionExpanded(binding.layoutItemsClothes, binding.ivChevronClothes, true);
        setSectionExpanded(binding.layoutItemsFood, binding.ivChevronFood, true);
        setSectionExpanded(binding.layoutItemsWater, binding.ivChevronWater, true);
        setSectionExpanded(binding.layoutItemsOther, binding.ivChevronOther, true);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> popBackStackSafely());

        bindToggle(binding.layoutHeaderDrug, binding.layoutItemsDrug, binding.ivChevronDrug);
        bindToggle(binding.layoutHeaderClothes, binding.layoutItemsClothes, binding.ivChevronClothes);
        bindToggle(binding.layoutHeaderFood, binding.layoutItemsFood, binding.ivChevronFood);
        bindToggle(binding.layoutHeaderWater, binding.layoutItemsWater, binding.ivChevronWater);
        bindToggle(binding.layoutHeaderOther, binding.layoutItemsOther, binding.ivChevronOther);
    }

    private void bindToggle(View header, View content, ImageView chevron) {
        header.setOnClickListener(v -> {
            boolean isExpanded = content.getVisibility() == View.VISIBLE;
            setSectionExpanded(content, chevron, !isExpanded);
        });
    }

    private void setSectionExpanded(View content, ImageView chevron, boolean expanded) {
        content.setVisibility(expanded ? View.VISIBLE : View.GONE);
        chevron.setRotation(expanded ? 0f : 180f);
    }

    private void mockSupplyItemsData() {
        binding.tvDrugItemType1.setText(R.string.volunteer_supply_detail_drug_item_type_1);
        binding.tvDrugItemQty1.setText(R.string.volunteer_supply_detail_drug_item_quantity_1);
        binding.tvDrugItemType2.setText(R.string.volunteer_supply_detail_drug_item_type_2);
        binding.tvDrugItemQty2.setText(R.string.volunteer_supply_detail_drug_item_quantity_2);
        binding.tvDrugItemType3.setText(R.string.volunteer_supply_detail_drug_item_type_3);
        binding.tvDrugItemQty3.setText(R.string.volunteer_supply_detail_drug_item_quantity_3);

        binding.tvClothesItemType1.setText(R.string.volunteer_supply_detail_clothes_item_type_1);
        binding.tvClothesItemQty1.setText(R.string.volunteer_supply_detail_clothes_item_quantity_1);
        binding.tvClothesItemType2.setText(R.string.volunteer_supply_detail_clothes_item_type_2);
        binding.tvClothesItemQty2.setText(R.string.volunteer_supply_detail_clothes_item_quantity_2);
        binding.tvClothesItemType3.setText(R.string.volunteer_supply_detail_clothes_item_type_3);
        binding.tvClothesItemQty3.setText(R.string.volunteer_supply_detail_clothes_item_quantity_3);

        binding.tvFoodItemType1.setText(R.string.volunteer_supply_detail_food_item_type_1);
        binding.tvFoodItemQty1.setText(R.string.volunteer_supply_detail_food_item_quantity_1);
        binding.tvFoodItemType2.setText(R.string.volunteer_supply_detail_food_item_type_2);
        binding.tvFoodItemQty2.setText(R.string.volunteer_supply_detail_food_item_quantity_2);
        binding.tvFoodItemType3.setText(R.string.volunteer_supply_detail_food_item_type_3);
        binding.tvFoodItemQty3.setText(R.string.volunteer_supply_detail_food_item_quantity_3);

        binding.tvWaterItemType1.setText(R.string.volunteer_supply_detail_water_item_type_1);
        binding.tvWaterItemQty1.setText(R.string.volunteer_supply_detail_water_item_quantity_1);
        binding.tvWaterItemType2.setText(R.string.volunteer_supply_detail_water_item_type_2);
        binding.tvWaterItemQty2.setText(R.string.volunteer_supply_detail_water_item_quantity_2);
        binding.tvWaterItemType3.setText(R.string.volunteer_supply_detail_water_item_type_3);
        binding.tvWaterItemQty3.setText(R.string.volunteer_supply_detail_water_item_quantity_3);

        binding.tvOtherItemType1.setText(R.string.volunteer_supply_detail_other_item_type_1);
        binding.tvOtherItemQty1.setText(R.string.volunteer_supply_detail_other_item_quantity_1);
    }
}
