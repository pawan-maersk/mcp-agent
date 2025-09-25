package mcp.agent.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Represents the function details of a tool.
 * Contains the name, description, and input schema for the function.
 */
public record ToolFunction(
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("parameters") McpSchema.JsonSchema parameters
) {
}
