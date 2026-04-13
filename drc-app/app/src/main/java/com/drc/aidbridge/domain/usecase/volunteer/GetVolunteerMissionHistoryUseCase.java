package com.drc.aidbridge.domain.usecase.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.mapper.volunteer.VolunteerInfoMapper;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerHistoryItemDto;
import com.drc.aidbridge.domain.model.volunteer.VolunteerHistoryItem;
import com.drc.aidbridge.domain.repository.volunteer.VolunteerRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class GetVolunteerMissionHistoryUseCase {

    private final VolunteerRepository volunteerRepository;
    private final VolunteerInfoMapper volunteerInfoMapper;

    @Inject
    public GetVolunteerMissionHistoryUseCase(VolunteerRepository volunteerRepository,
            VolunteerInfoMapper volunteerInfoMapper) {
        this.volunteerRepository = volunteerRepository;
        this.volunteerInfoMapper = volunteerInfoMapper;
    }

    public LiveData<NetworkResultWrapper<List<VolunteerHistoryItem>>> execute() {
        return Transformations.map(
                volunteerRepository.getMissionHistory(),
                result -> {
                    if (result == null) {
                        return NetworkResultWrapper.error("Dữ liệu lịch sử nhiệm vụ không hợp lệ.");
                    }

                    if (result.isLoading()) {
                        return NetworkResultWrapper.loading();
                    }

                    if (result.isError()) {
                        return NetworkResultWrapper.error(result.getMessage());
                    }

                    List<VolunteerHistoryItemDto> source = result.getData();
                    List<VolunteerHistoryItem> mapped = new ArrayList<>();
                    if (source != null) {
                        for (VolunteerHistoryItemDto itemDto : source) {
                            mapped.add(volunteerInfoMapper.mapToHistoryItemDomain(itemDto));
                        }
                    }
                    return NetworkResultWrapper.success(mapped);
                });
    }
}
