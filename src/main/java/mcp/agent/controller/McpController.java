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

    // Helper to extract the first JSON object or array from a string
    private String extractFirstJsonStructure(String text) {
        int objStart = text.indexOf('{');
        int objEnd = text.lastIndexOf('}');
        int arrStart = text.indexOf('[');
        int arrEnd = text.lastIndexOf(']');
        // Prefer array if present and well-formed
        if (arrStart != -1 && arrEnd != -1 && arrEnd > arrStart) {
            return text.substring(arrStart, arrEnd + 1);
        } else if (objStart != -1 && objEnd != -1 && objEnd > objStart) {
            return text.substring(objStart, objEnd + 1);
        }
        return null;
    }

    @PostMapping(value = "/ask", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> ask(@RequestBody String userPrompt) {
        String parseResult = ollamaService.buildPrompt(availableTools, userPrompt);
        String llmResponse = ollamaService.chat("qwen3-coder:480b-cloud", parseResult);
        try {
            String json = extractFirstJsonStructure(llmResponse);
            if (json == null) {
                return ResponseEntity.ok(llmResponse.trim());
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            // Always treat as array for unified processing
            List<JsonNode> toolCalls;
            if (root.isArray()) {
                toolCalls = new java.util.ArrayList<>();
                root.forEach(toolCalls::add);
            } else if ((root.has("name") || root.has("tool")) && root.has("arguments")) {
                toolCalls = java.util.Collections.singletonList(root);
            } else {
                return ResponseEntity.ok(llmResponse.trim());
            }
            StringBuilder allResults = new StringBuilder();
            McpSyncClient mcpSyncClient = mcpClients.get(0);
            for (JsonNode toolCall : toolCalls) {
                String toolName = toolCall.has("name") ? toolCall.get("name").asText() : toolCall.get("tool").asText();
                Map<String, Object> arguments = mapper.convertValue(toolCall.get("arguments"), Map.class);
                Object toolResult = mcpSyncClient.callTool(new McpSchema.CallToolRequest(toolName, arguments));
                String resultText = (toolResult == null) ? "No result returned." : toolResult.toString();
                allResults.append(resultText).append("\n\n");
            }
            String summarizationPrompt = buildSummarizationPrompt(userPrompt, allResults.toString().trim());
            String summary = ollamaService.chat("qwen3-coder:480b-cloud", summarizationPrompt);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to parse LLM response or call tool: " + e.getMessage());
        }
    }

    // Helper to build a summarization prompt for the LLM
    private String buildSummarizationPrompt(String userPrompt, String toolResult) {
        return "User request: \"" + userPrompt + "\"\n"
            + "Tool result: " + toolResult + "\n\n"
            + "Above is the result of calling one or more tools. The user cannot see the results, so you should explain them to the user if referencing them in your answer. Continue from where you left off without repeating yourself.";
    }


}
