package cn.example.aiagent.computeruse;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ComputerUseService {

    private static final String BASE_DIR = "./computeruse";

    public ComputerUseResult executeCommand(ComputerUseCommand command) {
        log.info("执行Computer Use命令: {}", command.getAction());

        ComputerUseResult result = new ComputerUseResult();
        result.setCommand(command);
        result.setTimestamp(LocalDateTime.now());

        try {
            switch (command.getAction()) {
                case "read_file" -> result.setOutput(readFile(command.getPath()));
                case "write_file" -> result.setOutput(writeFile(command.getPath(), command.getContent()));
                case "list_dir" -> result.setOutput(listDirectory(command.getPath()));
                case "create_dir" -> result.setOutput(createDirectory(command.getPath()));
                case "delete_file" -> result.setOutput(deleteFile(command.getPath()));
                case "run_command" -> result.setOutput(runSystemCommand(command.getCommand()));
                default -> result.setOutput("未知命令: " + command.getAction());
            }
            result.setSuccess(true);
        } catch (Exception e) {
            result.setOutput("执行失败: " + e.getMessage());
            result.setSuccess(false);
            log.error("Computer Use执行失败", e);
        }

        return result;
    }

    private String readFile(String filePath) throws IOException {
        Path path = Paths.get(BASE_DIR, filePath);
        if (!Files.exists(path)) {
            return "文件不存在: " + filePath;
        }
        return Files.readString(path);
    }

    private String writeFile(String filePath, String content) throws IOException {
        Path path = Paths.get(BASE_DIR, filePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return "文件写入成功: " + filePath;
    }

    private String listDirectory(String dirPath) {
        Path path = Paths.get(BASE_DIR, dirPath);
        File dir = path.toFile();
        
        if (!dir.exists()) {
            return "目录不存在: " + dirPath;
        }
        
        if (!dir.isDirectory()) {
            return "不是目录: " + dirPath;
        }
        
        StringBuilder sb = new StringBuilder();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                sb.append(file.getName())
                  .append(file.isDirectory() ? "/" : "")
                  .append("\n");
            }
        }
        return sb.toString();
    }

    private String createDirectory(String dirPath) throws IOException {
        Path path = Paths.get(BASE_DIR, dirPath);
        Files.createDirectories(path);
        return "目录创建成功: " + dirPath;
    }

    private String deleteFile(String filePath) throws IOException {
        Path path = Paths.get(BASE_DIR, filePath);
        if (Files.exists(path)) {
            Files.delete(path);
            return "文件删除成功: " + filePath;
        }
        return "文件不存在: " + filePath;
    }

    private String runSystemCommand(String cmd) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                processBuilder.command("cmd.exe", "/c", cmd);
            } else {
                processBuilder.command("bash", "-c", cmd);
            }

            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            
            String output = new String(process.getInputStream().readAllBytes());
            String error = new String(process.getErrorStream().readAllBytes());
            
            StringBuilder result = new StringBuilder();
            result.append("退出码: ").append(exitCode).append("\n");
            if (!output.isEmpty()) {
                result.append("输出: ").append(output).append("\n");
            }
            if (!error.isEmpty()) {
                result.append("错误: ").append(error);
            }
            
            return result.toString();
        } catch (Exception e) {
            return "命令执行失败: " + e.getMessage();
        }
    }

    @Data
    public static class ComputerUseCommand {
        private String action;
        private String path;
        private String content;
        private String command;
    }

    @Data
    public static class ComputerUseResult {
        private ComputerUseCommand command;
        private String output;
        private boolean success;
        private LocalDateTime timestamp;
    }
}
