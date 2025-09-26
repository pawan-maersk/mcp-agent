package mcp.agent.controller;

import io.modelcontextprotocol.spec.McpSchema;
import mcp.agent.model.Tool;
import io.modelcontextprotocol.client.McpSyncClient;
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

    public McpController(@Qualifier("availableTools") List<Tool> availableTools,
                        List<McpSyncClient> mcpClients) {
        this.availableTools = availableTools;
        this.mcpClients = mcpClients;
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
}
