package com.drc.aidbridge.modules.sponsor.internal.mapper;
import org.springframework.stereotype.Component;
import com.drc.aidbridge.modules.sponsor.internal.entity.Sponsor;
import com.drc.aidbridge.modules.user.UserDTO;
import com.drc.aidbridge.modules.sponsor.SponsorDTO;
import com.drc.aidbridge.modules.sponsor.internal.web.dto.SponsorProfileResponse;


@Component
public class SponsorMapper {
    public SponsorDTO toDTO (Sponsor entity) {
        return SponsorDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .organizationName(entity.getOrganizationName())
                .organizationType(entity.getOrganizationType())
                .donationCount(entity.getDonationCount())
                .totalDonatedValue(entity.getTotalDonatedValue())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }    

    public SponsorProfileResponse toResponse (Sponsor entity, UserDTO user){
        return SponsorProfileResponse.builder()
                .user(user)
                .sponsor(toDTO(entity))
                .build();
    }

    
}
