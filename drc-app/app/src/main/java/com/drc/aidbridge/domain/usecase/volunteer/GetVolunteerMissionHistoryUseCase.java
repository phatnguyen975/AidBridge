package com.drc.aidbridge.domain.usecase.volunteer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.drc.aidbridge.data.mapper.volunteer.VolunteerInfoMapper;
import com.drc.aidbridge.data.remote.NetworkResultWrapper;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerHistoryDataDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerHistoryItemDto;
import com.drc.aidbridge.data.remote.dto.response.volunteer.VolunteerHistoryPaginationDto;
import com.drc.aidbridge.domain.model.volunteer.VolunteerHistoryItem;
import com.drc.aidbridge.domain.model.volunteer.VolunteerHistoryPage;
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

    public LiveData<NetworkResultWrapper<VolunteerHistoryPage>> execute(int page, int limit) {
        return Transformations.map(
                volunteerRepository.getMissionHistory(page, limit),
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

                    VolunteerHistoryDataDto dataDto = result.getData();
                    List<VolunteerHistoryItem> mapped = new ArrayList<>();
                    List<VolunteerHistoryItemDto> sourceItems = dataDto != null ? dataDto.getItems() : null;
                    if (sourceItems != null) {
                        for (VolunteerHistoryItemDto itemDto : sourceItems) {
                            mapped.add(volunteerInfoMapper.mapToHistoryItemDomain(itemDto));
                        }
                    }

                    VolunteerHistoryPaginationDto pagination = dataDto != null ? dataDto.getPagination() : null;
                    int mappedPage = pagination != null ? pagination.getPage() : page;
                    int mappedLimit = pagination != null ? pagination.getLimit() : limit;
                    long mappedTotal = pagination != null ? pagination.getTotal() : mapped.size();
                    int mappedTotalPages = pagination != null ? pagination.getTotalPages() : 1;
                    boolean mappedHasNext = pagination != null && pagination.isHasNext();
                    boolean mappedHasPrevious = pagination != null && pagination.isHasPrevious();

                    VolunteerHistoryPage historyPage = new VolunteerHistoryPage(
                            mapped,
                            mappedPage,
                            mappedLimit,
                            mappedTotal,
                            mappedTotalPages,
                            mappedHasNext,
                            mappedHasPrevious);
                    return NetworkResultWrapper.success(historyPage);
                });
    }
}
