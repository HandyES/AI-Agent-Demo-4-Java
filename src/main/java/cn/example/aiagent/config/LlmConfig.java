package cn.example.aiagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.agent.llm")
public class LlmConfig {

    private String provider = "mock";

    private MockConfig mock = new MockConfig();

    private OllamaConfig ollama = new OllamaConfig();

    private QwenConfig qwen = new QwenConfig();

    private DeepSeekConfig deepseek = new DeepSeekConfig();

    @Data
    public static class MockConfig {
        private boolean enabled = true;
    }

    @Data
    public static class OllamaConfig {
        private boolean enabled = false;
        private String model = "qwen";
        private String baseUrl = "http://localhost:11434";
    }

    @Data
    public static class QwenConfig {
        private boolean enabled = false;
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    }

    @Data
    public static class DeepSeekConfig {
        private boolean enabled = false;
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com/v1";
    }
}
