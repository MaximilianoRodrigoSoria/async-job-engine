package com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.controller;

import com.ar.laboratory.asyncjobengine.job.application.inbound.command.CancelJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.EnqueueJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.GetJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.ListJobsCommand;
import com.ar.laboratory.asyncjobengine.job.application.inbound.command.RetryJobCommand;
import com.ar.laboratory.asyncjobengine.job.application.query.JobFilter;
import com.ar.laboratory.asyncjobengine.job.domain.model.Job;
import com.ar.laboratory.asyncjobengine.job.domain.model.JobStatus;
import com.ar.laboratory.asyncjobengine.job.domain.model.Priority;
import com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.api.JobApi;
import com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.dto.EnqueueJobRequest;
import com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.dto.JobResponse;
import com.ar.laboratory.asyncjobengine.job.infrastructure.inbound.web.mapper.JobDtoMapper;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.exception.BadRequestException;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.web.dto.PageResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST del motor de jobs. Rate limiting via {@code
 * resilience4j.ratelimiter.instances.jobs-api}.
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@RateLimiter(name = "jobs-api")
public class JobController implements JobApi {

    private final EnqueueJobCommand enqueueJobCommand;
    private final GetJobCommand getJobCommand;
    private final ListJobsCommand listJobsCommand;
    private final CancelJobCommand cancelJobCommand;
    private final RetryJobCommand retryJobCommand;
    private final JobDtoMapper mapper;

    @PostMapping
    @Override
    public ResponseEntity<JobResponse> enqueue(
            @Valid @RequestBody EnqueueJobRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Job job = enqueueJobCommand.execute(mapper.toDomain(request, idempotencyKey));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(job));
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<JobResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(getJobCommand.execute(id)));
    }

    @GetMapping
    @Override
    public ResponseEntity<PageResponse<JobResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String priority) {

        JobFilter filter = new JobFilter(parseStatus(status), jobType, parsePriority(priority));
        PageRequest pageable =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<JobResponse> result = listJobsCommand.execute(filter, pageable).map(mapper::toResponse);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @PostMapping("/{id}/cancel")
    @Override
    public ResponseEntity<JobResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(cancelJobCommand.execute(id)));
    }

    @PostMapping("/{id}/retry")
    @Override
    public ResponseEntity<JobResponse> retry(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(retryJobCommand.execute(id)));
    }

    @GetMapping("/dead-letter")
    @Override
    public ResponseEntity<PageResponse<JobResponse>> deadLetter(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        JobFilter filter = new JobFilter(JobStatus.FAILED, null, null);
        PageRequest pageable =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<JobResponse> result = listJobsCommand.execute(filter, pageable).map(mapper::toResponse);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @PostMapping("/dead-letter/{id}/reprocess")
    @Override
    public ResponseEntity<JobResponse> reprocess(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(retryJobCommand.execute(id)));
    }

    // ── helpers de parsing de query params ──────────────────────────────────

    private JobStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return JobStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Estado inválido: " + value);
        }
    }

    private Priority parsePriority(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Priority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Prioridad inválida: " + value);
        }
    }
}
