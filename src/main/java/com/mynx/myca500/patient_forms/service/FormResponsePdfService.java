package com.mynx.myca500.patient_forms.service;

import com.mynx.myca500.patient_forms.config.PdfProperties;
import com.mynx.myca500.patient_forms.entity.FormResponse;
import com.mynx.myca500.patient_forms.enums.PdfStatus;
import com.mynx.myca500.patient_forms.event.FormSubmittedEvent;
import com.mynx.myca500.patient_forms.exception.ApiException;
import com.mynx.myca500.patient_forms.exception.ResourceNotFoundException;
import com.mynx.myca500.patient_forms.repository.FormResponseRepository;
import com.mynx.myca500.patient_forms.repository.SentFormRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class FormResponsePdfService {

    static final String SUBMITTED = "SUBMITTED";

    private final FormResponseRepository responses;
    private final SentFormRepository sentForms;
    private final FormResponsePdfGenerator generator;
    private final S3PresignedUrlService presigner;
    private final PdfProperties props;
    private final Clock clock;

    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(200),
            daemonThreads("form-pdf-"), new ThreadPoolExecutor.AbortPolicy());
    private final ScheduledExecutorService sweeper =
            Executors.newSingleThreadScheduledExecutor(daemonThreads("form-pdf-sweep-"));

    public record PdfAccess(PdfStatus status, String url) {
        static final PdfAccess NONE = new PdfAccess(null, null);
    }

    public FormResponsePdfService(FormResponseRepository responses, SentFormRepository sentForms,
                                  FormResponsePdfGenerator generator, S3PresignedUrlService presigner,
                                  PdfProperties props, Clock clock) {
        this.responses = responses;
        this.sentForms = sentForms;
        this.generator = generator;
        this.presigner = presigner;
        this.props = props;
        this.clock = clock;
    }

    @PostConstruct
    void startSweeper() {
        long interval = props.sweepInterval().toMillis();
        sweeper.scheduleWithFixedDelay(this::sweep, interval, interval, TimeUnit.MILLISECONDS);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmitted(FormSubmittedEvent event) {
        schedule(event.formResponseId());
    }


    public PdfAccess access(FormResponse response) {
        if (response == null || !SUBMITTED.equals(response.getStatus())) return PdfAccess.NONE;
        String key = response.getPdfKey();
        if (key != null) {
            int slash = key.lastIndexOf('/');
            return new PdfAccess(PdfStatus.COMPLETED,
                    presigner.createPreSignedGetUrl(key.substring(0, slash), key.substring(slash + 1)));
        }
        PdfStatus status = response.getPdfStatus();
        if (status == null || status == PdfStatus.PENDING) {
            schedule(response.getId());
            return new PdfAccess(PdfStatus.PENDING, null);
        }
        return new PdfAccess(status, null);
    }

    public PdfStatus retry(UUID templateId, UUID sentFormId) {
        sentForms.findById(sentFormId)
                .filter(s -> s.getTemplateId().equals(templateId))
                .orElseThrow(() -> new ResourceNotFoundException("Sent form not found: " + sentFormId));
        FormResponse response = responses.findBySentFormId(sentFormId)
                .filter(r -> SUBMITTED.equals(r.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("No submitted response for sent form: " + sentFormId));
        if (responses.resetFailedPdf(response.getId()) == 0) {
            PdfStatus current = response.getPdfKey() != null ? PdfStatus.COMPLETED : response.getPdfStatus();
            throw ApiException.conflict("Only a failed PDF can be retried (current status: " + current + ")");
        }
        log.info("PDF retry requested responseId={}", response.getId());
        schedule(response.getId());
        return PdfStatus.PENDING;
    }

    void schedule(UUID responseId) {
        if (responseId == null || !inFlight.add(responseId)) return;
        try {
            executor.execute(() -> {
                try {
                    run(responseId);
                } finally {
                    inFlight.remove(responseId);
                }
            });
        } catch (RejectedExecutionException e) {

            inFlight.remove(responseId);
            log.warn("PDF render deferred, executor saturated responseId={}", responseId);
        }
    }


    void run(UUID responseId) {
        try {
            if (responses.claimPdf(responseId, clock.instant(), props.maxAttempts()) == 0) return; // done or owned
            FormResponse response = responses.findById(responseId).orElse(null);
            if (response == null) return;
            int attempt = response.getPdfAttempts();
            long started = System.nanoTime();
            try {
                String key = generator.generate(response);
                responses.completePdf(responseId, attempt, key, clock.instant());
                log.info("Form response PDF stored responseId={} attempt={} tookMs={}", responseId, attempt,
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
            } catch (FormResponsePdfGenerator.Failure f) {
                fail(responseId, attempt, f.code(), f);
            } catch (RuntimeException e) {
                fail(responseId, attempt, "UNEXPECTED:" + e.getClass().getSimpleName(), e);
            }
        } catch (RuntimeException e) {

            log.error("PDF job bookkeeping failed responseId={}", responseId, e);
        }
    }

    private void fail(UUID responseId, int attempt, String code, Exception e) {
        Instant now = clock.instant();
        Instant next = nextAttempt(attempt, now);
        responses.failPdf(responseId, attempt, now, code, next);
        log.error("Form response PDF failed responseId={} attempt={}/{} code={} nextAttemptAt={}",
                responseId, attempt, props.maxAttempts(), code, next, e);
    }

    Instant nextAttempt(int attempt, Instant now) {
        if (attempt >= props.maxAttempts()) return null;
        long base = props.retryBackoff().toMillis();
        long delay = Math.min(props.maxRetryBackoff().toMillis(), base << Math.min(Math.max(attempt - 1, 0), 20));
        return now.plusMillis(delay + ThreadLocalRandom.current().nextLong(delay / 10 + 1));
    }


    void sweep() {
        try {
            Instant now = clock.instant();
            int expired = responses.expireStalePdfJobs(now.minus(props.processingTimeout()), now);
            if (expired > 0) log.warn("Recovered {} stuck PDF job(s)", expired);

            int room = Math.min(props.sweepBatchSize(), executor.getQueue().remainingCapacity());
            if (room <= 0) return;
            List<UUID> due = responses.findDuePdfJobs(now, now.minus(props.pendingGrace()), props.maxAttempts(),
                    PageRequest.of(0, room));
            due.forEach(this::schedule);
        } catch (RuntimeException e) {
            log.error("PDF sweep failed", e);
        }
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        sweeper.shutdownNow();
        executor.shutdown();

        if (!executor.awaitTermination(20, TimeUnit.SECONDS)) executor.shutdownNow();
    }

    private static CustomizableThreadFactory daemonThreads(String prefix) {
        CustomizableThreadFactory factory = new CustomizableThreadFactory(prefix);
        factory.setDaemon(true);
        return factory;
    }
}
