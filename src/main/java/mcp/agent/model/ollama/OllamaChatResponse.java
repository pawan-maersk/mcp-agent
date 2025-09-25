package mcp.agent.model.ollama;

public class OllamaChatResponse {
    private String model;
    private OllamaMessage message;
    private long createdAt;
    private boolean done;
    private int totalDuration;
    private int loadDuration;
    private int promptEvalCount;
    private int promptEvalDuration;
    private int evalCount;
    private int evalDuration;

    public OllamaChatResponse() {
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public OllamaMessage getMessage() {
        return message;
    }

    public void setMessage(OllamaMessage message) {
        this.message = message;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(int totalDuration) {
        this.totalDuration = totalDuration;
    }

    public int getLoadDuration() {
        return loadDuration;
    }

    public void setLoadDuration(int loadDuration) {
        this.loadDuration = loadDuration;
    }

    public int getPromptEvalCount() {
        return promptEvalCount;
    }

    public void setPromptEvalCount(int promptEvalCount) {
        this.promptEvalCount = promptEvalCount;
    }

    public int getPromptEvalDuration() {
        return promptEvalDuration;
    }

    public void setPromptEvalDuration(int promptEvalDuration) {
        this.promptEvalDuration = promptEvalDuration;
    }

    public int getEvalCount() {
        return evalCount;
    }

    public void setEvalCount(int evalCount) {
        this.evalCount = evalCount;
    }

    public int getEvalDuration() {
        return evalDuration;
    }

    public void setEvalDuration(int evalDuration) {
        this.evalDuration = evalDuration;
    }
}
