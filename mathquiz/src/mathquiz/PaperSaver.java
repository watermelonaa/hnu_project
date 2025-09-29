package mathquiz;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 保存卷子到文件
public class PaperSaver {
    private final String username; // 用户名决定文件夹

    public PaperSaver(String username) {
        this.username = username;
    }

    public String save(List<String> questions) {
        try {
            Path dir = Paths.get("output", username); // output/用户名
            Files.createDirectories(dir); // 不存在则创建
            String filename = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")) + ".txt";
            Path file = dir.resolve(filename);

            // 写入：题号. 题目；题与题之间空一行
            try (BufferedWriter bw = Files.newBufferedWriter(file)) {
                int idx = 1;
                for (String q : questions) {
                    bw.write(idx + ". " + q);
                    bw.newLine();
                    bw.newLine(); // 空一行
                    idx++;
                }
            }
            return file.toString();
        } catch (IOException e) {
            return "保存失败:" + e.getMessage();
        }
    }
}



