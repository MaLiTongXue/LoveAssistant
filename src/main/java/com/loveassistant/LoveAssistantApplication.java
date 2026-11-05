package com.loveassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {
        // 默认关闭数据源自动配置，便于本地直接启动；启用 PgVector 时移除这一项
        DataSourceAutoConfiguration.class
})
public class LoveAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoveAssistantApplication.class, args);
    }

}
