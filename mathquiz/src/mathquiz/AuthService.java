package mathquiz;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// 简单的账号服务：内存中预置9个账号
public class AuthService {
    private final Map<String, String> userToPassword = new HashMap<>(); // 用户名->密码
    private final Map<String, AccountType> userToType = new HashMap<>(); // 用户名->账户类型

    public AuthService() {
        initializeAccounts();
    }

    // 初始化所有预设账号
    private void initializeAccounts() {
        addPrimaryAccounts();
        addMiddleAccounts();
        addHighAccounts();
    }

    // 添加小学账号
    private void addPrimaryAccounts() {
        String[] usernames = {"张三1", "张三2", "张三3"};
        for (String username : usernames) {
            userToPassword.put(username, "123");
            userToType.put(username, AccountType.PRIMARY);
        }
    }

    // 添加初中账号
    private void addMiddleAccounts() {
        String[] usernames = {"李四1", "李四2", "李四3"};
        for (String username : usernames) {
            userToPassword.put(username, "123");
            userToType.put(username, AccountType.MIDDLE);
        }
    }

    // 添加高中账号
    private void addHighAccounts() {
        String[] usernames = {"王五1", "王五2", "王五3"};
        for (String username : usernames) {
            userToPassword.put(username, "123");
            userToType.put(username, AccountType.HIGH);
        }
    }

    // 校验用户名和密码；成功返回账户类型
    public Optional<AccountType> verify(String username, String password) {
        if (!userToPassword.containsKey(username)) {
            return Optional.empty(); // 用户不存在
        }
        String pw = userToPassword.get(username);
        if (!Objects.equals(pw, password)) {
            return Optional.empty(); // 密码不正确
        }
        return Optional.ofNullable(userToType.get(username));
    }
}



