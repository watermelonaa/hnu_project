package mathquiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// 初中题目生成器：至少包含 平方 或 开根号
public class MiddleGenerator implements ProblemGenerator {
    private final Random random = new Random();

    @Override
    public List<String> generate(int count) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(makeExpression());
        }
        return list;
    }

    private String makeExpression() {
        int operands = 1 + random.nextInt(5); // 1-5个操作数
        List<String> parts = new ArrayList<>();

        boolean ensured = false; // 是否已确保含 平方 或 根号
        for (int i = 0; i < operands; i++) {
            int val = 1 + random.nextInt(100);
            String term = String.valueOf(val);
            if (!ensured && random.nextBoolean()) {
                if (random.nextBoolean()) {
                    term = "sqrt(" + term + ")"; // 开根号
                } else {
                    term = "(" + term + ")^2"; // 平方
                }
                ensured = true;
            }
            parts.add(term);
        }

        // 若还未包含，则强制在第0项加入平方
        if (!ensured) {
            String term = parts.get(0);
            parts.set(0, "(" + term + ")^2");
        }

        String[] ops = new String[]{"+", "-", "×", "÷"};
        List<String> tokens = new ArrayList<>();
        tokens.add(parts.get(0));
        for (int i = 1; i < operands; i++) {
            String op = ops[random.nextInt(ops.length)];
            tokens.add(op);
            tokens.add(parts.get(i));
        }
        return String.join(" ", tokens);
    }
}



