package com.drc.aidbridge.modules.volunteer.internal.usecase;

import com.drc.aidbridge.modules.volunteer.VolunteerDTO;
import com.drc.aidbridge.modules.volunteer.internal.entity.Volunteer;
import com.drc.aidbridge.modules.volunteer.internal.mapper.VolunteerMapper;
import com.drc.aidbridge.modules.volunteer.internal.repository.VolunteerJpaRepository;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.uber.h3core.H3Core;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindNearbyVolunteersUseCase {

    private final VolunteerJpaRepository volunteerRepository;
    private final VolunteerMapper volunteerMapper;
    private final H3Core h3Core;
    private final GraphHopper graphHopper;

    @Transactional(readOnly = true)
    public List<VolunteerDTO> execute(BigDecimal lat, BigDecimal lng) {
        return execute(lat, lng, 0);
    }

    @Transactional(readOnly = true)
    public List<VolunteerDTO> execute(BigDecimal lat, BigDecimal lng, int retryCount) {
        if (lat == null || lng == null) {
            return List.of();
        }

        try {
            // Map coordinates to resolution 8 H3 cell
            String centerHex = h3Core.latLngToCellAddress(lat.doubleValue(), lng.doubleValue(), 8);
            
            // Adjust search radius based on retry count
            int[] kSteps;
            if (retryCount <= 0) {
                kSteps = new int[]{2, 5};
            } else if (retryCount <= 2) {
                kSteps = new int[]{5, 10};
            } else {
                kSteps = new int[]{10, 20};
            }

            List<Volunteer> candidates = new java.util.ArrayList<>();
            List<String> kRingList = new java.util.ArrayList<>();

            // Expand search grid incrementally until matching targets are found
            for (int k : kSteps) {
                kRingList = h3Core.gridDisk(centerHex, k);
                candidates = volunteerRepository.findByH3Indices(kRingList);
                if (!candidates.isEmpty()) {
                    break;
                }
            }

            // Phase 2: Distance Matrix Routing (ETA Ranking)
            // Using graphhopper to calculate the real route from volunteer to user
            return candidates.stream()
                    .map(volunteerMapper::toDTO)
                    .map(v -> {
                        if (v.getCurrentLocation() == null) return v;
                        GHRequest ghRequest = new GHRequest(
                                v.getCurrentLocation().getLat().doubleValue(),
                                v.getCurrentLocation().getLng().doubleValue(),
                                lat.doubleValue(),
                                lng.doubleValue()
                        );
                        ghRequest.setProfile("urgent_response");
                        try {
                            GHResponse ghResponse = graphHopper.route(ghRequest);
                            if (!ghResponse.hasErrors()) {
                                v.setEtaSeconds(ghResponse.getBest().getTime() / 1000); // ms -> seconds
                            } else {
                                double distKm = haversineKm(
                                        v.getCurrentLocation().getLat().doubleValue(),
                                        v.getCurrentLocation().getLng().doubleValue(),
                                        lat.doubleValue(),
                                        lng.doubleValue());
                                v.setEtaSeconds((long) (distKm / 50.0 * 3600.0)); // fallback 50 km/h
                            }
                        } catch (Exception e) {
                            double distKm = haversineKm(
                                    v.getCurrentLocation().getLat().doubleValue(),
                                    v.getCurrentLocation().getLng().doubleValue(),
                                    lat.doubleValue(),
                                    lng.doubleValue());
                            v.setEtaSeconds((long) (distKm / 50.0 * 3600.0));
                        }
                        return v;
                    })
                    .sorted(Comparator.comparingLong(v -> v.getEtaSeconds() != null ? v.getEtaSeconds() : Long.MAX_VALUE))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
