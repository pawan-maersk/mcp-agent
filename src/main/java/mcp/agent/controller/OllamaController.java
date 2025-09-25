package mcp.agent.controller;

import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ollama")
public class OllamaController {

    private final OllamaChatModel ollamaChatModel;

    @Autowired
    public OllamaController(OllamaChatModel ollamaChatModel) {
        this.ollamaChatModel = ollamaChatModel;
    }

    /**
     * Send a prompt to the Ollama model and get a plain text response
     */
    @PostMapping(value = "/prompt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> sendPrompt(@RequestBody String prompt) {
        ChatResponse response = ollamaChatModel.call(new UserMessage(prompt));
        return ResponseEntity.ok(response.getResult().getOutput().getContent());
    }
}
