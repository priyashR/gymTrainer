package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.vault.adapters.inbound.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Generic paginated response wrapper matching the platform API standard.
 * Wraps a Spring {@link Page} into the standard shape: content, page, size, totalElements, totalPages.
 *
 * @param <T> the type of items in the response
 */
public record PaginatedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * Creates a {@link PaginatedResponse} from a Spring {@link Page}, applying a mapper
     * to convert each domain element to its response DTO.
     *
     * @param springPage the Spring Page result
     * @param mapper     function to convert domain objects to response DTOs
     * @param <D>        domain type
     * @param <R>        response DTO type
     * @return paginated response with mapped content
     */
    public static <D, R> PaginatedResponse<R> from(Page<D> springPage, Function<D, R> mapper) {
        List<R> content = springPage.getContent().stream()
                .map(mapper)
                .toList();
        return new PaginatedResponse<>(
                content,
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages()
        );
    }
}
