package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.config.ImageProperties;
import com.mynx.myca500.patient_forms.config.PdfProperties;
import com.mynx.myca500.patient_forms.entity.FormResponse;
import com.mynx.myca500.patient_forms.entity.FormTemplateVersion;
import com.mynx.myca500.patient_forms.entity.Recipient;
import com.mynx.myca500.patient_forms.entity.SentForm;
import com.mynx.myca500.patient_forms.repository.FormTemplateVersionRepository;
import com.mynx.myca500.patient_forms.repository.RecipientRepository;
import com.mynx.myca500.patient_forms.repository.SentFormRepository;
import com.mynx.myca500.patient_forms.service.FormResponsePdfRenderer.Attachment;
import com.mynx.myca500.patient_forms.validation.S3Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;


@Slf4j
@Component
public class FormResponsePdfGenerator {

    static final String PDF_ROOT = "form-responses/";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final int HEADER_BYTES = 1024;
    private static final COSName GOTO = COSName.getPDFName("GoTo");

    private final SentFormRepository sentForms;
    private final FormTemplateVersionRepository versions;
    private final RecipientRepository recipients;
    private final FormResponsePdfRenderer renderer;
    private final S3Client s3Client;
    private final S3PresignedUrlService presigner;
    private final PdfProperties props;
    private final ImageProperties imageProps;

    public FormResponsePdfGenerator(SentFormRepository sentForms, FormTemplateVersionRepository versions,
                                    RecipientRepository recipients, FormResponsePdfRenderer renderer,
                                    S3Client s3Client, S3PresignedUrlService presigner,
                                    PdfProperties props, ImageProperties imageProps) {
        this.sentForms = sentForms;
        this.versions = versions;
        this.recipients = recipients;
        this.renderer = renderer;
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.props = props;
        this.imageProps = imageProps;
    }

    public static final class Failure extends RuntimeException {
        private final String code;

        Failure(String stage, Throwable cause) {
            super(stage, cause);
            this.code = (stage + ":" + cause.getClass().getSimpleName());
        }

        public String code() { return code.length() > 300 ? code.substring(0, 300) : code; }
    }

    static String pdfKey(UUID sentFormId) {
        return PDF_ROOT + sentFormId + "/response.pdf";
    }


    public String generate(FormResponse response) {
        UUID sentFormId = response.getSentFormId();
        String stage = "LOAD";
        Path dir = null;
        try {
            FormResponsePdfRenderer.Source source = source(response);

            stage = "TEMP";
            dir = Files.createTempDirectory("form-pdf-");
            JobAssets assets = new JobAssets(sentFormId, dir);

            stage = "RENDER";
            Path form = dir.resolve("form.pdf");
            List<String> appended = render(source, assets, form);

            Path result = form;
            if (!appended.isEmpty()) {
                stage = "MERGE";
                try {
                    result = merge(form, appended, assets, dir);
                } catch (IOException | RuntimeException e) {

                    log.warn("PDF attachments not merged sentFormId={} reason={}", sentFormId, e.getClass().getSimpleName());
                    assets.disableAttachments();
                    render(source, assets, form);
                }
            }

            stage = "UPLOAD";
            String key = pdfKey(sentFormId);
            s3Client.putObject(b -> b.bucket(presigner.getBucketName()).key(key)
                            .contentType(PDF_CONTENT_TYPE)
                            .contentDisposition("inline; filename=\"form-response.pdf\""),
                    RequestBody.fromFile(result));
            return key;
        } catch (Failure f) {
            throw f;
        } catch (Exception e) {
            throw new Failure(stage, e);
        } finally {
            deleteQuietly(dir);
        }
    }

    private FormResponsePdfRenderer.Source source(FormResponse response) {
        UUID sentFormId = response.getSentFormId();
        SentForm form = sentForms.findById(sentFormId)
                .orElseThrow(() -> new IllegalStateException("Missing sent form " + sentFormId));
        FormTemplateVersion version = versions
                .findByTemplateIdAndVersionNumber(response.getTemplateId(), response.getTemplateVersion())
                .orElseThrow(() -> new IllegalStateException(
                        "Missing template " + response.getTemplateId() + " v" + response.getTemplateVersion()));
        return new FormResponsePdfRenderer.Source(
                version.getLabel(),
                version.getDescription(),
                recipientLabel(form),
                response.getSubmittedAt(),
                response.getTemplateVersion(),
                version.getFields() == null ? List.of() : version.getFields(),
                response.getAnswers());
    }

    private List<String> render(FormResponsePdfRenderer.Source source, JobAssets assets, Path target) throws IOException {
        try (OutputStream out = Files.newOutputStream(target)) {
            return renderer.render(source, assets, out);
        }
    }

    private static Path merge(Path form, List<String> appended, JobAssets assets, Path dir) throws IOException {
        Path merged = dir.resolve("merged.pdf");
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDocumentMergeMode(PDFMergerUtility.DocumentMergeMode.OPTIMIZE_RESOURCES_MODE);
        merger.addSource(form.toFile());
        for (String key : appended) merger.addSource(assets.attachmentFile(key).toFile());
        merger.setDestinationFileName(merged.toString());
        merger.mergeDocuments(IOUtils.createTempFileOnlyStreamCache());
        return merged;
    }

    private String recipientLabel(SentForm form) {
        String name = RecipientService.parseId(form.getRecipientId())
                .flatMap(recipients::findById)
                .map(Recipient::fullName)
                .filter(n -> !n.isBlank())
                .orElse(null);
        String email = form.getRecipientEmail();
        if (name == null) return email;
        return email == null || email.isBlank() ? name : name + " (" + email + ")";
    }
    private final class JobAssets implements FormResponsePdfRenderer.Assets {

        private final UUID sentFormId;
        private final String uploadPrefix;
        private final Path dir;
        private final Map<String, Optional<String>> images = new HashMap<>();
        private final Map<String, Optional<BufferedImage>> diagrams = new HashMap<>();
        private final Map<String, Attachment> attachments = new HashMap<>();
        private final Map<String, Path> attachmentFiles = new HashMap<>();
        private int appendedPages;
        private boolean attachmentsDisabled;

        JobAssets(UUID sentFormId, Path dir) {
            this.sentFormId = sentFormId;
            this.uploadPrefix = FormUploadUrlResolver.uploadPrefix(sentFormId);
            this.dir = dir;
        }

        @Override
        public String image(String uploadKey) {
            return images.computeIfAbsent(uploadKey, k -> Optional.ofNullable(loadImage(k))).orElse(null);
        }

        @Override
        public BufferedImage diagram(String imageName) {
            return diagrams.computeIfAbsent(imageName, n -> Optional.ofNullable(loadDiagram(n))).orElse(null);
        }

        @Override
        public Attachment attachment(String uploadKey) {
            if (!uploadKey.toLowerCase(Locale.ROOT).endsWith(".pdf")) return null;
            Attachment a = attachments.computeIfAbsent(uploadKey, this::prepareAttachment);
            return attachmentsDisabled && a.isIncluded()
                    ? Attachment.rejected("it could not be combined with this document")
                    : a;
        }

        void disableAttachments() { attachmentsDisabled = true; }

        Path attachmentFile(String key) { return attachmentFiles.get(key); }

        private boolean isOwnUpload(String key) {
            return key.startsWith(uploadPrefix) && key.length() > uploadPrefix.length()
                    && key.indexOf('/', uploadPrefix.length()) < 0 && !key.contains("..");
        }

        private String loadImage(String key) {
            if (!isOwnUpload(key)) return null;
            byte[] bytes = readBounded(key, imageProps.maxBytes());
            if (bytes == null) return null;
            String type = TemplateImageUploader.sniff(Arrays.copyOf(bytes, Math.min(bytes.length, 12)));
            // OpenPDF embeds PNG and JPEG only.
            if (!"image/png".equals(type) && !"image/jpeg".equals(type)) return null;
            return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }

        private BufferedImage loadDiagram(String name) {
            if (!S3Constants.isSafeFileName(name)) return null;
            byte[] bytes = readBounded(S3Constants.FORMS_COMMON_FOLDER + "/" + name, imageProps.maxBytes());
            return bytes == null ? null : decode(bytes, props.maxDiagramPixels());
        }

        private Attachment prepareAttachment(String key) {
            if (!isOwnUpload(key)) return Attachment.rejected("it is not a file uploaded with this form");
            Path file = dir.resolve("attachment-" + attachments.size() + ".pdf");
            long max = props.maxAttachmentBytes();
            String tooLarge = String.format(Locale.ROOT, "it is larger than %.0f MB", max / (1024.0 * 1024.0));

            try (ResponseInputStream<GetObjectResponse> in = get(key)) {
                Long length = in.response().contentLength();
                if (length != null && length > max) {
                    in.abort();
                    return Attachment.rejected(tooLarge);
                }
                if (copyBounded(in, file, max) > max) {
                    in.abort();
                    deleteQuietly(file);
                    return Attachment.rejected(tooLarge);
                }
            } catch (NoSuchKeyException e) {
                return Attachment.rejected("the file could not be found");
            } catch (IOException e) {
                throw new Failure("ATTACHMENT_READ", e);
            }

            Attachment verdict = inspect(file);
            if (verdict.isIncluded()) {
                if (appendedPages + verdict.pages() > props.maxAppendedPages()) {
                    verdict = Attachment.rejected("the attachment page limit for this form ("
                            + props.maxAppendedPages() + " pages) was reached");
                } else {
                    appendedPages += verdict.pages();
                    attachmentFiles.put(key, file);
                    return verdict;
                }
            }
            deleteQuietly(file);
            return verdict;
        }
        private Attachment inspect(Path file) {
            if (!hasPdfHeader(file)) return Attachment.rejected("it is not a valid PDF");
            Path clean = dir.resolve(file.getFileName() + ".clean.pdf");
            int pages;
            try (PDDocument doc = Loader.loadPDF(file.toFile(), IOUtils.createTempFileOnlyStreamCache())) {
                if (doc.isEncrypted()) return Attachment.rejected("it is password-protected or encrypted");
                pages = doc.getNumberOfPages();
                if (pages == 0) return Attachment.rejected("it has no pages");
                if (pages > props.maxAttachmentPages()) {
                    return Attachment.rejected("it has more than " + props.maxAttachmentPages() + " pages");
                }
                stripActiveContent(doc);
                doc.save(clean.toFile());
            } catch (InvalidPasswordException e) {
                return Attachment.rejected("it is password-protected or encrypted");
            } catch (IOException | RuntimeException e) {
                deleteQuietly(clean);
                return Attachment.rejected("it is damaged or not a valid PDF");
            }
            try {
                Files.move(clean, file, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new Failure("ATTACHMENT_WRITE", e);
            }
            return Attachment.included(pages);
        }

        private byte[] readBounded(String key, long max) {
            try (ResponseInputStream<GetObjectResponse> in = get(key)) {
                Long length = in.response().contentLength();
                if (length != null && length > max) {
                    in.abort();
                    return null;
                }
                byte[] bytes = in.readNBytes((int) Math.min(max + 1, Integer.MAX_VALUE));
                if (bytes.length > max) {
                    in.abort();
                    return null;
                }
                return bytes;
            } catch (NoSuchKeyException e) {
                log.warn("PDF asset missing sentFormId={}", sentFormId);
                return null;
            } catch (IOException e) {
                throw new Failure("ASSET_READ", e);
            }
        }
        private ResponseInputStream<GetObjectResponse> get(String key) {
            try {
                return s3Client.getObject(b -> b.bucket(presigner.getBucketName()).key(key));
            } catch (NoSuchKeyException e) {
                throw e;
            } catch (SdkException e) {
                throw new Failure("ASSET_READ", e);
            }
        }
    }

    static void stripActiveContent(PDDocument doc) throws IOException {
        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        COSDictionary root = catalog.getCOSObject();
        root.removeItem(COSName.OPEN_ACTION);
        root.removeItem(COSName.AA);
        if (root.getDictionaryObject(COSName.NAMES) instanceof COSDictionary names) {
            names.removeItem(COSName.JAVA_SCRIPT);
            names.removeItem(COSName.EMBEDDED_FILES);
        }
        if (root.getDictionaryObject(COSName.ACRO_FORM) instanceof COSDictionary acroForm) {
            acroForm.removeItem(COSName.XFA);
        }
        for (PDPage page : doc.getPages()) {
            page.getCOSObject().removeItem(COSName.AA);
            for (PDAnnotation annotation : page.getAnnotations()) {
                COSDictionary a = annotation.getCOSObject();
                a.removeItem(COSName.AA);
                COSBase action = a.getDictionaryObject(COSName.A);
                if (action instanceof COSDictionary d && !GOTO.equals(d.getCOSName(COSName.S))) {
                    a.removeItem(COSName.A);
                }
            }
        }
    }

    private static boolean hasPdfHeader(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return new String(in.readNBytes(HEADER_BYTES), StandardCharsets.ISO_8859_1).contains("%PDF-");
        } catch (IOException e) {
            return false;
        }
    }

    static long copyBounded(InputStream in, Path target, long max) throws IOException {
        long total = 0;
        byte[] buffer = new byte[64 * 1024];
        try (OutputStream out = Files.newOutputStream(target)) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > max) return max + 1;
                out.write(buffer, 0, n);
            }
        }
        return total;
    }

    static BufferedImage decode(byte[] bytes, long maxPixels) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) return null; // e.g. WEBP, which the JDK cannot decode
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > maxPixels) return null;
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {

        }
    }
}
