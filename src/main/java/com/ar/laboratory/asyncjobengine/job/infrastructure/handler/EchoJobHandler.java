package com.ar.laboratory.asyncjobengine.job.infrastructure.handler;

import com.ar.laboratory.asyncjobengine.job.application.handler.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handler de ejemplo: devuelve el payload recibido como resultado. Útil para pruebas de extremo a
 * extremo del motor sin efectos colaterales.
 */
@Slf4j
@Component
public class EchoJobHandler implements JobHandler {

    @Override
    public String jobType() {
        return "echo";
    }

    @Override
    public String handle(String payload) {
        log.debug("[echo] procesando payload: {}", payload);
        return payload == null ? "{}" : payload;
    }
}
