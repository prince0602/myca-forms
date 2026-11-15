package com.mynx.myca500.patient_forms.helper;

import com.mynx.myca500.patient_forms.dto.DiagramConfig;
import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

@Component
public class FormFieldTraverser {

    public Set<String> collectImages(List<FormFieldDto> fields) {
        Set<String> out = new LinkedHashSet<>();
        walk(fields, out);
        return out;
    }

    private void walk(List<FormFieldDto> fields, Set<String> out) {
        for (FormFieldDto f : fields) {
            String img = imageOf(f);
            if (!img.isBlank()) out.add(img);
            walk(f.children(), out);
        }
    }

    public List<FormFieldDto> mapImages(List<FormFieldDto> fields, UnaryOperator<String> fn) {
        return fields.stream().map(f -> mapField(f, fn)).toList();
    }

    private FormFieldDto mapField(FormFieldDto f, UnaryOperator<String> fn) {
        FormFieldDto cur = f;
        String img = imageOf(f);
        if (!img.isBlank()) {
            DiagramConfig d = f.config().diagram();
            cur = cur.withConfig(f.config().withDiagram(d.withImage(fn.apply(img))));
        }
        if (!f.children().isEmpty()) {
            cur = cur.withChildren(mapImages(f.children(), fn));
        }
        return cur;
    }

    private String imageOf(FormFieldDto f) {
        DiagramConfig d = f.config().diagram();
        return d == null ? "" : d.image();
    }
}