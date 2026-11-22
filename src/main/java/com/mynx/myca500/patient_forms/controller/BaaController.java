package com.mynx.myca500.patient_forms.controller;

import com.mynx.myca500.patient_forms.dto.response.BaaResponse;
import com.mynx.myca500.patient_forms.service.BaaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compliance/baa")
@RequiredArgsConstructor
public class BaaController {

    private final BaaService baaService;

    @GetMapping("/current")
    public BaaResponse getCurrent() {
        return baaService.getCurrent();
    }
}
