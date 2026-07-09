package cn.example.aiagent.mcp;

import lombok.Data;

import java.util.List;

@Data
public class McpTool {
    private String name;
    private String description;
    private List<McpParameter> parameters;
}
