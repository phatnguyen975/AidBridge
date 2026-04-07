package com.drc.aidbridge.modules.sponsor.internal.usecase;

import org.springframework.stereotype.Component;

import com.drc.aidbridge.modules.user.UserFacade;
import com.drc.aidbridge.modules.user.UserDTO;
import lombok.RequiredArgsConstructor;
import com.drc.aidbridge.modules.sponsor.internal.entity.Sponsor;
import com.drc.aidbridge.modules.sponsor.internal.mapper.SponsorMapper;
import com.drc.aidbridge.modules.sponsor.internal.repository.SponsorJpaRepository;
import com.drc.aidbridge.modules.sponsor.internal.web.dto.SponsorProfileResponse;
import java.util.UUID;
import com.drc.aidbridge.modules.shared.exception.ResourceNotFoundException;
@Component
@RequiredArgsConstructor
public class GetSponsorProfileUseCase {
    
    private final SponsorJpaRepository sponsorRepository;
    private final UserFacade userFacade;
    private final SponsorMapper sponsorMapper;
    public SponsorProfileResponse execute(UUID userId) {
        UserDTO user = userFacade.getUserById(userId);
        Sponsor sponsor = sponsorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor profile not found"));
        return sponsorMapper.toResponse(sponsor, user);
    }
}
