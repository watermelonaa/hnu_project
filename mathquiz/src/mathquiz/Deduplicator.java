package mathquiz;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 去重：保证同一账号的卷子题目不与历史重复
public class Deduplicator {
    private final String username; // 当前账号名

    public Deduplicator(String username) {
        this.username = username;
    }

    // 读取该用户文件夹所有历史题目，构建集合用于去重
    private Set<String> loadHistoryQuestions() {
        Set<String> set = new HashSet<>();
        Path dir = getUserDir();
        if (!Files.exists(dir)) {
            return set;
        }
        try {
            // 遍历该文件夹下所有txt文件
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt")) {
                for (Path p : stream) {
                    List<String> lines = Files.readAllLines(p);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) {
                            continue;
                        }
                        // 文件格式为：题号. 空格 题目；我们只取题目部分做去重
                        int idx = trimmed.indexOf('.');
                        if (idx > -1 && idx + 1 < trimmed.length()) {
                            String q = trimmed.substring(idx + 1).trim();
                            if (!q.isEmpty()) {
                                set.add(q);
                            }
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            // 简化：如果读取失败则忽略历史
        }
        return set;
    }

    private Path getUserDir() {
        // 以用户名创建独立文件夹
        return Paths.get("output", username);
    }

    // 确保生成的题目列表在历史中不重复。不足时用生成器补齐。
    public List<String> ensureUnique(List<String> generated, int targetCount, ProblemGenerator generator) {
        Set<String> history = loadHistoryQuestions();
        LinkedHashSet<String> current = new LinkedHashSet<>(); // 保持顺序
        for (String q : generated) {
            if (!history.contains(q)) {
                current.add(q);
            }
            if (current.size() == targetCount) {
                break;
            }
        }

        // 如果数量不够，继续生成直到够或尝试次数达到上限
        int safeGuard = 0; // 简单的安全阈值避免死循环
        while (current.size() < targetCount && safeGuard < 1000) {
            List<String> more = generator.generate(1);
            for (String q : more) {
                if (!history.contains(q) && !current.contains(q)) {
                    current.add(q);
                }
                if (current.size() == targetCount) {
                    break;
                }
            }
            safeGuard++;
        }
        return new ArrayList<>(current);
    }
}



