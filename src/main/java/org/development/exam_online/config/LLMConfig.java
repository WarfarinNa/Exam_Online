package org.development.exam_online.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;


@Configuration
@ConfigurationProperties(prefix = "llm")
@Data
public class LLMConfig {
    

    private String apiUrl = "https://api.siliconflow.cn/v1/chat/completions";

    private String apiKey;

    private String defaultModel = "Qwen/Qwen2.5-7B-Instruct";

    private Integer maxTokens = 2000;

    private BigDecimal temperature = BigDecimal.valueOf(0.7);

    private Integer timeout = 30000;

    private Integer maxRetries = 3;

    private Boolean enabled = true;
}
