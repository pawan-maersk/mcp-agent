package mcp.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a tool that can be used by the chat client.
 * This is a wrapper around MCP tools to make them compatible with chat clients.
 */
public record Tool(
    @JsonProperty("type") String type,
    @JsonProperty("function") ToolFunction function
) {

    /**
     * Creates a new Tool with type "function"
     */
    public static Tool function(ToolFunction function) {
        return new Tool("function", function);
    }
}
