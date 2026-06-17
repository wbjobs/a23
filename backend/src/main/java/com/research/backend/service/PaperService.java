package com.research.backend.service;

import com.research.backend.neo4j.entity.Paper;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PaperService {
    Paper uploadPaper(MultipartFile file, Long submitterId, String authorName);
    Paper getPaperById(Long id);
    List<Paper> getPaperList();
    List<Paper> getPapersByAuthor(Long authorId);
    void deletePaper(Long id);
    List<Paper> getPapersBySubmitter(Long submitterId);
}
