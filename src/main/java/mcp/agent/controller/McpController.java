package mcp.agent.controller;

import mcp.agent.model.Tool;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
}

