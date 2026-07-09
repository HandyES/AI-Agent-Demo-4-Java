package cn.example.aiagent.mcp;

import lombok.Data;

@Data
public class McpParameter {
    private String name;
    private String type;
    private String description;
    private boolean required;
}
