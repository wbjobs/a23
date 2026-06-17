package com.research.backend.service;

import com.research.backend.dto.TopicDto;

import java.util.List;

public interface TopicService {
    List<TopicDto> extractTopics(Long paperId);
    List<TopicDto> getTopicsByPaper(Long paperId);
    List<TopicDto> getCoreTopics(Long paperId);
    List<String> getPaperKeywordsOptimized(Long paperId);
    List<TopicDto> updateCoreTopicsWithPagerank(Long paperId, List<TopicDto> coreTopics);
}
