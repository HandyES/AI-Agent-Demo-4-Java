package cn.example.aiagent.llm;

import cn.example.aiagent.config.LlmConfig;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
public class MockLlm {

    private final Random random = new Random();

    private final Map<String, String> mockResponses = new HashMap<>();

    @Autowired
    private LlmConfig llmConfig;

    public MockLlm() {
        mockResponses.put("你好", "你好！我是AI Agent，很高兴为你服务。");
        mockResponses.put("今天天气怎么样", "今天天气晴朗，气温25度，适合户外活动。");
        mockResponses.put("什么是人工智能", "人工智能是计算机科学的一个分支，致力于研究、开发用于模拟、延伸和扩展人的智能的理论、方法、技术及应用系统。");
        mockResponses.put("谢谢", "不客气，有任何问题随时问我！");
    }

    public ChatResponse call(String promptText) {
        log.debug("Mock LLM 接收到请求: {}", promptText);

        String responseText = generateMockResponse(promptText);
        log.debug("Mock LLM 生成响应: {}", responseText);

        Generation generation = new Generation(responseText);
        generation.setMetadata(Map.of(
                "provider", "mock",
                "timestamp", LocalDateTime.now().toString(),
                "inputTokens", promptText.length(),
                "outputTokens", responseText.length()
        ));

        return new ChatResponse(List.of(generation));
    }

    private String generateMockResponse(String input) {
        if (mockResponses.containsKey(input)) {
            return mockResponses.get(input);
        }

        String[] patterns = {
                "这是一个关于\"%s\"的问题。根据我的知识，%s",
                "您提出了一个很好的问题。关于\"%s\"，我可以告诉您：%s",
                "让我来回答您关于\"%s\"的问题：%s"
        };

        String[] answers = {
                "这是一个复杂的话题，涉及多个方面的知识。",
                "这个问题需要进一步的研究和分析。",
                "相关的信息非常丰富，我可以为您提供更多细节。",
                "根据现有资料，这是一个值得深入探讨的问题。"
        };

        String pattern = patterns[random.nextInt(patterns.length)];
        String answer = answers[random.nextInt(answers.length)];

        return String.format(pattern, input, answer);
    }

    public String getProviderName() {
        return "mock";
    }

    @Data
    public static class ChatResponse {
        private List<Generation> generations;
        
        public ChatResponse(List<Generation> generations) {
            this.generations = generations;
        }
        
        public Generation getResult() {
            return generations.isEmpty() ? null : generations.get(0);
        }
    }

    @Data
    public static class Generation {
        private String text;
        private Map<String, Object> metadata;
        
        public Generation(String text) {
            this.text = text;
        }
    }
}
