package com.anurag.itds.dto;

public record DatasetSourceResponse(
        String name,
        String file,
        boolean imported,
        long records,
        String servedBy,
        String notes) {
}
