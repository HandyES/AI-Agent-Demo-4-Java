package cn.example.aiagent.integration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ApiIntegrationService {

    private final Map<String, ApiClient> registeredClients = new ConcurrentHashMap<>();

    public void registerApiClient(ApiClient client) {
        registeredClients.put(client.getName(), client);
        log.info("注册API客户端: {}", client.getName());
    }

    public ApiIntegrationResult callApi(String apiName, Map<String, Object> parameters) {
        log.info("调用API: {}, 参数: {}", apiName, parameters);

        ApiIntegrationResult result = new ApiIntegrationResult();
        result.setApiName(apiName);
        result.setParameters(parameters);
        result.setTimestamp(LocalDateTime.now());

        ApiClient client = registeredClients.get(apiName);
        if (client == null) {
            result.setSuccess(false);
            result.setError("API客户端不存在: " + apiName);
            return result;
        }

        try {
            Object response = mockApiCall(client, parameters);
            result.setSuccess(true);
            result.setResponse(response);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("调用失败: " + e.getMessage());
            log.error("API调用失败", e);
        }

        return result;
    }

    private Object mockApiCall(ApiClient client, Map<String, Object> parameters) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Mock API响应");
        response.put("data", parameters);
        response.put("api", client.getName());
        return response;
    }

    @Data
    public static class ApiClient {
        private String name;
        private String baseUrl;
        private String apiKey;
        private Map<String, String> headers = new HashMap<>();
    }

    @Data
    public static class ApiIntegrationResult {
        private String apiName;
        private Map<String, Object> parameters;
        private Object response;
        private boolean success;
        private String error;
        private LocalDateTime timestamp;
    }
}
