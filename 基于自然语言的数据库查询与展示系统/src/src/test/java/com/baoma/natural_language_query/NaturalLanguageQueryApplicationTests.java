package com.baoma.natural_language_query;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "llm.api.key=test-key",
    "llm.api.url=https://api.moonshot.cn/v1/chat/completions",
    "llm.model=moonshot-v1-8k"
})
class NaturalLanguageQueryApplicationTests {

    @Test
    void contextLoads() {
    }

}
