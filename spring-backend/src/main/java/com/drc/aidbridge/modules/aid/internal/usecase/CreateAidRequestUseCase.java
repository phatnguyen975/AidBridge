package com.drc.aidbridge.modules.aid.internal.usecase;

import com.drc.aidbridge.modules.aid.AidRequestCreatedEvent;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequest;
import com.drc.aidbridge.modules.aid.internal.entity.AidRequestItem;
import com.drc.aidbridge.modules.aid.internal.mapper.AidMapper;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestItemJpaRepository;
import com.drc.aidbridge.modules.aid.internal.repository.AidRequestJpaRepository;
import com.drc.aidbridge.modules.aid.internal.web.dto.AidRequestResponse;
import com.drc.aidbridge.modules.aid.internal.web.dto.CreateAidRequest;
import com.drc.aidbridge.modules.shared.enums.AidStatus;
import com.drc.aidbridge.modules.user.UserFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateAidRequestUseCase {

    private final AidRequestJpaRepository aidRequestRepository;
    private final AidRequestItemJpaRepository aidRequestItemRepository;
    private final UserFacade userFacade;
    private final AidMapper aidMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AidRequestResponse execute(UUID requesterId, CreateAidRequest request) {
        userFacade.getUserById(requesterId);

        int adults = request.getAdultsCount() != null ? request.getAdultsCount() : 0;
        int elderly = request.getElderlyCount() != null ? request.getElderlyCount() : 0;
        int children = request.getChildrenCount() != null ? request.getChildrenCount() : 0;

        if ((adults + elderly + children) <= 0) {
            throw new IllegalArgumentException("Total people count must be greater than 0");
        }

        AidRequest aidRequest = AidRequest.builder()
            .requesterId(requesterId)
            .status(AidStatus.PENDING)
            .location(AidRequest.createPoint(request.getLat().doubleValue(), request.getLng().doubleValue()))
            .address(request.getAddress())
            .description(request.getNotes())
            .numberAdult(adults)
            .numberElderly(elderly)
            .numberChildren(children)
            .build();

        AidRequest saved = aidRequestRepository.save(aidRequest);

        List<AidRequestItem> items = request.getItems().stream()
            .map(aidMapper::toItemEntity)
            .peek(item -> item.setAidRequest(saved))
            .collect(Collectors.toList());

        List<AidRequestItem> savedItems = aidRequestItemRepository.saveAll(items);
        saved.setItems(savedItems);

        BigDecimal lat = saved.getLocation() != null ? BigDecimal.valueOf(saved.getLocation().getY()) : null;
        BigDecimal lng = saved.getLocation() != null ? BigDecimal.valueOf(saved.getLocation().getX()) : null;

        log.info(
            "Creating aid request id={} requesterId={} urgencyLevel={} lat={} lng={}",
            saved.getId(),
            requesterId,
            request.getUrgencyLevel(),
            lat,
            lng
        );
        eventPublisher.publishEvent(new AidRequestCreatedEvent(saved.getId(), lat, lng));

        return aidMapper.toResponse(saved, savedItems, null);
    }
}
