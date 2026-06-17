package com.research.backend.service.impl;

import com.research.backend.neo4j.entity.Paper;
import com.research.backend.entity.PdfParseResult;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.service.KnowledgeGraphService;
import com.research.backend.service.NlpService;
import com.research.backend.service.PaperService;
import com.research.backend.service.PdfParseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {

    @Value("${app.file.upload-path:./uploads/papers}")
    private String uploadPath;

    private final PaperRepository paperRepository;
    private final PdfParseService pdfParseService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final NlpService nlpService;

    @Override
    public Paper uploadPaper(MultipartFile file, Long submitterId, String authorName) {
        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".pdf";
            String storedFilename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadDir.resolve(storedFilename);
            file.transferTo(filePath.toFile());

            PdfParseResult parseResult = pdfParseService.callPythonParsePdf(file);

            Paper paper = new Paper();
            paper.setTitle(parseResult.getTitle() != null ? parseResult.getTitle() : originalFilename);
            paper.setAbstractText(parseResult.getAbstractText());
            paper.setFilePath(filePath.toString());
            paper.setAuthorName(authorName);
            paper.setSubmitterId(submitterId);
            paper.setStatus("PENDING");
            paper.setUploadTime(LocalDateTime.now());
            paper.setUpdateTime(LocalDateTime.now());
            Paper savedPaper = paperRepository.save(paper);

            knowledgeGraphService.buildPaperGraph(savedPaper.getId(), parseResult);

            return savedPaper;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public Paper getPaperById(Long id) {
        return paperRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("论文不存在"));
    }

    @Override
    public List<Paper> getPaperList() {
        return paperRepository.findAll();
    }

    @Override
    public List<Paper> getPapersByAuthor(Long authorId) {
        return paperRepository.findPapersByAuthorId(authorId);
    }

    @Override
    public void deletePaper(Long id) {
        Paper paper = getPaperById(id);
        if (paper.getFilePath() != null) {
            try {
                Path filePath = Paths.get(paper.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) {
            }
        }
        paperRepository.deleteById(id);
    }

    @Override
    public List<Paper> getPapersBySubmitter(Long submitterId) {
        return paperRepository.findBySubmitterId(submitterId);
    }
}
