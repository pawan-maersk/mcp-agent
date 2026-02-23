package mcp.agent.config;

import io.modelcontextprotocol.client.McpSyncClient;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import mcp.agent.model.Tool;
import mcp.agent.model.ToolFunction;

@Configuration
public class McpClientConfig {

    @Bean
    @Qualifier("availableTools")
    List<Tool> chatClientTools(List<McpSyncClient> mcpSyncClientList) {
        return mcpSyncClientList.stream()
            .flatMap(client -> client.listTools().tools().stream())
            .map(tool -> {
                String responseType = "summarize";
                String desc = tool.description();
                if (desc != null) {
                    Pattern pattern = Pattern.compile("\\[.*?purpose\\s*=\\s*([^;\\]]+)");
                    Matcher matcher = pattern.matcher(desc);
                    if (matcher.find()) {
                        responseType = matcher.group(1).trim();
                    }
                }
                return new Tool(
                    "function",
                    new ToolFunction(
                        tool.name(),
                        tool.description(),
                        tool.inputSchema()),
                    responseType
                );
            })
            .toList();
    }

}
