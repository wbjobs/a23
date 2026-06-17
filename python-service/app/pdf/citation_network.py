import re
from collections import defaultdict


class CitationNetwork:
    def __init__(self):
        self.networkx_available = False
        try:
            import networkx
            self.networkx_available = True
        except ImportError:
            self.networkx_available = False

    def build_network(self, citations, current_paper_title):
        nodes = []
        edges = []

        current_id = 'current_paper'
        nodes.append({
            'id': current_id,
            'title': current_paper_title or 'Current Paper',
            'authors': [],
            'year': ''
        })

        if citations:
            for idx, citation in enumerate(citations):
                cite_id = f'citation_{idx}'
                title = citation.get('title', '')
                authors = citation.get('authors', [])
                year = citation.get('year', '')

                nodes.append({
                    'id': cite_id,
                    'title': title,
                    'authors': authors,
                    'year': year
                })

                edges.append({
                    'source': current_id,
                    'target': cite_id,
                    'type': 'cites'
                })

        return {
            'nodes': nodes,
            'edges': edges
        }

    def compute_pagerank(self, network, damping=0.85, max_iter=100):
        nodes = network.get('nodes', [])
        edges = network.get('edges', [])

        if not nodes:
            return {}

        if self.networkx_available:
            return self._pagerank_networkx(nodes, edges, damping, max_iter)
        else:
            return self._pagerank_manual(nodes, edges, damping, max_iter)

    def get_core_references(self, citations, current_paper_title, top_k=5):
        if not citations:
            return []

        try:
            network = self.build_network(citations, current_paper_title)
            pagerank_scores = self.compute_pagerank(network)

            scored_citations = []
            for idx, citation in enumerate(citations):
                cite_id = f'citation_{idx}'
                score = pagerank_scores.get(cite_id, 0.0)

                title = citation.get('title', '')
                year = citation.get('year', '')
                authors = citation.get('authors', [])

                if year:
                    try:
                        year_int = int(year)
                        recency_bonus = max(0, (2024 - year_int) * 0.01)
                        score += recency_bonus
                    except (ValueError, TypeError):
                        pass

                if authors:
                    score += min(len(authors), 5) * 0.005

                scored_citations.append({
                    'id': idx + 1,
                    'title': title,
                    'authors': authors,
                    'year': year,
                    'pagerank_score': score
                })

            scored_citations.sort(key=lambda x: x['pagerank_score'], reverse=True)
            return scored_citations[:top_k]
        except Exception:
            return citations[:top_k] if citations else []

    def extract_core_topics(self, citations, full_text, num_topics=3):
        topics = []

        try:
            citation_titles = []
            for citation in citations:
                title = citation.get('title', '')
                if title:
                    citation_titles.append(title)

            all_text = ' '.join(citation_titles) + ' ' + (full_text or '')

            words = self._tokenize(all_text)
            stop_words = self._get_stop_words()
            words = [w for w in words if w not in stop_words and len(w) > 2]

            word_freq = defaultdict(int)
            for word in words:
                word_freq[word] += 1

            title_words = []
            for title in citation_titles:
                tw = self._tokenize(title)
                tw = [w for w in tw if w not in stop_words and len(w) > 2]
                title_words.extend(tw)

            title_word_freq = defaultdict(int)
            for word in title_words:
                title_word_freq[word] += 1

            combined_scores = {}
            for word in word_freq:
                freq_score = word_freq[word]
                title_score = title_word_freq.get(word, 0) * 2
                combined_scores[word] = freq_score + title_score

            sorted_words = sorted(combined_scores.items(), key=lambda x: x[1], reverse=True)

            if sorted_words:
                topics_per_topic = max(3, len(sorted_words) // num_topics)
                for i in range(num_topics):
                    start = i * topics_per_topic
                    end = min(start + topics_per_topic, len(sorted_words))
                    topic_words = [(w, float(s)) for w, s in sorted_words[start:end]]

                    if topic_words:
                        topics.append({
                            'topic_id': i,
                            'topic_words': topic_words,
                            'size': len(topic_words)
                        })
        except Exception:
            pass

        return topics

    def _pagerank_networkx(self, nodes, edges, damping, max_iter):
        try:
            import networkx as nx

            G = nx.DiGraph()

            for node in nodes:
                G.add_node(node['id'])

            for edge in edges:
                G.add_edge(edge['source'], edge['target'])

            pagerank = nx.pagerank(G, alpha=damping, max_iter=max_iter)
            return pagerank
        except Exception:
            return self._pagerank_manual(nodes, edges, damping, max_iter)

    def _pagerank_manual(self, nodes, edges, damping, max_iter):
        node_ids = [node['id'] for node in nodes]
        num_nodes = len(node_ids)

        if num_nodes == 0:
            return {}

        node_idx = {nid: i for i, nid in enumerate(node_ids)}

        out_links = defaultdict(list)
        in_links = defaultdict(list)

        for edge in edges:
            src = edge.get('source', '')
            tgt = edge.get('target', '')
            if src in node_idx and tgt in node_idx:
                out_links[src].append(tgt)
                in_links[tgt].append(src)

        pagerank = {nid: 1.0 / num_nodes for nid in node_ids}

        for _ in range(max_iter):
            new_pagerank = {}
            for nid in node_ids:
                rank_sum = 0.0
                for in_node in in_links[nid]:
                    out_count = len(out_links[in_node])
                    if out_count > 0:
                        rank_sum += pagerank[in_node] / out_count

                new_rank = (1 - damping) / num_nodes + damping * rank_sum
                new_pagerank[nid] = new_rank

            diff = sum(abs(new_pagerank[nid] - pagerank[nid]) for nid in node_ids)
            pagerank = new_pagerank

            if diff < 1e-6:
                break

        return pagerank

    def _tokenize(self, text):
        if not text:
            return []
        text = text.lower()
        words = re.findall(r'[a-zA-Z\u4e00-\u9fa5]+', text)
        return words

    def _get_stop_words(self):
        return {
            'the', 'a', 'an', 'and', 'or', 'but', 'in', 'on', 'at', 'to', 'for',
            'of', 'with', 'by', 'from', 'as', 'is', 'are', 'was', 'were', 'be',
            'been', 'being', 'have', 'has', 'had', 'do', 'does', 'did', 'will',
            'would', 'could', 'should', 'may', 'might', 'can', 'shall', 'this',
            'that', 'these', 'those', 'it', 'its', 'they', 'them', 'their', 'we',
            'our', 'you', 'your', 'he', 'she', 'his', 'her', 'not', 'no', 'nor',
            'so', 'if', 'then', 'than', 'too', 'very', 'just', 'also', 'more',
            'most', 'some', 'any', 'each', 'every', 'all', 'both', 'few',
            'other', 'another', 'such', 'only', 'own', 'same',
            'about', 'above', 'after', 'again', 'against',
            'between', 'during', 'through', 'under', 'until',
            'what', 'when', 'where', 'which', 'who', 'whom', 'why', 'how',
            'using', 'used', 'use', 'method', 'methods', 'based', 'approach',
            'results', 'result', 'study', 'studies', 'paper', 'research',
            'analysis', 'model', 'models', 'data', 'system', 'systems',
            '的', '是', '在', '了', '和', '与', '及', '或', '等', '中', '为',
            '对', '将', '把', '被', '让', '给', '向', '从', '到', '由',
            '以', '于', '上', '下', '这', '那', '些', '个',
            '有', '没', '不', '也', '就', '都', '还', '而', '但', '并',
            '我们', '你们', '他们', '它们', '自己',
            '可以', '能够', '应该', '必须', '需要', '可能',
            '方法', '结果', '如图', '所示', '如下', '以上', '以下',
            '本文', '本研究', '论文', '通过', '基于', '利用', '使用'
        }
