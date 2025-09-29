package mathquiz;

import java.util.Optional;
import java.util.OptionalInt;

// 输入校验与解析
public class Validators {
    // 解析题目数量：10-30 或 -1；返回OptionalInt
    public static OptionalInt parseCount(String input) {
        try {
            int n = Integer.parseInt(input.trim());
            if (n == -1) {
                return OptionalInt.of(-1);
            }
            if (n >= 10 && n <= 30) {
                return OptionalInt.of(n);
            }
            return OptionalInt.empty();
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    // 解析切换命令："切换为 小学/初中/高中"
    public static Optional<AccountType> parseSwitchCommand(String input) {
        String trimmed = input.trim();
        if (!trimmed.startsWith("切换为")) {
            return Optional.empty();
        }
        String rest = trimmed.substring("切换为".length()).trim();
        if (rest.isEmpty()) {
            return Optional.empty();
        }
        switch (rest) {
            case "小学":
                return Optional.of(AccountType.PRIMARY);
            case "初中":
                return Optional.of(AccountType.MIDDLE);
            case "高中":
                return Optional.of(AccountType.HIGH);
            default:
                return Optional.empty();
        }
    }

    // 判断是否像切换命令但无效（用于输出"请输入小学、初中和高中三个选项中的一个"）
    public static boolean isSwitchCommandLikeButInvalid(String input) {
        String trimmed = input.trim();
        if (!trimmed.startsWith("切换为")) {
            return false;
        }
        // 以"切换为"开头，但不是3个选项之一
        Optional<AccountType> ok = parseSwitchCommand(input);
        return !ok.isPresent();
    }
}



