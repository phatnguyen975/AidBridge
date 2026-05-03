package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.mission.internal.repository.MissionJpaRepository;
import com.drc.aidbridge.modules.mission.internal.repository.projection.MissionHistoryProjection;
import com.drc.aidbridge.modules.shared.enums.MissionType;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import com.drc.aidbridge.modules.volunteer.internal.web.dto.VolunteerMissionHistoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetVolunteerMissionHistoryUseCaseTest {

    private VolunteerJpaRepository volunteerRepository;
    private MissionJpaRepository missionRepository;
    private GetVolunteerMissionHistoryUseCase useCase;

    @BeforeEach
    void setUp() {
        volunteerRepository = mock(VolunteerJpaRepository.class);
        missionRepository = mock(MissionJpaRepository.class);
        useCase = new GetVolunteerMissionHistoryUseCase(
                volunteerRepository,
                missionRepository);
    }

    @Test
    void execute_ShouldReturnMissionHistoryWithPagination() {
        UUID userId = UUID.randomUUID();

        MissionHistoryProjection projection = mock(MissionHistoryProjection.class);
        when(projection.getMissionType()).thenReturn("RESCUE");
        when(projection.getCompletedAt()).thenReturn(java.time.Instant.parse("2026-04-12T10:15:30Z"));
        when(projection.getAddress()).thenReturn("123 Nguyen Hue, Da Nang");

        when(volunteerRepository.existsByUserId(userId)).thenReturn(true);
        when(missionRepository.findHistoryProjectionByVolunteerId(eq(userId), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 10), 1));

        VolunteerMissionHistoryResponse response = useCase.execute(userId, 1, 10);

        assertEquals(1, response.getItems().size());
        assertEquals(MissionType.RESCUE, response.getItems().get(0).getMissionType());
        assertEquals("123 Nguyen Hue, Da Nang", response.getItems().get(0).getAddress());
        assertEquals(java.time.Instant.parse("2026-04-12T10:15:30Z"), response.getItems().get(0).getCompletedAt());
        assertEquals(1, response.getPagination().getPage());
        assertEquals(10, response.getPagination().getLimit());
        assertEquals(1L, response.getPagination().getTotal());
        assertEquals(1, response.getPagination().getTotalPages());
    }

    @Test
    void execute_ShouldThrow_WhenVolunteerProfileNotFound() {
        UUID userId = UUID.randomUUID();
        when(volunteerRepository.existsByUserId(userId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(userId, 1, 10));
    }
}
