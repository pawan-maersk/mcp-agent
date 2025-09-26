package mcp.agent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import mcp.agent.model.Tool;
import io.modelcontextprotocol.client.McpSyncClient;
import mcp.agent.service.OllamaService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final List<Tool> availableTools;
    private final List<McpSyncClient> mcpClients;
    private final OllamaService ollamaService;

    public McpController(@Qualifier("availableTools") List<Tool> availableTools,
                         List<McpSyncClient> mcpClients, OllamaService ollamaService) {
        this.availableTools = availableTools;
        this.mcpClients = mcpClients;
        this.ollamaService = ollamaService;
    }

    /**
     * Diagnostic endpoint to check MCP client status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("clientCount", mcpClients.size());
        status.put("toolCount", availableTools.size());
        return ResponseEntity.ok(status);
    }

    /**
     * Get all available MCP tools
     */
    @GetMapping("/tools")
    public ResponseEntity<List<Tool>> getAvailableTools() {
        return ResponseEntity.ok(availableTools);
    }

    /**
     * Trigger a tool by name with dynamic parameters, returns plain text
     */
    @PostMapping(value = "/tool/trigger", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> triggerTool(
            @RequestParam String toolName,
            @RequestBody Map<String, Object> input) {
        if (mcpClients.isEmpty()) {
            return ResponseEntity.status(500).body("No MCP client configured.");
        }
        McpSyncClient mcpSyncClient = mcpClients.get(0);
        boolean toolExists = mcpSyncClient.listTools().tools().stream()
            .anyMatch(tool -> tool.name().equals(toolName));
        if (!toolExists) {
            return ResponseEntity.badRequest().body("Tool not found: " + toolName);
        }
        Object result = mcpSyncClient.callTool(new McpSchema.CallToolRequest(toolName, input));
        String resultText = (result == null) ? "No result returned." : result.toString();
        return ResponseEntity.ok(resultText);
    }

    @PostMapping(value = "/ask", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> ask(@RequestBody String userPrompt) {
        String parseResult = ollamaService.buildPrompt(availableTools, userPrompt);
        String llmResponse = ollamaService.chat("qwen3-coder:480b-cloud", parseResult);

        try {
            String json = extractFirstJsonObject(llmResponse);
            if (json == null) {
                return ResponseEntity.status(500).body("LLM response did not contain a valid JSON object.");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            String toolName = root.get("name").asText();
            Map<String, Object> arguments = mapper.convertValue(root.get("arguments"), Map.class);

            McpSyncClient mcpSyncClient = mcpClients.get(0);
            Object toolResult = mcpSyncClient.callTool(new McpSchema.CallToolRequest(toolName, arguments));
            String resultText = (toolResult == null) ? "No result returned." : toolResult.toString();

            // Build summarization prompt and call LLM again
            String summarizationPrompt = buildSummarizationPrompt(userPrompt, resultText);
            String summary = ollamaService.chat("qwen3-coder:480b-cloud", summarizationPrompt);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to parse LLM response or call tool: " + e.getMessage());
        }
    }

    // Helper to extract the first JSON object from a string
    private String extractFirstJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    // Helper to build a summarization prompt for the LLM
    private String buildSummarizationPrompt(String userPrompt, String toolResult) {
        return "User request: \"" + userPrompt + "\"\n"
            + "Tool result: " + toolResult + "\n\n"
            + "Above is the result of calling one or more tools. The user cannot see the results, so you should explain them to the user if referencing them in your answer. "
            + "Continue from where you left off without repeating yourself. Provide a clear, concise answer for the user.";
    }


}
