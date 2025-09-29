package mathquiz;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        ConsolePrompter prompter = new ConsolePrompter(new Scanner(System.in));
        AuthService authService = new AuthService();
        while (true) { // 整个程序主循环
            UserSession session = loginUser(prompter, authService);
            if (session != null) {
                handleUserSession(prompter, session);
            }
        }
    }

    // 处理用户登录，返回用户会话，失败返回null
    private static UserSession loginUser(ConsolePrompter prompter, AuthService authService) {
        while (true) {
            String line = prompter.readLine("请输入用户名和密码（用空格分隔）：");
            String[] parts = line.trim().split("\\s+", 2);
            if (parts.length != 2) {
                prompter.println("请输入正确的用户名、密码");
                continue;
            }
            String username = parts[0];
            String password = parts[1];
            Optional<AccountType> typeOpt = authService.verify(username, password);
            if (!typeOpt.isPresent()) {
                prompter.println("请输入正确的用户名、密码");
                continue;
            }
            AccountType currentType = typeOpt.get();
            prompter.println("当前选择为 " + currentType.displayName() + " 出题");
            return new UserSession(username, currentType);
        }
    }

    // 处理登录后的用户会话
    private static void handleUserSession(ConsolePrompter prompter, UserSession session) {
        while (true) {
            String prompt = "准备生成 " + session.getAccountType().displayName() + " 数学题目，请输入生成题目数量（输入-1将退出当前用户，重新登录）：";
            String input = prompter.readLine(prompt).trim();

            // 处理切换命令
            Optional<AccountType> switchType = Validators.parseSwitchCommand(input);
            if (switchType.isPresent()) {
                session.setAccountType(switchType.get());
                prompter.println("准备生成 " + session.getAccountType().displayName() + " 数学题目，请输入生成题目数量");
                continue;
            } else if (Validators.isSwitchCommandLikeButInvalid(input)) {
                prompter.println("请输入小学、初中和高中三个选项中的一个");
                continue;
            }

            // 处理数量输入
            OptionalInt countOpt = Validators.parseCount(input);
            if (!countOpt.isPresent()) {
                prompter.println("请输入10-30之间的数字，或-1退出登录");
                continue;
            }
            int count = countOpt.getAsInt();
            if (count == -1) {
                break; // 退出当前用户
            }

            // 生成并保存题目
            generateAndSaveQuestions(prompter, session, count);
        }
    }

    // 生成并保存题目
    private static void generateAndSaveQuestions(ConsolePrompter prompter, UserSession session, int count) {
        ProblemGenerator generator = GeneratorFactory.getGenerator(session.getAccountType());
        List<String> raw = generator.generate(count);
        Deduplicator deduplicator = new Deduplicator(session.getUsername());
        List<String> unique = deduplicator.ensureUnique(raw, count, generator);
        PaperSaver saver = new PaperSaver(session.getUsername());
        String filePath = saver.save(unique);
        prompter.println("已生成并保存：" + filePath);
    }
}

// 用户会话类，保存用户名和账户类型
class UserSession {
    private final String username;
    private AccountType accountType;

    public UserSession(String username, AccountType accountType) {
        this.username = username;
        this.accountType = accountType;
    }

    public String getUsername() {
        return username;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }
}



