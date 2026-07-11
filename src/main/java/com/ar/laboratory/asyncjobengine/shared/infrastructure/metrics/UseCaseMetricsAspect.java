package com.ar.laboratory.asyncjobengine.shared.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspecto que registra métricas de ejecución para todos los use cases de la capa de aplicación.
 *
 * <p>Por cada invocación a un método de un {@code Command} se registran:
 *
 * <ul>
 *   <li>{@code usecase.calls} — contador con tags {@code usecase} y {@code status} (success/error)
 *   <li>{@code usecase.execution.seconds} — timer de latencia con los mismos tags
 * </ul>
 *
 * <p>Las métricas son accesibles en {@code /actuator/prometheus} y {@code /actuator/metrics}.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class UseCaseMetricsAspect {

    private static final String METRIC_CALLS = "usecase.calls";
    private static final String METRIC_TIMER = "usecase.execution.seconds";
    private static final String TAG_USECASE = "usecase";
    private static final String TAG_STATUS = "status";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_ERROR = "error";

    private final MeterRegistry meterRegistry;

    /**
     * Intercepta todos los métodos de las interfaces {@code Command} en la capa de aplicación y
     * registra contador + timer por use case.
     */
    @Around(
            "execution(* com.ar.laboratory.asyncjobengine..application.inbound.command..*(..))"
                    + " && !within(com.ar.laboratory.asyncjobengine..infrastructure..*)")
    public Object measureUseCase(ProceedingJoinPoint pjp) throws Throwable {
        String useCaseName = pjp.getTarget().getClass().getSimpleName();
        Timer.Sample sample = Timer.start(meterRegistry);
        String status = STATUS_SUCCESS;

        try {
            return pjp.proceed();
        } catch (Exception ex) {
            status = STATUS_ERROR;
            throw ex;
        } finally {
            String finalStatus = status;
            sample.stop(
                    Timer.builder(METRIC_TIMER)
                            .description("Latencia de ejecución del use case")
                            .tag(TAG_USECASE, useCaseName)
                            .tag(TAG_STATUS, finalStatus)
                            .register(meterRegistry));

            meterRegistry
                    .counter(METRIC_CALLS, TAG_USECASE, useCaseName, TAG_STATUS, finalStatus)
                    .increment();

            log.trace("Use case ejecutado — usecase={}, status={}", useCaseName, finalStatus);
        }
    }
}
