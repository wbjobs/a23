import re
import math
from collections import Counter, defaultdict


class TopicModeler:
    def __init__(self):
        self.bertopic_available = False
        self.sklearn_available = False

        try:
            import bertopic
            self.bertopic_available = True
        except ImportError:
            self.bertopic_available = False

        try:
            import sklearn
            self.sklearn_available = True
        except ImportError:
            self.sklearn_available = False

    def extract_topics(self, documents, num_topics=5):
        if not documents or len(documents) == 0:
            return []

        if self.bertopic_available:
            return self._extract_topics_bertopic(documents, num_topics)
        elif self.sklearn_available:
            return self._extract_topics_sklearn(documents, num_topics)
        else:
            return self._extract_topics_simple(documents, num_topics)

    def extract_topics_from_text(self, full_text, num_topics=5):
        if not full_text or not full_text.strip():
            return []

        documents = self._split_text_to_documents(full_text)
        return self.extract_topics(documents, num_topics)

    def get_keywords_from_topics(self, topics, top_k=10):
        if not topics:
            return []

        keyword_scores = defaultdict(float)

        for topic in topics:
            topic_words = topic.get('topic_words', [])
            size = topic.get('size', 1)

            for word, score in topic_words:
                keyword_scores[word] += score * (size / 10.0)

        sorted_keywords = sorted(keyword_scores.items(), key=lambda x: x[1], reverse=True)
        return [{'word': word, 'score': score} for word, score in sorted_keywords[:top_k]]

    def _split_text_to_documents(self, full_text):
        paragraphs = re.split(r'\n\s*\n', full_text.strip())
        paragraphs = [p.strip() for p in paragraphs if p.strip() and len(p.strip()) > 20]

        if len(paragraphs) < 5:
            sentences = re.split(r'(?<=[.!?。！？])\s+', full_text.strip())
            sentences = [s.strip() for s in sentences if s.strip() and len(s.strip()) > 10]
            return sentences if sentences else paragraphs

        return paragraphs

    def _extract_topics_bertopic(self, documents, num_topics):
        try:
            from bertopic import BERTopic

            topic_model = BERTopic(nr_topics=num_topics, verbose=False)
            topics, probabilities = topic_model.fit_transform(documents)

            topic_info = topic_model.get_topic_info()
            topic_words_list = []

            for topic_id in range(len(topic_info)):
                if topic_id == -1:
                    continue

                words = topic_model.get_topic(topic_id)
                if words:
                    topic_words = [(word, float(score)) for word, score in words]

                    rep_docs = []
                    try:
                        rep_docs = topic_model.get_representative_docs(topic_id)
                        if rep_docs:
                            rep_docs = list(rep_docs)[:3]
                    except Exception:
                        pass

                    size = len([t for t in topics if t == topic_id])

                    topic_words_list.append({
                        'topic_id': topic_id,
                        'topic_words': topic_words,
                        'representative_docs': rep_docs,
                        'size': size
                    })

            return topic_words_list
        except Exception:
            return self._extract_topics_simple(documents, num_topics)

    def _extract_topics_sklearn(self, documents, num_topics):
        try:
            from sklearn.feature_extraction.text import TfidfVectorizer
            from sklearn.cluster import KMeans
            import numpy as np

            if len(documents) < num_topics:
                num_topics = max(1, len(documents))

            stop_words = self._get_stop_words()

            vectorizer = TfidfVectorizer(
                max_features=1000,
                stop_words=stop_words,
                min_df=1
            )

            tfidf_matrix = vectorizer.fit_transform(documents)
            feature_names = vectorizer.get_feature_names_out()

            n_clusters = min(num_topics, len(documents))
            kmeans = KMeans(n_clusters=n_clusters, random_state=42, n_init=10)
            kmeans.fit(tfidf_matrix)

            clusters = kmeans.labels_.tolist()
            centroids = kmeans.cluster_centers_

            topics = []
            for cluster_id in range(n_clusters):
                centroid = centroids[cluster_id]
                top_indices = centroid.argsort()[-10:][::-1]

                topic_words = []
                for idx in top_indices:
                    if centroid[idx] > 0:
                        topic_words.append((feature_names[idx], float(centroid[idx])))

                cluster_docs = [documents[i] for i, c in enumerate(clusters) if c == cluster_id]
                size = len(cluster_docs)
                rep_docs = cluster_docs[:3] if cluster_docs else []

                if topic_words:
                    topics.append({
                        'topic_id': cluster_id,
                        'topic_words': topic_words,
                        'representative_docs': rep_docs,
                        'size': size
                    })

            topics.sort(key=lambda x: x['size'], reverse=True)
            return topics
        except Exception:
            return self._extract_topics_simple(documents, num_topics)

    def _extract_topics_simple(self, documents, num_topics):
        try:
            stop_words = self._get_stop_words()

            all_words = []
            doc_word_freq = []

            for doc in documents:
                words = self._tokenize(doc)
                words = [w for w in words if w not in stop_words and len(w) > 2]
                all_words.extend(words)
                doc_word_freq.append(Counter(words))

            word_doc_freq = Counter()
            for freq in doc_word_freq:
                for word in freq:
                    word_doc_freq[word] += 1

            total_docs = len(documents)
            word_idf = {}
            for word, df in word_doc_freq.items():
                word_idf[word] = math.log((total_docs + 1) / (df + 1)) + 1

            if len(documents) <= num_topics or len(documents) < 3:
                topic_words = []
                for word, tf in Counter(all_words).most_common(50):
                    if word not in stop_words and len(word) > 2:
                        score = tf * word_idf.get(word, 1)
                        topic_words.append((word, score))

                topic_words = topic_words[:10]
                return [{
                    'topic_id': 0,
                    'topic_words': topic_words,
                    'representative_docs': documents[:3],
                    'size': len(documents)
                }]

            chunk_size = max(1, len(documents) // num_topics)
            topics = []

            for i in range(num_topics):
                start = i * chunk_size
                end = min(start + chunk_size, len(documents))
                chunk_docs = documents[start:end]

                chunk_words = []
                for doc in chunk_docs:
                    words = self._tokenize(doc)
                    words = [w for w in words if w not in stop_words and len(w) > 2]
                    chunk_words.extend(words)

                word_freq = Counter(chunk_words)
                topic_words = []
                for word, tf in word_freq.most_common(20):
                    score = tf * word_idf.get(word, 1)
                    topic_words.append((word, score))

                topic_words.sort(key=lambda x: x[1], reverse=True)
                topic_words = topic_words[:10]

                if topic_words:
                    topics.append({
                        'topic_id': i,
                        'topic_words': topic_words,
                        'representative_docs': chunk_docs[:3],
                        'size': len(chunk_docs)
                    })

            return topics
        except Exception:
            return []

    def _tokenize(self, text):
        text = text.lower()
        words = re.findall(r'[a-zA-Z\u4e00-\u9fa5]+', text)
        return words

    def _get_stop_words(self):
        english_stop = {
            'the', 'a', 'an', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for',
            'of', 'with', 'by', 'from', 'as', 'is', 'are', 'was', 'were', 'be',
            'been', 'being', 'have', 'has', 'had', 'do', 'does', 'did', 'will',
            'would', 'could', 'should', 'may', 'might', 'can', 'shall', 'this',
            'that', 'these', 'those', 'it', 'its', 'they', 'them', 'their', 'we',
            'our', 'you', 'your', 'he', 'she', 'his', 'her', 'not', 'no', 'nor',
            'so', 'if', 'then', 'than', 'too', 'very', 'just', 'also', 'more',
            'most', 'some', 'any', 'each', 'every', 'all', 'both', 'few', 'more',
            'other', 'another', 'such', 'only', 'own', 'same', 'so', 'than',
            'too', 'very', 's', 't', 'd', 'll', 'm', 'o', 're', 've', 'y',
            'about', 'above', 'after', 'again', 'against', 'all', 'am', 'among',
            'around', 'because', 'before', 'between', 'during', 'each', 'few',
            'further', 'here', 'how', 'into', 'through', 'under', 'until', 'up',
            'what', 'when', 'where', 'which', 'who', 'whom', 'why', 'how',
            'using', 'used', 'use', 'method', 'methods', 'based', 'approach',
            'results', 'result', 'figure', 'table', 'section', 'paper', 'study'
        }

        chinese_stop = {
            '的', '是', '在', '了', '和', '与', '及', '或', '等', '中', '为',
            '对', '将', '把', '被', '让', '给', '向', '从', '到', '由',
            '以', '于', '上', '下', '左', '右', '前', '后', '里', '外',
            '这', '那', '些', '个', '一', '二', '三', '四', '五', '六',
            '七', '八', '九', '十', '百', '千', '万', '亿',
            '有', '没', '不', '也', '就', '都', '还', '而', '但', '并',
            '我们', '你们', '他们', '它们', '自己', '这个', '那个',
            '可以', '能够', '应该', '必须', '需要', '可能', '大概',
            '方法', '结果', '如图', '所示', '如下', '以上', '以下',
            '本文', '本研究', '论文', '通过', '基于', '利用', '使用'
        }

        return english_stop.union(chinese_stop)
