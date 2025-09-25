package mcp.agent.service;

import mcp.agent.model.ollama.OllamaChatRequest;
import mcp.agent.model.ollama.OllamaChatResponse;
import mcp.agent.model.ollama.OllamaMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class OllamaService {

    private final RestTemplate restTemplate;
    private final String ollamaApiUrl;
    private final String ollamaApiKey;
    private final String defaultModel;

    @Autowired
    public OllamaService(
            RestTemplate restTemplate,
            String ollamaApiUrl,
            String ollamaApiKey,
            @Value("${ollama.model.default}") String defaultModel) {
        this.restTemplate = restTemplate;
        this.ollamaApiUrl = ollamaApiUrl;
        this.ollamaApiKey = ollamaApiKey;
        this.defaultModel = defaultModel;
    }

    /**
     * Send a prompt to the Ollama model and get a response
     *
     * @param prompt The user prompt to send to the model
     * @return The model's response
     */
    public String sendPrompt(String prompt) {
        OllamaMessage message = new OllamaMessage("user", prompt);
        OllamaChatRequest request = new OllamaChatRequest(
            defaultModel,
            List.of(message),
            false
        );

        OllamaChatResponse response = sendChatRequest(request);
        return response.getMessage().getContent();
    }

    private OllamaChatResponse sendChatRequest(OllamaChatRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Add authorization header if API key is provided
        if (ollamaApiKey != null && !ollamaApiKey.isEmpty()) {
            headers.set("Authorization", "Bearer " + ollamaApiKey);
        }

        HttpEntity<OllamaChatRequest> entity = new HttpEntity<>(request, headers);
        String url = ollamaApiUrl + "/chat";

        return restTemplate.postForObject(url, entity, OllamaChatResponse.class);
    }
}
