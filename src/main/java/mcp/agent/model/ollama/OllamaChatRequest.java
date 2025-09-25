package mcp.agent.model.ollama;

import java.util.List;

public class OllamaChatRequest {
    private String model;
    private List<OllamaMessage> messages;
    private boolean stream;

    public OllamaChatRequest() {
    }

    public OllamaChatRequest(String model, List<OllamaMessage> messages, boolean stream) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<OllamaMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<OllamaMessage> messages) {
        this.messages = messages;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }
}
