package com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.specification;

import com.ar.laboratory.asyncjobengine.example.application.query.ExampleFilter;
import com.ar.laboratory.asyncjobengine.example.infrastructure.outbound.persistence.entity.ExampleEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Especificación JPA dinámica para {@link ExampleEntity}.
 *
 * <p>Construye predicados JPA Criteria a partir de un {@link ExampleFilter}, combinándolos con AND
 * entre campos distintos y OR dentro del campo {@code search}.
 *
 * <pre>
 * Ejemplo de query generado con filter(name="ana", search="123"):
 *   WHERE name ILIKE '%ana%'
 *     AND (name ILIKE '%123%' OR dni ILIKE '%123%')
 * </pre>
 */
public class ExampleSpecification implements Specification<ExampleEntity> {

    private final ExampleFilter filter;

    private ExampleSpecification(ExampleFilter filter) {
        this.filter = filter;
    }

    /**
     * Factory method — crea una especificación a partir del filtro. Si el filtro está vacío,
     * retorna una especificación que no aplica ningún predicado (SELECT * FROM example).
     */
    public static Specification<ExampleEntity> of(ExampleFilter filter) {
        if (filter == null || filter.isEmpty()) {
            // Devuelve un predicado siempre verdadero (SELECT * sin WHERE).
            // Evita Specification.where(null) que es ambiguo en Spring Data 4
            // frente a la nueva sobrecarga where(PredicateSpecification<T>).
            return (root, query, cb) -> cb.conjunction();
        }
        return new ExampleSpecification(filter);
    }

    @Override
    public Predicate toPredicate(
            Root<ExampleEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

        List<Predicate> andPredicates = new ArrayList<>();

        // Filtro por name (ILIKE parcial)
        if (hasValue(filter.name())) {
            andPredicates.add(
                    cb.like(cb.lower(root.get("name")), "%" + filter.name().toLowerCase() + "%"));
        }

        // Filtro por dni (ILIKE parcial)
        if (hasValue(filter.dni())) {
            andPredicates.add(
                    cb.like(cb.lower(root.get("dni")), "%" + filter.dni().toLowerCase() + "%"));
        }

        // search libre: OR sobre name y dni
        if (hasValue(filter.search())) {
            String pattern = "%" + filter.search().toLowerCase() + "%";
            Predicate searchName = cb.like(cb.lower(root.get("name")), pattern);
            Predicate searchDni = cb.like(cb.lower(root.get("dni")), pattern);
            andPredicates.add(cb.or(searchName, searchDni));
        }

        return andPredicates.isEmpty()
                ? cb.conjunction()
                : cb.and(andPredicates.toArray(new Predicate[0]));
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
