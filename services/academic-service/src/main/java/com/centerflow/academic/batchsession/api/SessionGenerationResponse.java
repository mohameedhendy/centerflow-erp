package com.centerflow.academic.batchsession.api;

import com.centerflow.academic.batchsession.application.SessionGenerationResult;

public record SessionGenerationResponse(
        int generatedCount,
        int skippedCount
) {

    public static SessionGenerationResponse from(
            SessionGenerationResult result
    ) {
        return new SessionGenerationResponse(
                result.generatedCount(),
                result.skippedCount()
        );
    }
}