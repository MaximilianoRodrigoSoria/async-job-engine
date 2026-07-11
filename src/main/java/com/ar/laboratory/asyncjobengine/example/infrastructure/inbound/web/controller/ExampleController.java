package com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.controller;

import com.ar.laboratory.asyncjobengine.example.application.inbound.command.CreateExampleCommand;
import com.ar.laboratory.asyncjobengine.example.application.inbound.command.FindExampleByDniCommand;
import com.ar.laboratory.asyncjobengine.example.application.inbound.command.ListExamplesCommand;
import com.ar.laboratory.asyncjobengine.example.application.query.ExampleFilter;
import com.ar.laboratory.asyncjobengine.example.domain.model.Example;
import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.api.ExampleApi;
import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.dto.CreateExampleRequest;
import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.dto.ExampleResponse;
import com.ar.laboratory.asyncjobengine.example.infrastructure.inbound.web.mapper.ExampleDtoMapper;
import com.ar.laboratory.asyncjobengine.shared.infrastructure.web.dto.PageResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para Examples. Implementa ExampleApi para documentación OpenAPI.
 *
 * <p>Rate limiting configurado en {@code resilience4j.ratelimiter.instances.examples-api}: 50 req/s
 * por defecto. Cuando se supera el límite, Resilience4j lanza {@code RequestNotPermitted} que el
 * {@code GlobalExceptionHandler} convierte en HTTP 429.
 */
@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
@RateLimiter(name = "examples-api")
public class ExampleController implements ExampleApi {

    private final CreateExampleCommand createExampleCommand;
    private final ListExamplesCommand listExamplesCommand;
    private final FindExampleByDniCommand findExampleByDniCommand;
    private final ExampleDtoMapper dtoMapper;

    @PostMapping
    @Override
    public ResponseEntity<ExampleResponse> create(
            @Valid @RequestBody CreateExampleRequest request) {
        Example domain = dtoMapper.toDomain(request);
        Example result = createExampleCommand.execute(domain);
        ExampleResponse response = dtoMapper.toResponse(result);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Override
    public ResponseEntity<PageResponse<ExampleResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String search) {

        ExampleFilter filter = new ExampleFilter(name, dni, search);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));

        Page<Example> examples = listExamplesCommand.execute(filter, pageable);
        Page<ExampleResponse> responsePage = examples.map(dtoMapper::toResponse);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    @GetMapping("/dni/{dni}")
    @Override
    public ResponseEntity<ExampleResponse> findByDni(@PathVariable String dni) {
        Example example = findExampleByDniCommand.execute(dni);
        ExampleResponse response = dtoMapper.toResponse(example);

        return ResponseEntity.ok(response);
    }
}
