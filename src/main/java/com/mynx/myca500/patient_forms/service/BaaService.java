package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.response.BaaResponse;
import com.mynx.myca500.patient_forms.entity.BaaDocument;
import com.mynx.myca500.patient_forms.exception.BusinessException;
import com.mynx.myca500.patient_forms.exception.ErrorCode;
import com.mynx.myca500.patient_forms.mapper.BaaMapper;
import com.mynx.myca500.patient_forms.repository.BaaDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaaService {

    private final BaaDocumentRepository baaDocumentRepository;

    public BaaResponse getCurrent() {
        return BaaMapper.toResponse(findCurrent());
    }

    public BaaDocument requireCurrent(String acceptedVersion) {
        BaaDocument current = findCurrent();
        if (!current.getVersion().equals(acceptedVersion)) {
            throw new BusinessException(ErrorCode.BAA_VERSION_STALE);
        }
        return current;
    }

    private BaaDocument findCurrent() {
        return baaDocumentRepository.findFirstByCurrentTrue()
                .orElseThrow(() -> new IllegalStateException("No current BAA document configured"));
    }
}
