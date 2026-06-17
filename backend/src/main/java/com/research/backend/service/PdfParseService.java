package com.research.backend.service;

import com.research.backend.entity.PdfParseResult;
import org.springframework.web.multipart.MultipartFile;

public interface PdfParseService {
    PdfParseResult callPythonParsePdf(MultipartFile file);
}
