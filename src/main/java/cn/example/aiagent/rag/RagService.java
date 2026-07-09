package cn.example.aiagent.rag;

import cn.example.aiagent.llm.LlmService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class RagService {

    @Autowired
    private RagConfig ragConfig;

    @Autowired
    private MockEmbeddingModel mockEmbeddingModel;

    @Autowired
    private LlmService llmService;

    private final List<Document> documentStore = new ArrayList<>();

    public RagResult query(String question) {
        String mode = ragConfig.getMode();
        log.info("RAG查询模式: {}, 问题: {}", mode, question);

        return switch (mode) {
            case "basic" -> basicRagQuery(question);
            case "advanced" -> advancedRagQuery(question);
            case "hybrid" -> hybridRagQuery(question);
            case "graph" -> graphRagQuery(question);
            default -> basicRagQuery(question);
        };
    }

    public RagResult query(String question, String mode) {
        ragConfig.setMode(mode);
        return query(question);
    }

    private RagResult basicRagQuery(String question) {
        log.debug("执行基础RAG查询");
        RagResult result = new RagResult();
        result.setMode("basic");
        result.setQuestion(question);

        List<Document> retrievedDocs = retrieveDocuments(question, 3);
        result.setRetrievedDocuments(retrievedDocs);

        String answer = generateAnswer(question, retrievedDocs);
        result.setAnswer(answer);

        return result;
    }

    private RagResult advancedRagQuery(String question) {
        log.debug("执行Advanced RAG查询");
        RagResult result = new RagResult();
        result.setMode("advanced");
        result.setQuestion(question);

        List<Document> retrievedDocs = retrieveDocuments(question, 5);
        retrievedDocs = rerankDocuments(question, retrievedDocs);
        result.setRetrievedDocuments(retrievedDocs);

        String answer = generateAnswer(question, retrievedDocs);
        result.setAnswer(answer);
        result.setReranked(true);

        return result;
    }

    private RagResult hybridRagQuery(String question) {
        log.debug("执行混合检索RAG查询");
        RagResult result = new RagResult();
        result.setMode("hybrid");
        result.setQuestion(question);

        List<Document> semanticDocs = retrieveDocuments(question, 3);
        List<Document> keywordDocs = keywordSearch(question, 3);

        Set<Document> combinedDocs = new LinkedHashSet<>();
        combinedDocs.addAll(semanticDocs);
        combinedDocs.addAll(keywordDocs);

        List<Document> finalDocs = new ArrayList<>(combinedDocs).subList(0, Math.min(5, combinedDocs.size()));
        result.setRetrievedDocuments(finalDocs);
        result.setHybridSearch(true);

        String answer = generateAnswer(question, finalDocs);
        result.setAnswer(answer);

        return result;
    }

    private RagResult graphRagQuery(String question) {
        log.debug("执行GraphRAG查询");
        RagResult result = new RagResult();
        result.setMode("graph");
        result.setQuestion(question);

        List<Document> retrievedDocs = retrieveDocuments(question, 3);
        Map<String, List<String>> entityGraph = buildEntityGraph(retrievedDocs);
        result.setRetrievedDocuments(retrievedDocs);
        result.setEntityGraph(entityGraph);
        result.setGraphRag(true);

        String answer = generateAnswer(question, retrievedDocs);
        result.setAnswer(answer);

        return result;
    }

    private List<Document> retrieveDocuments(String query, int limit) {
        float[] queryEmbedding = mockEmbeddingModel.embed(query);

        List<Document> results = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, documentStore.size()); i++) {
            Document doc = documentStore.get(i);
            float score = calculateSimilarity(queryEmbedding, new float[384]);
            doc.setScore(score);
            results.add(doc);
        }

        results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        return results;
    }

    private List<Document> keywordSearch(String query, int limit) {
        List<Document> results = new ArrayList<>();
        String[] keywords = query.split("\\s+");

        for (Document doc : documentStore) {
            int matches = 0;
            for (String keyword : keywords) {
                if (doc.getContent().toLowerCase().contains(keyword.toLowerCase())) {
                    matches++;
                }
            }
            if (matches > 0) {
                doc.setScore((float) matches / keywords.length);
                results.add(doc);
            }
        }

        results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        return results.subList(0, Math.min(limit, results.size()));
    }

    private List<Document> rerankDocuments(String query, List<Document> documents) {
        documents.sort((a, b) -> {
            int aMatches = countMatches(query, a.getContent());
            int bMatches = countMatches(query, b.getContent());
            return Integer.compare(bMatches, aMatches);
        });
        return documents;
    }

    private int countMatches(String query, String content) {
        int count = 0;
        for (String word : query.split("\\s+")) {
            if (content.toLowerCase().contains(word.toLowerCase())) {
                count++;
            }
        }
        return count;
    }

    private Map<String, List<String>> buildEntityGraph(List<Document> documents) {
        Map<String, List<String>> graph = new HashMap<>();
        
        for (Document doc : documents) {
            String content = doc.getContent();
            List<String> entities = extractEntities(content);
            
            for (int i = 0; i < entities.size(); i++) {
                String entity = entities.get(i);
                graph.computeIfAbsent(entity, k -> new ArrayList<>());
                
                for (int j = 0; j < entities.size(); j++) {
                    if (i != j) {
                        String related = entities.get(j);
                        if (!graph.get(entity).contains(related)) {
                            graph.get(entity).add(related);
                        }
                    }
                }
            }
        }
        
        return graph;
    }

    private List<String> extractEntities(String content) {
        List<String> entities = new ArrayList<>();
        String[] words = content.split("[\\s,.;:!?()]+");
        
        for (String word : words) {
            if (word.length() > 3 && Character.isUpperCase(word.charAt(0))) {
                entities.add(word);
            }
        }
        
        return entities;
    }

    private float calculateSimilarity(float[] a, float[] b) {
        float dotProduct = 0;
        float magA = 0;
        float magB = 0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            magA += a[i] * a[i];
            magB += b[i] * b[i];
        }
        
        return dotProduct / (float) (Math.sqrt(magA) * Math.sqrt(magB));
    }

    private String generateAnswer(String question, List<Document> documents) {
        StringBuilder context = new StringBuilder();
        for (Document doc : documents) {
            context.append(doc.getContent()).append("\n");
        }

        String prompt = String.format("基于以下上下文回答问题：\n\n%s\n\n问题：%s", context, question);
        return llmService.generateResponse(prompt).getResult().getText();
    }

    public void addDocument(Document document) {
        documentStore.add(document);
        log.info("添加文档: {}", document.getId());
    }

    public void addDocuments(List<Document> documents) {
        documentStore.addAll(documents);
        log.info("批量添加 {} 个文档", documents.size());
    }

    public int getDocumentCount() {
        return documentStore.size();
    }

    @Data
    public static class Document {
        private String id;
        private String content;
        private float score;
        
        public Document(String id, String content) {
            this.id = id;
            this.content = content;
        }
    }

    @Data
    public static class RagResult {
        private String mode;
        private String question;
        private String answer;
        private List<Document> retrievedDocuments;
        private boolean reranked = false;
        private boolean hybridSearch = false;
        private boolean graphRag = false;
        private Map<String, List<String>> entityGraph;
        private LocalDateTime timestamp = LocalDateTime.now();
    }
}
