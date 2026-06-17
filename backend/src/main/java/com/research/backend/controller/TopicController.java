package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.dto.TopicDto;
import com.research.backend.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topic")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping("/paper/{paperId}")
    public Result<List<TopicDto>> getTopicsByPaper(@PathVariable Long paperId) {
        List<TopicDto> topics = topicService.getTopicsByPaper(paperId);
        return Result.success(topics);
    }

    @GetMapping("/paper/{paperId}/core")
    public Result<List<TopicDto>> getCoreTopics(@PathVariable Long paperId) {
        List<TopicDto> coreTopics = topicService.getCoreTopics(paperId);
        return Result.success(coreTopics);
    }

    @GetMapping("/paper/{paperId}/keywords")
    public Result<List<String>> getOptimizedKeywords(@PathVariable Long paperId) {
        List<String> keywords = topicService.getPaperKeywordsOptimized(paperId);
        return Result.success(keywords);
    }

    @PostMapping("/paper/{paperId}/extract")
    public Result<List<TopicDto>> extractTopics(@PathVariable Long paperId) {
        List<TopicDto> topics = topicService.extractTopics(paperId);
        return Result.success("主题提取成功", topics);
    }
}
