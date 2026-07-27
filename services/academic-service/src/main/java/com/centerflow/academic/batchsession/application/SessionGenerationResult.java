package com.centerflow.academic.batchsession.application;

public record SessionGenerationResult(
        int generatedCount,
        int skippedCount
) {
}