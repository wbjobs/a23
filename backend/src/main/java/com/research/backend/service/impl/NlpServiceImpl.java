package com.research.backend.service.impl;

import com.research.backend.service.NlpService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NlpServiceImpl implements NlpService {

    private static final List<String> INNOVATION_PATTERNS = Arrays.asList(
            "we propose", "we present", "we introduce", "this paper proposes",
            "this paper presents", "this paper introduces", "our contribution",
            "our main contribution", "the key contribution", "novel", "innovative",
            "first time", "new approach", "new method", "new framework",
            "we propose a", "we present a", "we introduce a", "we design",
            "we develop", "we construct", "we build", "propose a novel",
            "本文提出", "本文介绍", "本文设计", "本文构建", "本文开发",
            "提出了一种", "提出了一个", "设计了一种", "设计了一个",
            "主要贡献", "核心贡献", "创新点", "创新性", "新颖的",
            "首次提出", "率先提出", "开创性", "突破性"
    );

    private static final List<String> METHOD_KEYWORDS = Arrays.asList(
            "method", "algorithm", "approach", "technique", "framework", "model",
            "network", "classifier", "regression", "svm", "random forest",
            "neural network", "cnn", "rnn", "lstm", "transformer", "bert",
            "deep learning", "machine learning", "reinforcement learning",
            "gradient descent", "optimization", "clustering", "k-means",
            "decision tree", "bayesian", "markov", "gan", "autoencoder",
            "attention mechanism", "self-attention", "graph neural network",
            "graph convolutional network", "gcn", "gat", "gru", "seq2seq",
            "word2vec", "glove", "fasttext", "elmo", "gpt", "vit",
            "resnet", "inception", "vgg", "alexnet", "unet", "yolo",
            "active learning", "transfer learning", "few-shot learning",
            "zero-shot learning", "contrastive learning", "self-supervised learning",
            "semi-supervised learning", "multi-task learning", "ensemble learning",
            "adversarial learning", "meta-learning", "online learning",
            "方法", "算法", "模型", "网络", "框架", "机制", "策略",
            "深度学习", "机器学习", "强化学习", "神经网络", "卷积神经网络",
            "循环神经网络", "注意力机制", "自注意力", "图神经网络",
            "Transformer", "BERT", "GPT", "生成对抗网络", "自动编码器",
            "迁移学习", "主动学习", "对比学习", "自监督学习", "半监督学习",
            "集成学习", "元学习", "少样本学习", "零样本学习", "聚类",
            "优化", "梯度下降", "随机森林", "支持向量机", "决策树",
            "贝叶斯", "马尔可夫", "隐马尔可夫", "条件随机场", "主题模型",
            "词向量", "词嵌入", "序列到序列", "残差网络", "目标检测"
    );

    private static final List<String> DATASET_PATTERNS = Arrays.asList(
            "dataset", "data set", "corpus", "benchmark", "collection",
            "imagenet", "cifar", "mnist", "squad", "glue", "yelp", "amazon",
            "wikipedia", "pubmed", "arxiv", "twitter", "facebook", "reddit",
            "coco", "pascal voc", "cityscapes", "imagenet", "kinetics",
            "ucf101", "hmdb51", "lsmdc", "ms coco", "flickr30k",
            "ag news", "20 newsgroups", "imdb", "reuters", "trec",
            "conll", "ontonotes", "ace", "semeval", "mit movie review",
            "数据集", "语料库", "基准测试", "评测集", "训练集", "测试集",
            "验证集"
    );

    private static final List<String> TECH_KEYWORDS = Arrays.asList(
            "deep learning", "machine learning", "computer vision", "natural language processing",
            "knowledge graph", "reinforcement learning", "graph neural network", "transformer",
            "generative model", "data mining", "information retrieval", "recommender system",
            "sentiment analysis", "text classification", "named entity recognition",
            "machine translation", "speech recognition", "object detection", "image segmentation",
            "representation learning", "feature extraction", "dimensionality reduction",
            "pattern recognition", "anomaly detection", "predictive modeling",
            "深度学习", "机器学习", "计算机视觉", "自然语言处理", "知识图谱",
            "强化学习", "图神经网络", "生成模型", "数据挖掘", "信息检索",
            "推荐系统", "情感分析", "文本分类", "命名实体识别", "机器翻译",
            "语音识别", "目标检测", "图像分割", "表示学习", "特征提取",
            "降维", "模式识别", "异常检测", "预测建模", "特征工程",
            "语义理解", "知识表示", "多模态学习", "跨模态", "视觉问答",
            "图像生成", "文本生成", "对话系统", "问答系统", "文本摘要"
    );

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "as", "is", "are", "was", "were", "be",
            "been", "being", "have", "has", "had", "do", "does", "did", "will",
            "would", "could", "should", "may", "might", "must", "can", "this",
            "that", "these", "those", "we", "our", "their", "which", "who",
            "what", "when", "where", "why", "how", "paper", "study", "research",
            "experiment", "experimental", "result", "results", "method", "methods",
            "approach", "approaches", "model", "models", "using", "used", "use",
            "also", "both", "each", "than", "then", "such", "not", "no", "yes",
            "more", "most", "less", "least", "very", "much", "many", "some", "any",
            "all", "one", "two", "three", "first", "second", "third", "finally",
            "thus", "therefore", "hence", "however", "moreover", "furthermore",
            "的", "了", "和", "是", "在", "有", "我", "你", "他", "她", "它",
            "们", "这", "那", "就", "都", "而", "及", "与", "或", "等", "但",
            "并", "个", "上", "下", "中", "为", "以", "对", "从", "到", "被",
            "将", "把", "让", "向", "由", "于", "因", "此", "如", "所", "其",
            "他", "她", "它", "各", "每", "该", "本", "某", "每", "各", "一",
            "二", "三", "不", "也", "又", "再", "还", "只", "才", "就", "已",
            "很", "最", "更", "非", "常", "比", "较", "可", "能", "会", "要",
            "应", "该", "需", "须", "当", "应", "可", "即", "则", "若", "如",
            "因", "为", "所", "以", "由", "于", "至", "于", "通", "过", "经",
            "过", "采", "用", "利", "用", "使", "用", "运", "用", "基", "于",
            "根", "据", "按", "照", "依", "据", "针", "对", "面", "向", "结",
            "果", "表", "明", "证", "明", "显", "示", "说", "明", "发", "现",
            "提", "出", "设", "计", "实", "现", "构", "建", "建", "立", "开",
            "发", "研", "究", "分", "析", "讨", "论", "总", "结", "归", "纳",
            "文", "章", "论", "文", "本", "文", "实", "验", "方", "法", "模",
            "型", "算", "法", "数", "据", "结", "果"
    ));

    @Override
    public String extractInnovation(String abstractText, String fullText) {
        String combined = (abstractText != null ? abstractText : "") + " " +
                (fullText != null ? fullText : "");
        String combinedLower = combined.toLowerCase();

        List<String> innovationSentences = new ArrayList<>();
        String[] sentences = combined.split("[.!?。！？]+");
        String[] sentencesLower = combinedLower.split("[.!?。！？]+");

        for (int i = 0; i < sentences.length; i++) {
            String trimmed = sentences[i].trim();
            String trimmedLower = sentencesLower[i].trim();
            for (String pattern : INNOVATION_PATTERNS) {
                if (trimmedLower.contains(pattern.toLowerCase()) && trimmed.length() > 10 && trimmed.length() < 500) {
                    innovationSentences.add(trimmed);
                    break;
                }
            }
        }

        if (innovationSentences.isEmpty() && sentences.length > 0) {
            for (int i = 0; i < sentences.length; i++) {
                String trimmed = sentences[i].trim();
                String trimmedLower = sentencesLower[i].trim();
                if (trimmedLower.contains("propose") || trimmedLower.contains("present") ||
                        trimmedLower.contains("introduce") || trimmedLower.contains("contribution") ||
                        trimmedLower.contains("提出") || trimmedLower.contains("设计") ||
                        trimmedLower.contains("贡献")) {
                    innovationSentences.add(trimmed);
                    break;
                }
            }
        }

        if (innovationSentences.isEmpty()) {
            if (abstractText != null && !abstractText.isEmpty()) {
                return abstractText.length() > 500 ? abstractText.substring(0, 500) + "..." : abstractText;
            }
            return combined.length() > 500 ? combined.substring(0, 500) + "..." : combined;
        }

        return String.join(" ", innovationSentences.stream().limit(3).toList());
    }

    @Override
    public List<String> extractMethods(String abstractText, String fullText) {
        String combined = (abstractText != null ? abstractText : "") + " " +
                (fullText != null ? fullText : "");
        String combinedLower = combined.toLowerCase();

        Set<String> foundMethods = new LinkedHashSet<>();
        for (String keyword : METHOD_KEYWORDS) {
            if (combinedLower.contains(keyword.toLowerCase())) {
                foundMethods.add(keyword);
            }
        }

        Pattern complexMethodPattern = Pattern.compile(
                "(\\w+\\s+(?:method|algorithm|approach|framework|model|network))",
                Pattern.CASE_INSENSITIVE
        );
        Matcher complexMatcher = complexMethodPattern.matcher(combined);
        while (complexMatcher.find() && foundMethods.size() < 20) {
            String method = complexMatcher.group(1).trim().toLowerCase();
            String[] words = method.split("\\s+");
            if (words.length <= 6 && words.length >= 2) {
                boolean hasStopWord = false;
                for (String w : words) {
                    if (STOPWORDS.contains(w.toLowerCase())) {
                        hasStopWord = true;
                        break;
                    }
                }
                if (!hasStopWord) {
                    foundMethods.add(method);
                }
            }
        }

        Pattern chineseMethodPattern = Pattern.compile(
                "([\\u4e00-\\u9fa5]{2,8}(?:方法|算法|模型|网络|框架|机制|策略))"
        );
        Matcher chineseMatcher = chineseMethodPattern.matcher(combined);
        while (chineseMatcher.find() && foundMethods.size() < 25) {
            String method = chineseMatcher.group(1).trim();
            foundMethods.add(method);
        }

        return new ArrayList<>(foundMethods).subList(0, Math.min(15, foundMethods.size()));
    }

    @Override
    public List<String> extractDatasets(String abstractText, String fullText) {
        String combined = (abstractText != null ? abstractText : "") + " " +
                (fullText != null ? fullText : "");
        String combinedLower = combined.toLowerCase();

        Set<String> foundDatasets = new LinkedHashSet<>();
        for (String pattern : DATASET_PATTERNS) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(pattern) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = p.matcher(combinedLower);
            if (matcher.find()) {
                foundDatasets.add(pattern);
            }
        }

        Pattern datasetPattern = Pattern.compile(
                "([A-Z][A-Za-z0-9\\-]+(?:\\s+[Dd]ataset|\\s+[Cc]orpus|\\s+[Bb]enchmark))"
        );
        Matcher datasetMatcher = datasetPattern.matcher(combined);
        while (datasetMatcher.find() && foundDatasets.size() < 20) {
            String dataset = datasetMatcher.group(1).trim();
            String lowerDataset = dataset.toLowerCase();
            boolean exists = false;
            for (String existing : foundDatasets) {
                if (existing.equalsIgnoreCase(lowerDataset)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                foundDatasets.add(dataset);
            }
        }

        Pattern datasetWithNumber = Pattern.compile(
                "\\b([A-Z]+\\-?\\d+[A-Za-z]*)\\b"
        );
        Matcher numberMatcher = datasetWithNumber.matcher(combined);
        while (numberMatcher.find() && foundDatasets.size() < 25) {
            String candidate = numberMatcher.group(1).trim();
            if (candidate.length() >= 3 && candidate.length() <= 15) {
                boolean exists = false;
                for (String existing : foundDatasets) {
                    if (existing.equalsIgnoreCase(candidate)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    foundDatasets.add(candidate);
                }
            }
        }

        return new ArrayList<>(foundDatasets).subList(0, Math.min(10, foundDatasets.size()));
    }

    @Override
    public List<String> extractKeywords(String abstractText, String fullText) {
        String combined = (abstractText != null ? abstractText : "") + " " +
                (fullText != null ? fullText : "");
        String combinedLower = combined.toLowerCase();

        Set<String> foundKeywords = new LinkedHashSet<>();

        for (String tech : TECH_KEYWORDS) {
            if (combinedLower.contains(tech.toLowerCase())) {
                foundKeywords.add(tech);
            }
        }

        for (String method : METHOD_KEYWORDS) {
            if (combinedLower.contains(method.toLowerCase()) && foundKeywords.size() < 30) {
                foundKeywords.add(method);
            }
        }

        String[] words = combined.replaceAll("[^a-zA-Z\\u4e00-\\u9fa5\\s]", " ").split("\\s+");
        Map<String, Integer> wordFreq = new HashMap<>();
        for (String word : words) {
            String lowerWord = word.toLowerCase();
            if (lowerWord.length() > 2 && !STOPWORDS.contains(lowerWord)) {
                boolean isChinese = lowerWord.matches("[\\u4e00-\\u9fa5]+");
                if (isChinese && lowerWord.length() >= 2) {
                    wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
                } else if (!isChinese && lowerWord.length() >= 4) {
                    wordFreq.put(lowerWord, wordFreq.getOrDefault(lowerWord, 0) + 1);
                }
            }
        }

        List<Map.Entry<String, Integer>> sortedWords = new ArrayList<>(wordFreq.entrySet());
        sortedWords.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<String, Integer> entry : sortedWords) {
            if (entry.getValue() >= 2 && foundKeywords.size() < 35) {
                foundKeywords.add(entry.getKey());
            } else {
                break;
            }
        }

        Pattern capitalizedPattern = Pattern.compile("\\b([A-Z][a-z]+(?:\\s+[A-Z][a-z]+){1,3})\\b");
        Matcher capMatcher = capitalizedPattern.matcher(combined);
        while (capMatcher.find() && foundKeywords.size() < 40) {
            String phrase = capMatcher.group(1).toLowerCase();
            String[] phraseWords = phrase.split("\\s+");
            boolean valid = true;
            for (String pw : phraseWords) {
                if (STOPWORDS.contains(pw)) {
                    valid = false;
                    break;
                }
            }
            if (valid && !foundKeywords.contains(phrase)) {
                foundKeywords.add(phrase);
            }
        }

        Pattern chinesePhrasePattern = Pattern.compile("([\\u4e00-\\u9fa5]{2,6})");
        Matcher chineseMatcher = chinesePhrasePattern.matcher(combined);
        Map<String, Integer> chineseFreq = new HashMap<>();
        while (chineseMatcher.find()) {
            String phrase = chineseMatcher.group(1);
            if (!STOPWORDS.contains(phrase)) {
                chineseFreq.put(phrase, chineseFreq.getOrDefault(phrase, 0) + 1);
            }
        }
        List<Map.Entry<String, Integer>> sortedChinese = new ArrayList<>(chineseFreq.entrySet());
        sortedChinese.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        for (Map.Entry<String, Integer> entry : sortedChinese) {
            if (entry.getValue() >= 3 && foundKeywords.size() < 45) {
                foundKeywords.add(entry.getKey());
            } else {
                break;
            }
        }

        return new ArrayList<>(foundKeywords).subList(0, Math.min(20, foundKeywords.size()));
    }
}
