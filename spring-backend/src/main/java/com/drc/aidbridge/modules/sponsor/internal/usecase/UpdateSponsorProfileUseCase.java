package com.drc.aidbridge.modules.sponsor.internal.usecase;

import com.drc.aidbridge.modules.sponsor.internal.entity.Sponsor;
import com.drc.aidbridge.modules.sponsor.internal.mapper.SponsorMapper;
import com.drc.aidbridge.modules.sponsor.internal.repository.SponsorJpaRepository;
import com.drc.aidbridge.modules.sponsor.internal.web.dto.SponsorProfileResponse;
import com.drc.aidbridge.modules.sponsor.internal.web.dto.UpdateSponsorRequest;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateSponsorProfileUseCase {

    private final SponsorJpaRepository sponsorRepository;
    private final SponsorMapper sponsorMapper;
    private final UserFacade userFacade;

    @Transactional
    public SponsorProfileResponse execute(UUID userId, UpdateSponsorRequest request) {
        Sponsor sponsor = sponsorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor profile not found"));

        sponsor.setOrganizationName(request.getOrganizationName());
        sponsor.setOrganizationType(request.getOrganizationType());

        sponsor = sponsorRepository.save(sponsor);
        log.info("Updated sponsor profile for user: {}", userId);

        UserDTO user = userFacade.getUserById(userId);
        return sponsorMapper.toResponse(sponsor, user);
    }
}
