package com.ar.laboratory.asyncjobengine.job.domain.model;

/**
 * Prioridad de un {@link Job}. El {@code weight} numérico define el orden de toma por el worker
 * (mayor peso = se procesa antes), permitiendo un {@code ORDER BY priority DESC} en la cola.
 */
public enum Priority {
    LOW(0),
    NORMAL(1),
    HIGH(2);

    private final int weight;

    Priority(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    /** Resuelve la prioridad a partir de su peso persistido; por defecto {@link #NORMAL}. */
    public static Priority fromWeight(int weight) {
        for (Priority p : values()) {
            if (p.weight == weight) {
                return p;
            }
        }
        return NORMAL;
    }
}
