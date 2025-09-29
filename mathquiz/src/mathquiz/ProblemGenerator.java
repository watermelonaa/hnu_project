package mathquiz;

import java.util.List;

// 题目生成器接口（返回题目字符串列表）
public interface ProblemGenerator {
    // 生成指定数量的题目列表
    List<String> generate(int count);
}



