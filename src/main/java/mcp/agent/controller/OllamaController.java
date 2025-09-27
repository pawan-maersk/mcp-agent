package mcp.agent.controller;

import mcp.agent.service.OllamaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/ollama")
public class OllamaController {

    private static final Logger log = LoggerFactory.getLogger(OllamaController.class);
    private final OllamaService ollamaCloudService;

    public OllamaController(OllamaService ollamaCloudService) {
        this.ollamaCloudService = ollamaCloudService;
    }

//    for threshold
    @PostMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> chat(@RequestBody String prompt) {
        log.info("Chat Message: {}", prompt);
        String result = ollamaCloudService.chat("qwen3-coder:480b-cloud", prompt);
        String jsonArray = extractJsonArray(result);
        return ResponseEntity.ok(jsonArray);
    }

    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        if (start == -1) return "[]";
        int end = findMatchingBracket(text, start);
        if (end == -1) return "[]";
        return text.substring(start, end + 1);
    }

    private int findMatchingBracket(String text, int start) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}
