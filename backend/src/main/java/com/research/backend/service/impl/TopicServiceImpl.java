package com.research.backend.service.impl;

import com.research.backend.dto.TopicDto;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.neo4j.entity.Topic;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.neo4j.repository.TopicRepository;
import com.research.backend.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final PaperRepository paperRepository;

    @Override
    @Transactional
    public List<TopicDto> extractTopics(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        List<Topic> existingTopics = topicRepository.findByPaperId(paperId);
        topicRepository.deleteAll(existingTopics);

        List<Topic> savedTopics = new ArrayList<>();
        if (paper.getKeywords() != null && !paper.getKeywords().isEmpty()) {
            int size = paper.getKeywords().size();
            for (int i = 0; i < size; i++) {
                Topic topic = new Topic();
                topic.setName(paper.getKeywords().get(i).getName());
                topic.setWeight(1.0 - (i * 0.05));
                topic.setIsCore(i < 5);
                topic.setSource("BERTOPIC");
                savedTopics.add(topicRepository.save(topic));
            }
        }

        paper.setTopics(savedTopics);
        paperRepository.save(paper);

        return savedTopics.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TopicDto> getTopicsByPaper(Long paperId) {
        List<Topic> topics = topicRepository.findByPaperId(paperId);
        return topics.stream()
                .map(this::convertToDto)
                .sorted(Comparator.comparingDouble(TopicDto::getWeight).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<TopicDto> getCoreTopics(Long paperId) {
        List<Topic> coreTopics = topicRepository.findCoreTopicsByPaperId(paperId);
        if (coreTopics.isEmpty()) {
            coreTopics = topicRepository.findTop10ByPaperIdOrderByWeightDesc(paperId);
        }
        return coreTopics.stream()
                .map(this::convertToDto)
                .sorted(Comparator.comparingDouble(TopicDto::getWeight).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPaperKeywordsOptimized(Long paperId) {
        List<Topic> coreTopics = topicRepository.findCoreTopicsByPaperId(paperId);
        if (coreTopics.isEmpty()) {
            coreTopics = topicRepository.findTop10ByPaperIdOrderByWeightDesc(paperId);
        }
        return coreTopics.stream()
                .map(Topic::getName)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<TopicDto> updateCoreTopicsWithPagerank(Long paperId, List<TopicDto> coreTopicDtos) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        List<Topic> existingTopics = topicRepository.findByPaperId(paperId);

        for (TopicDto dto : coreTopicDtos) {
            for (Topic topic : existingTopics) {
                if (topic.getName().equals(dto.getName())) {
                    topic.setIsCore(true);
                    topic.setSource("COMBINED");
                    if (dto.getWeight() != null) {
                        topic.setWeight(dto.getWeight());
                    }
                    topicRepository.save(topic);
                }
            }
        }

        return getCoreTopics(paperId);
    }

    private TopicDto convertToDto(Topic topic) {
        TopicDto dto = new TopicDto();
        dto.setId(topic.getId());
        dto.setName(topic.getName());
        dto.setWeight(topic.getWeight());
        dto.setIsCore(topic.getIsCore());
        dto.setSource(topic.getSource());
        return dto;
    }
}
