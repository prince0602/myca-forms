package com.mynx.myca500.patient_forms.validation;

import com.mynx.myca500.patient_forms.dto.DiagramConfig;
import com.mynx.myca500.patient_forms.dto.DiagramPoint;
import com.mynx.myca500.patient_forms.dto.FieldConfig;
import com.mynx.myca500.patient_forms.dto.FieldOption;
import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import com.mynx.myca500.patient_forms.dto.TableColumn;
import com.mynx.myca500.patient_forms.enums.ColumnType;
import com.mynx.myca500.patient_forms.enums.FieldType;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


public final class CompiledFormSchema {

    private static final Set<FieldType> DISPLAY_ONLY =
            EnumSet.of(FieldType.TAB, FieldType.DOCUMENT, FieldType.FREE_TEXT);

    private final Map<String, FormTemplateValidator.FieldRule> inputs;

    private CompiledFormSchema(Map<String, FormTemplateValidator.FieldRule> inputs) {
        this.inputs = inputs;
    }

    public static CompiledFormSchema compile(List<FormFieldDto> fields) {
        Map<String, FormTemplateValidator.FieldRule> out = new LinkedHashMap<>();
        walk(fields == null ? List.of() : fields, out);
        return new CompiledFormSchema(Collections.unmodifiableMap(out));
    }

    public Collection<FormTemplateValidator.FieldRule> inputs() { return inputs.values(); }

    public boolean isInput(String fieldId) { return inputs.containsKey(fieldId); }

    public int size() { return inputs.size(); }



    static String optionKey(FieldOption o) {
        if (o == null) return null;
        return o.id() != null && !o.id().isBlank() ? o.id() : o.label();
    }

    private static void walk(List<FormFieldDto> fields, Map<String, FormTemplateValidator.FieldRule> out) {
        for (FormFieldDto f : fields) {
            if (!DISPLAY_ONLY.contains(f.type())) out.putIfAbsent(f.id(), toRule(f));
            walk(f.children(), out);
        }
    }

    private static FormTemplateValidator.FieldRule toRule(FormFieldDto f) {
        FieldConfig c = f.config();

        Set<String> optionKeys = c.options().stream()
                .map(CompiledFormSchema::optionKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

        Map<String, ColumnType> columns = c.columns() == null ? Map.of() : c.columns().stream()
                .collect(Collectors.toUnmodifiableMap(TableColumn::id, TableColumn::type, (a, b) -> a));

        DiagramConfig d = c.diagram();
        Set<String> pointIds = d == null ? Set.of() : d.points().stream()
                .map(DiagramPoint::id)
                .collect(Collectors.toUnmodifiableSet());

        return new FormTemplateValidator.FieldRule(
                f.id(),
                f.type(),
                f.required(),
                c.format(),
                c.maxLength(),
                optionKeys,
                columns,
                c.rowLabels() == null ? 0 : c.rowLabels().size(),
                Boolean.TRUE.equals(c.allowAddRows()),
                pointIds);
    }
}
