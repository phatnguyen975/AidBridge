package com.drc.aidbridge.domain.repository.victim;

import androidx.lifecycle.LiveData;

import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.domain.model.victim.VictimReliefRequest;
import com.drc.aidbridge.domain.model.victim.VictimSupplyCategory;

import java.util.List;

public interface VictimSupplyRepository {

    LiveData<NetworkResultWrapper<List<VictimSupplyCategory>>> getSupplyCategories();

    LiveData<NetworkResultWrapper<String>> submitReliefRequest(VictimReliefRequest request);
}
