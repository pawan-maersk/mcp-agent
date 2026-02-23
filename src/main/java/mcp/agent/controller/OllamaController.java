package mcp.agent.controller;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import mcp.agent.service.OllamaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/ollama")
public class OllamaController {

    private static final Logger log = LoggerFactory.getLogger(OllamaController.class);
    private final OllamaService ollamaCloudService;
    private final List<McpSyncClient> mcpClients;

    public OllamaController(OllamaService ollamaCloudService, List<McpSyncClient> mcpClients) {
        this.ollamaCloudService = ollamaCloudService;
        this.mcpClients = mcpClients;
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

    @PostMapping(value = "/ladder/save", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> saveLadderSteps(@RequestBody String prompt) {
        String toolCallPrompt = "Given the following user prompt, extract the required fields (trade, groupName, weekNumber, lastStep, and the number of steps to add if specified), generate the steps array as described, and return a JSON object to call the saveThresholdLadder tool with these arguments. The stepsArray argument must be a stringified JSON array. User prompt: " + prompt;
        String toolCallRaw = ollamaCloudService.chat("qwen3-coder:480b-cloud", toolCallPrompt);
        log.info("Saving Ladder Steps to Tool Call: {}", toolCallRaw);
        int objStart = toolCallRaw.indexOf('{');
        int objEnd = toolCallRaw.lastIndexOf('}');
        if (objStart == -1 || objEnd == -1 || objEnd <= objStart) {
            return ResponseEntity.ok("Malformed tool call JSON from LLM: " + toolCallRaw);
        }
        String toolCallJson = toolCallRaw.substring(objStart, objEnd + 1);
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> toolCall = mapper.readValue(toolCallJson, Map.class);
            String toolName = "saveThresholdLadder";
            Map<String, Object> arguments = toolCall.containsKey("arguments")
                ? (Map<String, Object>) toolCall.get("arguments")
                : toolCall;

            // Normalize steps: accept either 'stepsArray' or 'steps', string or list
            Object stepsObj = arguments.containsKey("stepsArray")
                ? arguments.remove("stepsArray")
                : arguments.get("steps");
            if (stepsObj != null) {
                List<Map<String, Object>> stepsList;
                if (stepsObj instanceof String) {
                    try {
                        stepsList = mapper.readValue((String) stepsObj, List.class);
                    } catch (Exception ex) {
                        return ResponseEntity.ok("Malformed steps argument in tool call: " + stepsObj);
                    }
                } else if (stepsObj instanceof List) {
                    stepsList = (List<Map<String, Object>>) stepsObj;
                } else {
                    return ResponseEntity.ok("Malformed steps argument in tool call: " + stepsObj);
                }
                arguments.put("steps", stepsList);
            }

            if (mcpClients.isEmpty()) {
                return ResponseEntity.status(500).body("No MCP client configured.");
            }
            McpSyncClient mcpSyncClient = mcpClients.get(0);
            Object result = mcpSyncClient.callTool(new McpSchema.CallToolRequest(toolName, arguments));
            String resultText = (result == null) ? "No result returned." : result.toString();
            return ResponseEntity.ok(resultText);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to parse tool call JSON: " + e.getMessage());
        }
    }

    @PostMapping(value = "/ladder", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleLadder(@RequestBody String prompt) {
        String llmPrompt = "Given the user prompt, return a JSON object with: " +
            "'intent': 'add' if the user only wants to generate, preview, or add steps (e.g., 'add', 'generate', 'show', 'create', etc.), " +
            "or 'save' ONLY if the user explicitly asks to save, persist, store, or commit the steps. " +
            "'steps': the generated steps array, " +
            "'saveArgs': (object with trade, groupName, weekNumber, etc.) only if intent is 'save'. " +
            "Only return valid JSON. User prompt: " + prompt;
        String llmResponse = ollamaCloudService.chat("qwen3-coder:480b-cloud", llmPrompt);
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Extract JSON object from LLM response
            int objStart = llmResponse.indexOf('{');
            int objEnd = llmResponse.lastIndexOf('}');
            if (objStart == -1 || objEnd == -1 || objEnd <= objStart) {
                return ResponseEntity.ok(Map.of("error", "Malformed LLM response", "llmResponse", llmResponse));
            }
            String json = llmResponse.substring(objStart, objEnd + 1);
            Map<String, Object> root = mapper.readValue(json, Map.class);
            String intent = (String) root.get("intent");
            Object stepsObj = root.get("steps");
            if ("add".equalsIgnoreCase(intent)) {
                // Just return the steps array
                return ResponseEntity.ok(stepsObj);
            } else if ("save".equalsIgnoreCase(intent)) {
                // Backend safeguard: only allow save if prompt contains save/persist/store/commit
                String lowerPrompt = prompt.toLowerCase();
                if (!(lowerPrompt.contains("save") || lowerPrompt.contains("persist") || lowerPrompt.contains("store") || lowerPrompt.contains("commit"))) {
                    // LLM misclassified, treat as add
                    return ResponseEntity.ok(stepsObj);
                }
                Object saveArgsObj = root.get("saveArgs");
                if (saveArgsObj == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Missing saveArgs for save intent"));
                }
                Map<String, Object> saveArgs = mapper.convertValue(saveArgsObj, Map.class);
                // Normalize steps: always provide as 'steps' and as a List
                List<Map<String, Object>> stepsList;
                if (stepsObj instanceof String) {
                    try {
                        stepsList = mapper.readValue((String) stepsObj, List.class);
                    } catch (Exception ex) {
                        return ResponseEntity.ok(Map.of("error", "Malformed steps argument in LLM response", "steps", stepsObj));
                    }
                } else if (stepsObj instanceof List) {
                    stepsList = (List<Map<String, Object>>) stepsObj;
                } else {
                    return ResponseEntity.ok(Map.of("error", "Malformed steps argument in LLM response", "steps", stepsObj));
                }
                saveArgs.put("steps", stepsList);
                if (mcpClients.isEmpty()) {
                    return ResponseEntity.status(500).body(Map.of("error", "No MCP client configured."));
                }
                McpSyncClient mcpSyncClient = mcpClients.get(0);
                Object result = mcpSyncClient.callTool(new McpSchema.CallToolRequest("saveThresholdLadder", saveArgs));
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Unknown intent", "intent", intent));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to parse LLM response or call tool: " + e.getMessage()));
        }
    }
}
