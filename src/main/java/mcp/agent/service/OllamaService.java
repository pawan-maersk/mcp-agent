package mcp.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import mcp.agent.model.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class OllamaService {

    @Value("${ollama.api.key}")
    private String apiKey;

    private static final String API_URL = "https://ollama.com/api/chat";

    public String chat(String model, String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        // Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        body.put("messages", messages);
        body.put("stream", false);

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // Send request
        ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);

        // Extract response content
        Map responseBody = response.getBody();
        if (responseBody != null && responseBody.containsKey("message")) {
            Map message = (Map) responseBody.get("message");
            return (String) message.get("content");
        }
        return "No response";
    }

    public String buildPrompt(List<Tool> tools, String userPrompt) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String toolsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tools);
            return "TOOLS:\n" + toolsJson + "\n\nUSER REQUEST:\n" + userPrompt;
        } catch (Exception e) {
            return "TOOLS: [error serializing tools]\n\nUSER REQUEST:\n" + userPrompt;
        }
    }
}
