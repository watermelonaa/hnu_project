package mathquiz;

// 账户类型（枚举），用于区分小学/初中/高中
public enum AccountType {
    PRIMARY("小学"),
    MIDDLE("初中"),
    HIGH("高中");

    private final String displayName; // 中文显示名

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}