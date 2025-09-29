package mathquiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// 小学题目生成器：只含 + - × ÷ 和括号
public class PrimaryGenerator implements ProblemGenerator {
    private final Random random = new Random(); // 随机数

    @Override
    public List<String> generate(int count) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(makeExpression()); // 逐个生成表达式
        }
        return list;
    }

    // 生成一个仅含 + - × ÷ 和括号 的表达式，确保减法不产生负数
    private String makeExpression() {
        int operands = 2 + random.nextInt(4); // 操作数数量：2-5（确保至少2个操作数）
        List<Integer> nums = generateOperands(operands);
        List<String> ops = generateOperators(operands - 1);

        // 确保减法不产生负数
        adjustForNonNegativeSubtraction(nums, ops);

        // 构建表达式
        List<String> tokens = buildExpressionTokens(nums, ops);

        // 简单随机加括号（可选）
        if (operands >= 3 && random.nextBoolean()) {
            addParentheses(tokens, operands);
        }

        return String.join(" ", tokens);
    }

    // 生成操作数
    private List<Integer> generateOperands(int count) {
        List<Integer> nums = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int val = 1 + random.nextInt(100); // 1-100
            nums.add(val);
        }
        return nums;
    }

    // 生成运算符
    private List<String> generateOperators(int count) {
        String[] ops = new String[]{"+", "-", "×", "÷"};
        List<String> operators = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            operators.add(ops[random.nextInt(ops.length)]);
        }
        return operators;
    }

    // 调整操作数确保减法不产生负数
    private void adjustForNonNegativeSubtraction(List<Integer> nums, List<String> ops) {
        for (int i = 0; i < ops.size(); i++) {
            if ("-".equals(ops.get(i))) {
                // 确保被减数大于等于减数
                int minuend = nums.get(i);
                int subtrahend = nums.get(i + 1);
                if (minuend < subtrahend) {
                    // 交换两个数，确保被减数更大
                    nums.set(i, subtrahend);
                    nums.set(i + 1, minuend);
                }
            }
        }
    }

    // 构建表达式标记
    private List<String> buildExpressionTokens(List<Integer> nums, List<String> ops) {
        List<String> tokens = new ArrayList<>();
        tokens.add(String.valueOf(nums.get(0)));
        for (int i = 0; i < ops.size(); i++) {
            tokens.add(ops.get(i));
            tokens.add(String.valueOf(nums.get(i + 1)));
        }
        return tokens;
    }

    // 添加括号
    private void addParentheses(List<String> tokens, int operands) {
        int b = 1 + random.nextInt(operands - 1);
        int leftIndex = 0;
        int rightIndex = b * 2;
        tokens.add(leftIndex, "(");
        tokens.add(rightIndex + 2, ")");
    }
}


