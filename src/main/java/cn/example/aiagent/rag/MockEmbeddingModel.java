package cn.example.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class MockEmbeddingModel {

    private final Random random = new Random();

    public float[] embed(String text) {
        log.debug("Mock Embedding处理单个文本: {}", text);
        return generateRandomEmbedding(384);
    }

    public List<float[]> embed(List<String> texts) {
        log.debug("Mock Embedding处理 {} 个文本", texts.size());
        List<float[]> embeddings = new ArrayList<>();
        
        for (String text : texts) {
            float[] embedding = generateRandomEmbedding(384);
            embeddings.add(embedding);
        }
        
        return embeddings;
    }

    private float[] generateRandomEmbedding(int dimension) {
        float[] embedding = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            embedding[i] = random.nextFloat() * 2 - 1;
        }
        return embedding;
    }
}
