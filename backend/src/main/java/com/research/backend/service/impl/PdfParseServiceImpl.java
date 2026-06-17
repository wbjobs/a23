package com.research.backend.service.impl;

import com.research.backend.entity.PdfParseResult;
import com.research.backend.service.PdfParseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class PdfParseServiceImpl implements PdfParseService {

    @Value("${app.python-service.url:http://localhost:5000}/api/parse")
    private String pythonParseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public PdfParseResult callPythonParsePdf(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource byteArrayResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", byteArrayResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    pythonParseUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return mapToPdfParseResult(response.getBody());
            }
            throw new RuntimeException("PDF解析服务调用失败");
        } catch (IOException e) {
            throw new RuntimeException("读取PDF文件失败: " + e.getMessage());
        } catch (Exception e) {
            PdfParseResult fallback = new PdfParseResult();
            fallback.setTitle(file.getOriginalFilename());
            fallback.setAuthors(new ArrayList<>());
            fallback.setKeywords(new ArrayList<>());
            fallback.setAbstractText("");
            fallback.setFullText("");
            fallback.setMetadata(new HashMap<>());
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    private PdfParseResult mapToPdfParseResult(Map<String, Object> map) {
        PdfParseResult result = new PdfParseResult();
        result.setTitle((String) map.get("title"));
        result.setAbstractText((String) map.get("abstract"));
        Object authors = map.get("authors");
        result.setAuthors(authors != null ? (ArrayList<String>) authors : new ArrayList<>());
        Object keywords = map.get("keywords");
        result.setKeywords(keywords != null ? (ArrayList<String>) keywords : new ArrayList<>());
        result.setFullText((String) map.get("fullText"));
        Object metadata = map.get("metadata");
        result.setMetadata(metadata != null ? (Map<String, Object>) metadata : new HashMap<>());
        return result;
    }
}
