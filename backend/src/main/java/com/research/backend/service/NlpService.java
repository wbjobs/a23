package com.research.backend.service;

import java.util.List;

public interface NlpService {
    String extractInnovation(String abstractText, String fullText);
    List<String> extractMethods(String abstractText, String fullText);
    List<String> extractDatasets(String abstractText, String fullText);
    List<String> extractKeywords(String abstractText, String fullText);
}
