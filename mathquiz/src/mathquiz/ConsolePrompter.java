package mathquiz;

import java.util.Scanner;

// 控制台输入输出封装（简单）
public class ConsolePrompter {
    private final Scanner scanner; // 输入扫描器

    public ConsolePrompter(Scanner scanner) {
        this.scanner = scanner; // 保存传入的Scanner
    }

    public String readLine(String prompt) {
        // 打印提示并读取一行输入
        System.out.print(prompt);
        return scanner.nextLine();
    }

    public void println(String msg) {
        // 打印一行
        System.out.println(msg);
    }
}



