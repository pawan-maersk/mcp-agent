package mcp.agent.controller;

import mcp.agent.service.OllamaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ollama")
public class OllamaController {

    private final OllamaService ollamaCloudService;

    public OllamaController(OllamaService ollamaCloudService) {
        this.ollamaCloudService = ollamaCloudService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> chat(@RequestBody String prompt) {
        String result = ollamaCloudService.chat("qwen3-coder:480b-cloud", prompt);
        return ResponseEntity.ok(result);
    }
}
