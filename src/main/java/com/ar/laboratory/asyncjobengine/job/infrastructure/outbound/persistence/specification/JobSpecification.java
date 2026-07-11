package com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.specification;

import com.ar.laboratory.asyncjobengine.job.application.query.JobFilter;
import com.ar.laboratory.asyncjobengine.job.infrastructure.outbound.persistence.entity.JobEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Especificación JPA dinámica para el listado de {@link JobEntity} a partir de un {@link JobFilter}. */
public final class JobSpecification {

    private JobSpecification() {}

    public static Specification<JobEntity> of(JobFilter filter) {
        if (filter == null || filter.isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status().name()));
            }
            if (filter.jobType() != null && !filter.jobType().isBlank()) {
                predicates.add(cb.equal(root.get("jobType"), filter.jobType()));
            }
            if (filter.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.priority().weight()));
            }
            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
