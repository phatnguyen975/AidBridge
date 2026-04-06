package com.drc.aidbridge.modules.shelter.internal.web;

import com.drc.aidbridge.modules.shared.dto.ApiResponse;
import com.drc.aidbridge.modules.shelter.ShelterDTO;
import com.drc.aidbridge.modules.shelter.internal.usecase.GetShelterByIdUseCase;
import com.drc.aidbridge.modules.shelter.ShelterFacade;
import com.drc.aidbridge.modules.shelter.internal.web.dto.CreateShelterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shelters")
@RequiredArgsConstructor
public class ShelterController {

    private final GetShelterByIdUseCase getShelterByIdUseCase;
    private final ShelterFacade shelterFacade;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShelterDTO>> getShelter(@PathVariable UUID id) {
        ShelterDTO dto = getShelterByIdUseCase.execute(id);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.success("Shelter retrieved successfully", dto));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ShelterDTO>>> getActiveShelters() {
        List<ShelterDTO> list = shelterFacade.findActive();
        return ResponseEntity.ok(ApiResponse.success("Active shelters retrieved", list));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShelterDTO>> createShelter(@Valid @RequestBody CreateShelterRequest request) {
        ShelterDTO dto = shelterFacade.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Shelter created", dto));
    }
}
