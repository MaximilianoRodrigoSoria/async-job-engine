package com.ar.laboratory.asyncjobengine.job.infrastructure.handler;

import com.ar.laboratory.asyncjobengine.job.application.handler.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handler de ejemplo que simula el envío de un email de notificación. En un caso real llamaría a un
 * proveedor SMTP/HTTP; aquí solo registra la operación y devuelve un resultado JSON.
 */
@Slf4j
@Component
public class EmailNotificationJobHandler implements JobHandler {

    @Override
    public String jobType() {
        return "email-notification";
    }

    @Override
    public String handle(String payload) {
        log.info("[email-notification] simulando envío de email con payload: {}", payload);
        return "{\"delivered\":true}";
    }
}
