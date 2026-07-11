package com.ar.laboratory.asyncjobengine.example.application.inbound.command;

import com.ar.laboratory.asyncjobengine.example.domain.model.Example;

/** Puerto de entrada para crear un Example */
public interface CreateExampleCommand {

    Example execute(Example example);
}
