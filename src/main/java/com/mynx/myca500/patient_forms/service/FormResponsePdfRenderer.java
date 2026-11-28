package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.dto.DiagramConfig;
import com.mynx.myca500.patient_forms.dto.DiagramPoint;
import com.mynx.myca500.patient_forms.dto.DocumentBlock;
import com.mynx.myca500.patient_forms.dto.DocumentFieldRef;
import com.mynx.myca500.patient_forms.dto.DocumentInline;
import com.mynx.myca500.patient_forms.dto.DocumentText;
import com.mynx.myca500.patient_forms.dto.FieldConfig;
import com.mynx.myca500.patient_forms.dto.FieldOption;
import com.mynx.myca500.patient_forms.dto.FormFieldDto;
import com.mynx.myca500.patient_forms.dto.TableColumn;
import com.mynx.myca500.patient_forms.enums.BlockTag;
import com.mynx.myca500.patient_forms.enums.ColumnType;
import com.mynx.myca500.patient_forms.enums.FieldType;
import com.mynx.myca500.patient_forms.enums.InputFormat;
import com.mynx.myca500.patient_forms.enums.ListType;
import com.mynx.myca500.patient_forms.validation.CompiledFormSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class FormResponsePdfRenderer {

    static final String TEMPLATE = "form-response-pdf";
    private static final String EMPTY = "—";
    private static final String BLANK_INLINE = "\u00A0".repeat(12);

    private static final double DIAGRAM_MAX_W_MM = 120;
    private static final double DIAGRAM_MAX_H_MM = 140;
    private static final double PAIRED_DIAGRAM_MAX_W_MM = 80;
    private static final double IMAGE_MAX_W_MM = 120;
    private static final double IMAGE_MAX_H_MM = 90;
    private static final int MAX_ONE_LINE_VALUE = 80;
    private static final String MUTED = "color: #52646d; font-size: 9pt;";

    private static final Pattern INVALID_XML = Pattern.compile("[^\\x09\\x0A\\x0D\\x20-\\uD7FF\\uE000-\\uFFFD]");
    private static final Pattern CAMEL = Pattern.compile("([a-z])([A-Z])");
    private static final Pattern UNSAFE_CSS = Pattern.compile("(?i)url|expression|import|javascript|[;{}<>\\\\@]");
    private static final Pattern SAFE_CSS = Pattern.compile("[#%.,()\\-\\w\\s\"']{1,100}");

    private static final Set<String> CSS_PROPERTIES = Set.of(
            "color", "background-color", "font-size", "font-weight", "font-style", "font-family",
            "text-align", "text-decoration", "text-indent", "text-transform", "line-height", "letter-spacing",
            "vertical-align", "margin", "margin-top", "margin-right", "margin-bottom", "margin-left",
            "padding", "padding-top", "padding-right", "padding-bottom", "padding-left",
            "border", "border-top", "border-right", "border-bottom", "border-left",
            "border-color", "border-width", "border-style", "page-break-before", "page-break-after");
    private static final Set<String> UNITLESS = Set.of("font-weight", "line-height");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a 'UTC'", Locale.US).withZone(ZoneOffset.UTC);

    private final TemplateEngine templateEngine;

    public record Source(String title, String description, String recipient, Instant submittedAt,
                         int templateVersion, List<FormFieldDto> fields, Map<String, Object> answers) {
    }

    public record Attachment(int pages, String problem) {
        public static Attachment included(int pages) { return new Attachment(pages, null); }

        public static Attachment rejected(String problem) { return new Attachment(0, problem); }

        public boolean isIncluded() { return problem == null; }
    }

    public interface Assets {

        String image(String uploadKey);

        BufferedImage diagram(String imageName);

        Attachment attachment(String uploadKey);
    }


    public record Block(String tag, String style, boolean rule, boolean pageBreak, List<Run> runs) {

        static Block of(String tag, String style, List<Run> runs) {
            return new Block(tag, style, false, false, List.copyOf(runs));
        }

        static Block of(Run... runs) { return of("p", null, List.of(runs)); }

        static Block divider() { return new Block("p", null, true, false, List.of()); }

        static Block breakPage() { return new Block("p", null, false, true, List.of()); }
    }

    public record Option(String label, boolean checked) {
    }


    public record Cell(String text, Boolean checked) {
    }


    public record Run(String kind, String text, String style, List<Option> options, String image, String legend,
                      List<String> headers, List<List<Cell>> rows, boolean vertical, List<Run> cells,
                      List<Run> tables, int width, int height) {

        static Run text(String text, String style) {
            return new Run("text", text, style, null, null, null, null, null, false, null, null, 0, 0);
        }

        static Run value(String text, String style) {
            boolean oneLine = text != null && text.length() <= MAX_ONE_LINE_VALUE && text.indexOf('\n') < 0;
            String css = oneLine ? (style == null ? "" : style + " ") + "white-space: nowrap;" : style;
            return new Run("value", text, css, null, null, null, null, null, false, null, null, 0, 0);
        }

        static Run choices(List<Option> options, String style) {
            return new Run("choices", null, style, options, null, null, null, null, false, null, null, 0, 0);
        }

        static Run signature(String image) {
            return new Run("signature", null, null, null, image, null, null, null, false, null, null, 0, 0);
        }

        static Run table(List<String> headers, List<List<Cell>> rows) {
            return new Run("table", null, null, null, null, null, headers, rows, false, null, null, 0, 0);
        }

        static Run diagram(String image, String style, String legend, int width, int height) {
            return new Run("diagram", null, style, null, image, legend, null, null, false, null, null, width, height);
        }

        static Run pair(boolean vertical, List<Run> cells) {
            return new Run("pair", null, null, null, null, null, null, null, vertical, cells, null, 0, 0);
        }

        static Run tables(List<Run> tables, String style) {
            return new Run("tables", null, style, null, null, null, null, null, false, null, tables, 0, 0);
        }

        Run withStyle(String newStyle) {
            return new Run(kind, text, newStyle, options, image, legend, headers, rows, vertical, cells, tables,
                    width, height);
        }

        boolean is(String k) { return kind.equals(k); }

        boolean isBlankText() { return is("text") && (text == null || text.isBlank()); }
    }

    public List<String> render(Source source, Assets assets, OutputStream out) {
        Layout layout = new Layout(source.fields(), source.answers(), assets);
        Context context = new Context(Locale.US);
        context.setVariable("title", clean(source.title()));
        context.setVariable("blocks", layout.blocks(source));

        String xhtml = templateEngine.process(new TemplateSpec(TEMPLATE, TemplateMode.XML), context);

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(xhtml);
        renderer.layout();
        renderer.createPDF(out);
        return List.copyOf(layout.appended);
    }

    private static final class Layout {

        private final List<FormFieldDto> fields;
        private final Map<String, Object> answers;
        private final Assets assets;
        private final Map<String, FormFieldDto> byId = new HashMap<>();
        private final Set<String> inlined = new HashSet<>();
        private final List<String> appended = new ArrayList<>();
        private final List<Block> out = new ArrayList<>();

        Layout(List<FormFieldDto> fields, Map<String, Object> answers, Assets assets) {
            this.fields = fields == null ? List.of() : fields;
            this.answers = answers == null ? Map.of() : answers;
            this.assets = assets;
            index(this.fields);
            for (FormFieldDto f : byId.values()) {
                if (f.type() != FieldType.DOCUMENT || f.config().document() == null) continue;
                for (DocumentBlock b : nz(f.config().document().blocks())) {
                    for (DocumentInline in : nz(b.children())) {
                        if (in instanceof DocumentFieldRef ref && byId.containsKey(ref.field())) inlined.add(ref.field());
                    }
                }
            }
        }

        List<Block> blocks(Source source) {
            header(source);
            int headerEnd = out.size();
            for (FormFieldDto f : fields) {
                if (f.type() != FieldType.TAB) {
                    add(f);
                    continue;
                }
                boolean breakBefore = out.size() > headerEnd;
                if (breakBefore) out.add(Block.breakPage());
                int before = out.size();
                f.children().forEach(this::add);
                if (breakBefore && out.size() == before) out.remove(before - 1);
            }
            return List.copyOf(out);
        }

        private void header(Source source) {
            if (!isBlank(source.title())) out.add(Block.of("h1", null, List.of(Run.text(clean(source.title()), null))));
            if (!isBlank(source.description())) out.add(Block.of(Run.text(clean(source.description()), MUTED)));
            List<String> meta = new ArrayList<>();
            if (!isBlank(source.recipient())) meta.add("Submitted by " + clean(source.recipient()));
            if (source.submittedAt() != null) meta.add(TIMESTAMP.format(source.submittedAt()));
            if (source.templateVersion() > 0) meta.add("Form version " + source.templateVersion());
            if (!meta.isEmpty()) out.add(Block.of(Run.text(String.join(" · ", meta), MUTED)));
            if (!out.isEmpty()) out.add(Block.divider());
        }

        private void index(List<FormFieldDto> list) {
            for (FormFieldDto f : list) {
                byId.putIfAbsent(f.id(), f);
                index(f.children());
            }
        }

        private void add(FormFieldDto f) {
            if (!inlined.contains(f.id())) {
                switch (f.type()) {
                    case TAB -> { }
                    case FREE_TEXT -> {
                        if (!f.config().text().isBlank()) out.add(Block.of(Run.text(clean(f.config().text()), null)));
                    }
                    case DOCUMENT -> document(f);
                    default -> standalone(f);
                }
            }
            f.children().forEach(this::add);
        }

        /** A field outside any document: its label, then its answer, as one block. */
        private void standalone(FormFieldDto f) {
            List<Run> runs = new ArrayList<>();
            if (!f.label().isBlank()) runs.add(Run.text(clean(f.label()) + " ", null));
            List<Run> answer = answer(f, answers.get(f.id()), null);
            boolean blockLevel = answer.stream().anyMatch(r -> r.is("signature") || r.is("table") || r.is("diagram"));
            if (blockLevel && !runs.isEmpty()) runs.set(0, Run.text(clean(f.label()), null));
            runs.addAll(answer);
            out.add(Block.of(runs.toArray(Run[]::new)));
        }

        private void document(FormFieldDto f) {
            if (f.config().document() == null) return;
            int ordinal = 0;
            for (DocumentBlock b : nz(f.config().document().blocks())) {
                BlockTag tag = b.tag() == null ? BlockTag.P : b.tag();
                if (tag == BlockTag.HR) {
                    out.add(Block.divider());
                    ordinal = 0;
                    continue;
                }
                List<Run> runs = new ArrayList<>();
                if (tag == BlockTag.LI) {
                    ordinal = b.list() == ListType.ORDERED ? ordinal + 1 : 0;
                    runs.add(Run.text(ordinal > 0 ? ordinal + ". " : "• ", null));
                } else {
                    ordinal = 0;
                }
                for (DocumentInline in : nz(b.children())) {
                    if (in instanceof DocumentText t && t.text() != null) {
                        runs.add(Run.text(clean(t.text()), css(t.style())));
                    } else if (in instanceof DocumentFieldRef ref) {
                        FormFieldDto target = byId.get(ref.field());
                        String style = css(ref.style());
                        if (target == null) runs.add(Run.value(BLANK_INLINE, style));
                        else runs.addAll(answer(target, answers.get(ref.field()), style));
                    }
                }
                out.add(Block.of(tag.name().toLowerCase(Locale.ROOT), css(b.style()), pair(runs)));
            }
        }


        private List<Run> answer(FormFieldDto f, Object value, String style) {
            return switch (f.type()) {
                case RADIO, CHECKBOX -> List.of(choices(f, value, style));
                case SIGNATURE -> List.of(Run.signature(value instanceof String key ? assets.image(key) : null));
                case TABLE -> List.of(table(f, value, style));
                case BODY_DIAGRAM -> List.of(diagram(f, value, style));
                case INPUT -> f.config().format() == InputFormat.FILE ? List.of(upload(value, style))
                        : List.of(Run.value(text(f, value), style));
                default -> List.of(Run.value(text(f, value), style));
            };
        }

        private Run choices(FormFieldDto f, Object value, String style) {
            if (f.config().options().isEmpty()) return Run.value(text(f, value), style);
            Set<String> picked = value == null ? Set.of()
                    : list(value).stream().map(String::valueOf).collect(Collectors.toSet());
            List<Option> options = new ArrayList<>();
            for (FieldOption o : f.config().options()) {
                options.add(new Option(clean(o.label()), picked.contains(String.valueOf(CompiledFormSchema.optionKey(o)))));
            }
            return Run.choices(options, style);
        }

        private Run table(FormFieldDto f, Object value, String style) {
            FieldConfig c = f.config();
            List<TableColumn> columns = c.columns() == null ? List.of() : c.columns();
            List<String> rowLabels = c.rowLabels() == null ? List.of() : c.rowLabels();
            List<?> rows = value instanceof List<?> l ? l : List.of();
            if (columns.isEmpty() || rows.isEmpty()) return Run.value(EMPTY, style);
            boolean labelled = !rowLabels.isEmpty();
            List<String> headers = new ArrayList<>();
            if (labelled) headers.add("");
            columns.forEach(col -> headers.add(clean(col.label())));

            List<List<Cell>> body = new ArrayList<>(rows.size());
            for (int i = 0; i < rows.size(); i++) {
                Map<?, ?> cells = rows.get(i) instanceof Map<?, ?> m ? m : Map.of();
                List<Cell> row = new ArrayList<>(headers.size());
                if (labelled) row.add(new Cell(i < rowLabels.size() ? clean(rowLabels.get(i)) : "", null));
                for (TableColumn col : columns) {
                    Object v = cells.get(col.id());
                    row.add(col.type() == ColumnType.CHECKBOX ? new Cell(null, truthy(v))
                            : new Cell(v == null ? "" : clean(col.type() == ColumnType.DATE ? date(v) : scalar(v)), null));
                }
                body.add(row);
            }
            return Run.table(headers, body);
        }

        private Run upload(Object value, String style) {
            if (!(value instanceof String key)) return Run.value(EMPTY, style);
            String name = clean(key.substring(key.lastIndexOf('/') + 1));
            Attachment attachment = assets.attachment(key);
            if (attachment != null) {
                if (!attachment.isIncluded()) {
                    return Run.text("Attached file: " + name + ". Not included in this PDF: " + attachment.problem() + ".", style);
                }
                if (!appended.contains(key)) appended.add(key);
                int n = appended.indexOf(key) + 1;
                return Run.text("Attachment " + n + " (" + name + ", " + attachment.pages()
                        + (attachment.pages() == 1 ? " page" : " pages") + ") is appended after the form.", style);
            }
            String dataUri = assets.image(key);
            int[] size = dataUri == null ? null : imageSize(dataUri);
            if (size == null) return Run.text("Attached file: " + name, style);
            return Run.diagram(dataUri, fitStyle(size[0], size[1], IMAGE_MAX_W_MM, IMAGE_MAX_H_MM), null, size[0], size[1]);
        }

        private Run diagram(FormFieldDto f, Object value, String style) {
            DiagramConfig d = f.config().diagram();
            BufferedImage image = value == null || d == null || d.image().isBlank() ? null : assets.diagram(d.image());
            if (image == null) return Run.value(text(f, value), style);
            Map<String, DiagramPoint> points = d.points().stream()
                    .collect(Collectors.toMap(DiagramPoint::id, p -> p, (a, b) -> a));
            List<BodyDiagramPainter.Marker> markers = new ArrayList<>();
            List<String> legend = new ArrayList<>();
            int n = 0;
            for (Object id : list(value)) {
                DiagramPoint p = points.get(String.valueOf(id));
                String name = p == null ? String.valueOf(id) : p.label().isBlank() ? p.id() : p.label();
                n++;
                if (p != null && BodyDiagramPainter.onImage(p.x(), p.y())) {
                    markers.add(new BodyDiagramPainter.Marker(n, p.x(), p.y()));
                    legend.add(n + ". " + name);
                } else {
                    legend.add(n + ". " + name + " (position not shown)");
                }
            }
            String dataUri = png(BodyDiagramPainter.paint(image, markers));
            if (dataUri == null) return Run.value(text(f, value), style);
            return Run.diagram(dataUri, fitStyle(image.getWidth(), image.getHeight()),
                    clean(String.join("\n", legend)), image.getWidth(), image.getHeight());
        }


        private static List<Run> pair(List<Run> runs) {
            List<Run> result = new ArrayList<>(runs.size());
            for (int i = 0; i < runs.size(); i++) {
                Run run = runs.get(i);
                if (!run.is("diagram") && !run.is("table")) {
                    result.add(run);
                    continue;
                }
                List<Run> group = new ArrayList<>(List.of(run));
                int j = i + 1;
                while (j < runs.size()) {
                    int k = j;
                    while (k < runs.size() && runs.get(k).isBlankText()) k++;
                    if (k == runs.size() || !(runs.get(k).is("diagram") || runs.get(k).is("table"))) break;
                    group.add(runs.get(k));
                    j = k + 1;
                }
                List<Run> diagrams = group.stream().filter(r -> r.is("diagram")).toList();
                if (diagrams.size() != 1 || diagrams.size() == group.size()) {
                    result.add(run);
                    continue;
                }
                result.add(pairOf(group, diagrams.get(0)));
                i = j - 1;
            }
            return result;
        }
        private static Run pairOf(List<Run> group, Run diagram) {
            boolean vertical = diagram.width() > diagram.height();
            Run figure = vertical ? diagram : diagram.withStyle(
                    fitStyle(diagram.width(), diagram.height(), PAIRED_DIAGRAM_MAX_W_MM, DIAGRAM_MAX_H_MM));
            Run tables = Run.tables(group.stream().filter(r -> r.is("table")).toList(), null);
            return Run.pair(vertical, group.get(0) == diagram ? List.of(figure, tables) : List.of(tables, figure));
        }

        private String text(FormFieldDto f, Object value) {
            if (value == null || "".equals(value)) return EMPTY;
            return clean(switch (f.type()) {
                case DROPDOWN, RADIO -> optionLabel(f, value);
                case CHECKBOX -> list(value).stream().map(v -> optionLabel(f, v)).collect(Collectors.joining(", "));
                case BODY_DIAGRAM -> pointLabels(f, value);
                case DATE -> date(value);
                default -> scalar(value);
            });
        }

        private static String optionLabel(FormFieldDto f, Object key) {
            for (FieldOption o : f.config().options()) {
                if (Objects.equals(CompiledFormSchema.optionKey(o), key)) return o.label();
            }
            return String.valueOf(key);
        }

        private static String pointLabels(FormFieldDto f, Object value) {
            if (f.config().diagram() == null) return list(value).stream().map(String::valueOf).collect(Collectors.joining(", "));
            Map<String, String> labels = f.config().diagram().points().stream()
                    .collect(Collectors.toMap(DiagramPoint::id, p -> p.label().isBlank() ? p.id() : p.label(), (a, b) -> a));
            return list(value).stream().map(v -> labels.getOrDefault(String.valueOf(v), String.valueOf(v)))
                    .collect(Collectors.joining(", "));
        }
    }

    static String fitStyle(int width, int height) {
        return fitStyle(width, height, DIAGRAM_MAX_W_MM, DIAGRAM_MAX_H_MM);
    }

    static String fitStyle(int width, int height, double maxW, double maxH) {
        double scale = Math.min(maxW / width, maxH / height);
        return String.format(Locale.ROOT, "width: %.1fmm; height: %.1fmm;", width * scale, height * scale);
    }


    static String css(Map<String, Object> style) {
        if (style == null || style.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : style.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            String property = CAMEL.matcher(e.getKey().trim()).replaceAll("$1-$2").toLowerCase(Locale.ROOT);
            if (!CSS_PROPERTIES.contains(property)) continue;
            String value = e.getValue() instanceof Number n && !UNITLESS.contains(property)
                    ? scalar(n) + "px" : scalar(e.getValue()).trim();
            if (!SAFE_CSS.matcher(value).matches() || UNSAFE_CSS.matcher(value).find()) continue;
            sb.append(property).append(": ").append(value).append("; ");
        }
        return sb.isEmpty() ? null : sb.toString().trim();
    }

    static int[] imageSize(String dataUri) {
        int comma = dataUri.indexOf(',');
        if (comma < 0) return null;
        try (ImageInputStream in = ImageIO.createImageInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(dataUri.substring(comma + 1))))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) return null;
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                return w > 0 && h > 0 ? new int[]{w, h} : null;
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    private static String png(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", out)) return null;
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    static String scalar(Object v) {
        if (v instanceof Boolean b) return b ? "Yes" : "No";
        if (v instanceof BigDecimal d) return d.stripTrailingZeros().toPlainString();
        if (v instanceof Double d && d == Math.rint(d) && !d.isInfinite()) return String.valueOf(d.longValue());
        if (v instanceof Collection<?> c) return c.stream().map(FormResponsePdfRenderer::scalar).collect(Collectors.joining(", "));
        return String.valueOf(v);
    }

    private static boolean truthy(Object v) {
        return v instanceof Boolean b ? b : v != null && "true".equalsIgnoreCase(String.valueOf(v));
    }

    private static String date(Object value) {
        try {
            return DATE.format(LocalDate.parse(String.valueOf(value)));
        } catch (DateTimeParseException e) {
            return String.valueOf(value);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static List<?> list(Object v) {
        return v instanceof List<?> l ? l : List.of(v);
    }

    private static <T> List<T> nz(List<T> l) {
        return l == null ? List.of() : l;
    }

    static String clean(String s) {
        return s == null ? null : INVALID_XML.matcher(s).replaceAll("");
    }
}
