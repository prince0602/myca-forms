package com.mynx.myca500.patient_forms.validation;

import com.mynx.myca500.patient_forms.dto.AnswerError;
import com.mynx.myca500.patient_forms.enums.ColumnType;
import com.mynx.myca500.patient_forms.exception.FormSubmissionException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


@Component
public class AnswerValidator {

    static final int MAX_TEXT = 2_000;
    static final int MAX_TEXT_AREA = 10_000;
    static final int MAX_TABLE_ROWS = 100;
    static final int MAX_UPLOAD_KEY = 1_024;

    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()\\-]{6,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]{1,64}@[^\\s@]{1,255}\\.[^\\s@]{2,63}$");


    public Map<String, Object> validate(CompiledFormSchema schema, Map<String, Object> answers, String uploadPrefix) {
        return validate(schema, answers, uploadPrefix, true);
    }


    public Map<String, Object> validateDraft(CompiledFormSchema schema, Map<String, Object> answers, String uploadPrefix) {
        return validate(schema, answers, uploadPrefix, false);
    }

    private Map<String, Object> validate(CompiledFormSchema schema, Map<String, Object> answers, String uploadPrefix,
                                         boolean enforceRequired) {
        List<AnswerError> errors = new ArrayList<>();

        for (String id : answers.keySet()) {
            if (!schema.isInput(id)) errors.add(new AnswerError(id, "UNKNOWN_FIELD"));
        }

        Map<String, Object> clean = new LinkedHashMap<>(Math.max(16, schema.size() * 2));
        for (FormTemplateValidator.FieldRule rule : schema.inputs()) {
            Object raw = blankToNull(answers.get(rule.id()));
            if (raw == null) {
                if (enforceRequired && rule.required()) errors.add(new AnswerError(rule.id(), "REQUIRED"));
                continue;
            }
            try {
                clean.put(rule.id(), check(rule, raw, uploadPrefix));
            } catch (InvalidAnswer e) {
                errors.add(new AnswerError(rule.id(), e.getMessage()));
            }
        }

        if (!errors.isEmpty()) throw new FormSubmissionException(errors);
        return clean;
    }

    private Object check(FormTemplateValidator.FieldRule rule, Object raw, String uploadPrefix) {
        return switch (rule.type()) {
            case INPUT -> input(rule, raw, uploadPrefix);
            case DROPDOWN, RADIO -> oneOf(raw, rule.optionKeys(), "INVALID_OPTION");
            case CHECKBOX -> subsetOf(raw, rule.optionKeys(), "INVALID_OPTION");
            case DATE -> date(raw);
            case SCALE -> integer(raw);
            case SIGNATURE -> uploadKey(raw, uploadPrefix);
            case BODY_DIAGRAM -> subsetOf(raw, rule.pointIds(), "INVALID_POINT");
            case TABLE -> table(rule, raw);
            case TAB, DOCUMENT, FREE_TEXT -> throw new InvalidAnswer("NOT_ANSWERABLE");
        };
    }

    private Object input(FormTemplateValidator.FieldRule rule, Object raw, String uploadPrefix) {
        return switch (rule.format()) {
            case NUMBER -> number(raw);
            case PHONE -> matching(raw, PHONE, "INVALID_PHONE");
            case Email -> matching(raw, EMAIL, "INVALID_EMAIL");
            case FILE -> uploadKey(raw, uploadPrefix);
            case TEXT_AREA -> text(raw, limit(rule, MAX_TEXT_AREA));
            case PLAIN -> text(raw, limit(rule, MAX_TEXT));
        };
    }


    private static String text(Object raw, int max) {
        if (!(raw instanceof String s)) throw new InvalidAnswer("INVALID_TYPE");
        if (s.length() > max) throw new InvalidAnswer("TOO_LONG");
        return s;
    }

    private static String matching(Object raw, Pattern pattern, String code) {
        String s = text(raw, 320);
        if (!pattern.matcher(s).matches()) throw new InvalidAnswer(code);
        return s;
    }

    private static Number number(Object raw) {
        if (raw instanceof Number n) return n;
        if (raw instanceof String s && s.length() <= 40) {
            try {
                return new BigDecimal(s);
            } catch (NumberFormatException ignored) {

            }
        }
        throw new InvalidAnswer("INVALID_NUMBER");
    }

    private static long integer(Object raw) {
        if (raw instanceof Integer || raw instanceof Long || raw instanceof Short) return ((Number) raw).longValue();
        if (raw instanceof BigInteger b && b.bitLength() < 64) return b.longValue();
        if (raw instanceof Double d && d == Math.rint(d) && Math.abs(d) < 1e15) return d.longValue();
        if (raw instanceof String s && s.length() <= 19) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {

            }
        }
        throw new InvalidAnswer("INVALID_NUMBER");
    }

    private static String date(Object raw) {
        if (!(raw instanceof String s)) throw new InvalidAnswer("INVALID_DATE");
        try {
            return LocalDate.parse(s).toString();
        } catch (DateTimeParseException e) {
            throw new InvalidAnswer("INVALID_DATE");
        }
    }

    private static boolean bool(Object raw) {
        if (raw instanceof Boolean b) return b;
        throw new InvalidAnswer("INVALID_TYPE");
    }

    private static String oneOf(Object raw, Set<String> allowed, String code) {
        if (!(raw instanceof String s) || !allowed.contains(s)) throw new InvalidAnswer(code);
        return s;
    }

    private static List<String> subsetOf(Object raw, Set<String> allowed, String code) {
        if (!(raw instanceof Collection<?> values)) throw new InvalidAnswer("INVALID_TYPE");
        Set<String> out = new LinkedHashSet<>();
        for (Object v : values) out.add(oneOf(v, allowed, code));
        return List.copyOf(out);
    }

    private static String uploadKey(Object raw, String prefix) {
        if (!(raw instanceof String s)
                || s.length() > MAX_UPLOAD_KEY
                || !s.startsWith(prefix)
                || s.contains("..")) {
            throw new InvalidAnswer("INVALID_UPLOAD");
        }
        return s;
    }

    private static List<Map<String, Object>> table(FormTemplateValidator.FieldRule rule, Object raw) {
        if (!(raw instanceof List<?> rows)) throw new InvalidAnswer("INVALID_TYPE");
        int maxRows = rule.allowAddRows() || rule.fixedRows() == 0 ? MAX_TABLE_ROWS : rule.fixedRows();
        if (rows.size() > maxRows) throw new InvalidAnswer("TOO_MANY_ROWS");

        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> cells)) throw new InvalidAnswer("INVALID_TABLE");
            Map<String, Object> cleanRow = new LinkedHashMap<>();
            for (Map.Entry<?, ?> cell : cells.entrySet()) {
                ColumnType type = rule.columns().get(String.valueOf(cell.getKey()));
                if (type == null) throw new InvalidAnswer("UNKNOWN_COLUMN");
                Object value = blankToNull(cell.getValue());
                if (value != null) cleanRow.put((String) cell.getKey(), cell(type, value));
            }
            out.add(cleanRow);
        }
        return out;
    }

    private static Object cell(ColumnType type, Object value) {
        return switch (type) {
            case TEXT -> text(value, MAX_TEXT);
            case NUMBER -> number(value);
            case DATE -> date(value);
            case SCALE -> integer(value);
            case CHECKBOX -> bool(value);
        };
    }

    private static int limit(FormTemplateValidator.FieldRule rule, int hardMax) {
        return rule.maxLength() == null ? hardMax : Math.min(rule.maxLength(), hardMax);
    }

    private static Object blankToNull(Object v) {
        if (v instanceof String s) {
            String t = s.strip();
            return t.isEmpty() ? null : t;
        }
        if (v instanceof Collection<?> c && c.isEmpty()) return null;
        if (v instanceof Map<?, ?> m && m.isEmpty()) return null;
        return v;
    }

    private static final class InvalidAnswer extends RuntimeException {
        InvalidAnswer(String code) {
            super(code, null, false, false);
        }
    }
}