package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.response.OptionResponse;
import com.mynx.myca500.patient_forms.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reference")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/professions")
    public List<OptionResponse> getProfessions() {
        return referenceDataService.getProfessions();
    }

    @GetMapping("/team-sizes")
    public List<OptionResponse> getTeamSizes() {
        return referenceDataService.getTeamSizes();
    }
}
