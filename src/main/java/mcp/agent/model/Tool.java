package mcp.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a tool that can be used by the chat client.
 * This is a wrapper around MCP tools to make them compatible with chat clients.
 */
public record Tool(
    @JsonProperty("type") String type,
    @JsonProperty("function") ToolFunction function,
    @JsonProperty("responseType") String responseType
) {

    /**
     * Creates a new Tool with type "function" and responseType
     */
    public static Tool function(ToolFunction function, String responseType) {
        return new Tool("function", function, responseType);
    }
}
