package mcp.agent.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfig {

    @Value("${ollama.api.url:https://ollama.com/api}")
    private String ollamaApiUrl;

    @Value("${ollama.api.key:}")
    private String ollamaApiKey;

    @Value("${ollama.model.default:qwen3-coder:480b-cloud}")
    private String defaultModel;

    @Bean
    public OllamaApi ollamaApi() {
        // Remove /api from URL as Spring AI adds it internally
        String baseUrl = ollamaApiUrl.replace("/api", "");
        return new OllamaApi(baseUrl, ollamaApiKey);
    }

    @Bean
    public OllamaChatModel ollamaChatModel(OllamaApi ollamaApi) {
        // Create model options with the default model
        OllamaOptions options = OllamaOptions.builder()
                .withModel(defaultModel)
                .withStream(false)
                .build();

        return new OllamaChatModel(ollamaApi, options);
    }
}
