package mathquiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// 高中题目生成器：至少包含 sin/cos/tan
public class HighGenerator implements ProblemGenerator {
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
        int operands = 1 + random.nextInt(5);
        List<String> parts = new ArrayList<>();

        boolean ensured = false; // 是否已包含三角函数
        for (int i = 0; i < operands; i++) {
            int val = 1 + random.nextInt(100);
            String term = String.valueOf(val);
            if (!ensured && random.nextBoolean()) {
                String fn = pickOne(new String[]{"sin", "cos", "tan"});
                term = fn + "(" + term + ")"; // 添加三角函数
                ensured = true;
            }
            parts.add(term);
        }

        if (!ensured) {
            // 强制在第一个项上加入三角函数
            String fn = pickOne(new String[]{"sin", "cos", "tan"});
            parts.set(0, fn + "(" + parts.get(0) + ")");
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

    private String pickOne(String[] arr) {
        return arr[random.nextInt(arr.length)];
    }
}



