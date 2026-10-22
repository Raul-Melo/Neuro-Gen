package br.com.sentinelcorp.Neuro_Gen.shared.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope genérico para listas paginadas. A API nunca retorna um array puro — sempre carrega
 * metadados de paginação, facilitando evolução de cliente.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    public static <T> PagedResponse<T> of(List<T> content) {
        return new PagedResponse<>(content, 0, content.size(), content.size(), 1, true);
    }
}
