package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.service.PaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/paper")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    @PostMapping("/upload")
    public Result<Paper> uploadPaper(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "submitterId", required = false) Long submitterId,
            @RequestParam(value = "authorName", required = false) String authorName) {
        Paper paper = paperService.uploadPaper(file, submitterId, authorName);
        return Result.success("论文上传成功", paper);
    }

    @GetMapping("/{id}")
    public Result<Paper> getPaperById(@PathVariable Long id) {
        Paper paper = paperService.getPaperById(id);
        return Result.success(paper);
    }

    @GetMapping("/list")
    public Result<List<Paper>> getPaperList() {
        List<Paper> papers = paperService.getPaperList();
        return Result.success(papers);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deletePaper(@PathVariable Long id) {
        paperService.deletePaper(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/author/{authorId}")
    public Result<List<Paper>> getPapersByAuthor(@PathVariable Long authorId) {
        List<Paper> papers = paperService.getPapersByAuthor(authorId);
        return Result.success(papers);
    }

    @GetMapping("/submitter/{submitterId}")
    public Result<List<Paper>> getPapersBySubmitter(@PathVariable Long submitterId) {
        List<Paper> papers = paperService.getPapersBySubmitter(submitterId);
        return Result.success(papers);
    }
}
