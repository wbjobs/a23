package com.research.backend.service.impl;

import com.research.backend.neo4j.entity.Author;
import com.research.backend.neo4j.entity.Dataset;
import com.research.backend.neo4j.entity.Institution;
import com.research.backend.neo4j.entity.Keyword;
import com.research.backend.neo4j.entity.Paper;
import com.research.backend.entity.PdfParseResult;
import com.research.backend.neo4j.repository.AuthorRepository;
import com.research.backend.neo4j.repository.DatasetRepository;
import com.research.backend.neo4j.repository.InstitutionRepository;
import com.research.backend.neo4j.repository.KeywordRepository;
import com.research.backend.neo4j.repository.PaperRepository;
import com.research.backend.service.KnowledgeGraphService;
import com.research.backend.service.NlpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final PaperRepository paperRepository;
    private final AuthorRepository authorRepository;
    private final InstitutionRepository institutionRepository;
    private final KeywordRepository keywordRepository;
    private final DatasetRepository datasetRepository;
    private final NlpService nlpService;

    @Override
    @Transactional
    public void buildPaperGraph(Long paperId, PdfParseResult parseResult) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        String defaultAffiliation = paper.getAuthorName() != null && paper.getAuthorName().contains(" ") ?
                paper.getAuthorName().substring(paper.getAuthorName().lastIndexOf(" ") + 1) : "未知机构";

        List<Author> authors = new ArrayList<>();
        if (parseResult.getAuthors() != null) {
            for (String authorName : parseResult.getAuthors()) {
                String trimmedName = authorName.trim();
                if (trimmedName.isEmpty()) continue;
                Author author = authorRepository.findByName(trimmedName)
                        .orElseGet(() -> {
                            Author a = new Author();
                            a.setName(trimmedName);
                            Institution inst = institutionRepository.findByName(defaultAffiliation)
                                    .orElseGet(() -> {
                                        Institution newInst = new Institution();
                                        newInst.setName(defaultAffiliation);
                                        return institutionRepository.save(newInst);
                                    });
                            a.setInstitution(inst);
                            return authorRepository.save(a);
                        });

                if (author.getInstitution() == null) {
                    Institution inst = institutionRepository.findByName(defaultAffiliation)
                            .orElseGet(() -> {
                                Institution newInst = new Institution();
                                newInst.setName(defaultAffiliation);
                                return institutionRepository.save(newInst);
                            });
                    author.setInstitution(inst);
                    authorRepository.save(author);
                }

                authors.add(author);
            }
        }
        paper.setAuthors(authors);

        List<Keyword> keywords = new ArrayList<>();
        if (parseResult.getKeywords() != null && !parseResult.getKeywords().isEmpty()) {
            for (String keywordStr : parseResult.getKeywords()) {
                String trimmed = keywordStr.trim();
                if (trimmed.isEmpty()) continue;
                Keyword keyword = keywordRepository.findByName(trimmed.toLowerCase())
                        .orElseGet(() -> {
                            Keyword k = new Keyword();
                            k.setName(trimmed.toLowerCase());
                            return keywordRepository.save(k);
                        });
                keywords.add(keyword);
            }
        }
        if (keywords.isEmpty()) {
            List<String> extractedKeywords = nlpService.extractKeywords(
                    parseResult.getAbstractText(), parseResult.getFullText());
            for (String keywordStr : extractedKeywords) {
                String trimmed = keywordStr.trim();
                if (trimmed.isEmpty()) continue;
                Keyword keyword = keywordRepository.findByName(trimmed.toLowerCase())
                        .orElseGet(() -> {
                            Keyword k = new Keyword();
                            k.setName(trimmed.toLowerCase());
                            return keywordRepository.save(k);
                        });
                keywords.add(keyword);
            }
        }
        paper.setKeywords(keywords);

        List<Dataset> datasets = new ArrayList<>();
        List<String> extractedDatasets = nlpService.extractDatasets(
                parseResult.getAbstractText(), parseResult.getFullText());
        for (String datasetName : extractedDatasets) {
            String trimmed = datasetName.trim();
            if (trimmed.isEmpty()) continue;
            Dataset dataset = datasetRepository.findByName(trimmed)
                    .orElseGet(() -> {
                        Dataset d = new Dataset();
                        d.setName(trimmed);
                        return datasetRepository.save(d);
                    });
            datasets.add(dataset);
        }
        paper.setDatasets(datasets);

        String innovation = nlpService.extractInnovation(
                parseResult.getAbstractText(), parseResult.getFullText());
        paper.setInnovation(innovation);

        List<String> methods = nlpService.extractMethods(
                parseResult.getAbstractText(), parseResult.getFullText());
        paper.setMethods(methods);

        paperRepository.save(paper);
    }

    @Override
    public Map<String, List<String>> extractEntities(String text) {
        Map<String, List<String>> entities = new HashMap<>();
        if (text == null || text.isEmpty()) {
            entities.put("keywords", new ArrayList<>());
            entities.put("datasets", new ArrayList<>());
            entities.put("methods", new ArrayList<>());
            return entities;
        }
        entities.put("keywords", nlpService.extractKeywords(text, text));
        entities.put("datasets", nlpService.extractDatasets(text, text));
        entities.put("methods", nlpService.extractMethods(text, text));
        return entities;
    }

    @Override
    public Map<String, Object> getPaperGraph(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));
        Map<String, Object> graph = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        Map<String, Object> paperNode = new HashMap<>();
        paperNode.put("id", "paper_" + paper.getId());
        paperNode.put("label", "Paper");
        paperNode.put("title", paper.getTitle());
        nodes.add(paperNode);

        if (paper.getAuthors() != null) {
            for (Author author : paper.getAuthors()) {
                Map<String, Object> authorNode = new HashMap<>();
                authorNode.put("id", "author_" + author.getId());
                authorNode.put("label", "Author");
                authorNode.put("name", author.getName());
                nodes.add(authorNode);

                Map<String, Object> edge = new HashMap<>();
                edge.put("source", "paper_" + paper.getId());
                edge.put("target", "author_" + author.getId());
                edge.put("relationship", "AUTHORED_BY");
                edges.add(edge);

                if (author.getInstitution() != null) {
                    Map<String, Object> instNode = new HashMap<>();
                    instNode.put("id", "inst_" + author.getInstitution().getId());
                    instNode.put("label", "Institution");
                    instNode.put("name", author.getInstitution().getName());
                    nodes.add(instNode);

                    Map<String, Object> instEdge = new HashMap<>();
                    instEdge.put("source", "author_" + author.getId());
                    instEdge.put("target", "inst_" + author.getInstitution().getId());
                    instEdge.put("relationship", "AFFILIATED_WITH");
                    edges.add(instEdge);
                }
            }
        }

        if (paper.getKeywords() != null) {
            for (Keyword keyword : paper.getKeywords()) {
                Map<String, Object> keywordNode = new HashMap<>();
                keywordNode.put("id", "keyword_" + keyword.getId());
                keywordNode.put("label", "Keyword");
                keywordNode.put("name", keyword.getName());
                nodes.add(keywordNode);

                Map<String, Object> edge = new HashMap<>();
                edge.put("source", "paper_" + paper.getId());
                edge.put("target", "keyword_" + keyword.getId());
                edge.put("relationship", "HAS_KEYWORD");
                edges.add(edge);
            }
        }

        if (paper.getDatasets() != null) {
            for (Dataset dataset : paper.getDatasets()) {
                Map<String, Object> datasetNode = new HashMap<>();
                datasetNode.put("id", "dataset_" + dataset.getId());
                datasetNode.put("label", "Dataset");
                datasetNode.put("name", dataset.getName());
                nodes.add(datasetNode);

                Map<String, Object> edge = new HashMap<>();
                edge.put("source", "paper_" + paper.getId());
                edge.put("target", "dataset_" + dataset.getId());
                edge.put("relationship", "USES_DATASET");
                edges.add(edge);
            }
        }

        graph.put("nodes", nodes);
        graph.put("edges", edges);
        return graph;
    }
}
