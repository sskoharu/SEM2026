package org.app.services;

import java.util.List;
import java.util.Map;

public record PreExtractionResult(
        Map<String, Object> extractedParameters,
        List<String> irrelevantParameters
) {}
