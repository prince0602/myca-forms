package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.response.OptionResponse;
import com.mynx.myca500.patient_forms.enums.Profession;
import com.mynx.myca500.patient_forms.enums.TeamSize;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ReferenceDataService {

    public List<OptionResponse> getProfessions() {
        return Arrays.stream(Profession.values())
                .map(p -> new OptionResponse(p.name(), p.getLabel()))
                .toList();
    }

    public List<OptionResponse> getTeamSizes() {
        return Arrays.stream(TeamSize.values())
                .map(t -> new OptionResponse(t.name(), t.getLabel()))
                .toList();
    }
}
