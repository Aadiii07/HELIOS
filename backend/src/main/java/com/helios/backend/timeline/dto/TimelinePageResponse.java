package com.helios.backend.timeline.dto;

import java.util.List;

public record TimelinePageResponse(
        List<TimelineEventResponse> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
}
