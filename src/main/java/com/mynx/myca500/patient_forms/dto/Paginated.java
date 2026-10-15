package com.mynx.myca500.patient_forms.dto;

import java.util.List;

public record Paginated<T>(List<T> items, long total) {
}
