package com.drc.aidbridge.domain.repository;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.supply.ReliefRequestDto;
import com.drc.aidbridge.data.remote.dto.supply.SupplyCategoryDto;

import java.util.List;

public interface SupplyRepository {

    LiveData<NetworkResultWrapper<List<SupplyCategoryDto>>> getSupplyCategories();

    LiveData<NetworkResultWrapper<String>> submitReliefRequest(ReliefRequestDto requestDto);
}
