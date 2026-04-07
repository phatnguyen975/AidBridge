package com.drc.aidbridge.modules.sponsor.internal.usecase;

import com.drc.aidbridge.modules.sponsor.SponsorDTO;
import com.drc.aidbridge.modules.sponsor.internal.entity.Sponsor;
import com.drc.aidbridge.modules.sponsor.internal.mapper.SponsorMapper;
import com.drc.aidbridge.modules.sponsor.internal.repository.SponsorJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateSponsorProfileUseCase {

    private final SponsorJpaRepository sponsorRepository;
    private final SponsorMapper sponsorMapper;

    @Transactional
    public SponsorDTO execute(UUID userId) {
        return sponsorRepository.findByUserId(userId)
                .map(sponsorMapper::toDTO)
                .orElseGet(() -> {
                    Sponsor sponsor = Sponsor.builder()
                            .userId(userId)
                            .organizationName(null)
                            .organizationType(null)
                            .donationCount(0)
                            .totalDonatedValue(BigDecimal.ZERO)
                            .build();

                    sponsor = sponsorRepository.save(sponsor);
                    log.info("Created sponsor profile for user: {}", userId);
                    return sponsorMapper.toDTO(sponsor);
                });
    }
}



