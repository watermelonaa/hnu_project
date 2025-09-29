package mathquiz;

// 工厂：根据账户类型返回对应生成器
public class GeneratorFactory {
    public static ProblemGenerator getGenerator(AccountType type) {
        switch (type) {
            case PRIMARY:
                return new PrimaryGenerator();
            case MIDDLE:
                return new MiddleGenerator();
            case HIGH:
            default:
                return new HighGenerator();
        }
    }
}



