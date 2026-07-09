package cn.example.aiagent.llm;

import cn.example.aiagent.config.LlmConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LlmService {

    @Autowired
    private LlmConfig llmConfig;

    @Autowired
    private MockLlm mockLlm;

    public MockLlm.ChatResponse generateResponse(String promptText) {
        return mockLlm.call(promptText);
    }

    public MockLlm getChatClient() {
        String provider = llmConfig.getProvider();
        log.debug("当前LLM提供商: {}", provider);

        return switch (provider) {
            case "mock" -> mockLlm;
            case "ollama" -> {
                if (llmConfig.getOllama().isEnabled()) {
                    yield createOllamaClient();
                } else {
                    log.warn("Ollama未启用，回退到Mock");
                    yield mockLlm;
                }
            }
            case "qwen" -> {
                if (llmConfig.getQwen().isEnabled()) {
                    yield createQwenClient();
                } else {
                    log.warn("Qwen未启用，回退到Mock");
                    yield mockLlm;
                }
            }
            case "deepseek" -> {
                if (llmConfig.getDeepseek().isEnabled()) {
                    yield createDeepSeekClient();
                } else {
                    log.warn("DeepSeek未启用，回退到Mock");
                    yield mockLlm;
                }
            }
            default -> mockLlm;
        };
    }

    private MockLlm createOllamaClient() {
        log.info("创建Ollama客户端，模型: {}", llmConfig.getOllama().getModel());
        return mockLlm;
    }

    private MockLlm createQwenClient() {
        log.info("创建Qwen客户端");
        return mockLlm;
    }

    private MockLlm createDeepSeekClient() {
        log.info("创建DeepSeek客户端");
        return mockLlm;
    }

    public String getCurrentProvider() {
        return llmConfig.getProvider();
    }

    public void setProvider(String provider) {
        llmConfig.setProvider(provider);
        log.info("切换LLM提供商为: {}", provider);
    }
}
