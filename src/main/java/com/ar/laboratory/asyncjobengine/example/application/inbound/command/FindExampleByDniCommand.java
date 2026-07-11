package com.ar.laboratory.asyncjobengine.example.application.inbound.command;

import com.ar.laboratory.asyncjobengine.example.domain.model.Example;

/** Puerto de entrada para buscar un Example por DNI */
public interface FindExampleByDniCommand {

    Example execute(String dni);
}
