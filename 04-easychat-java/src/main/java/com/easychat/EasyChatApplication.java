package com.easychat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAsync // 开启异步任务
@MapperScan("com.easychat.mappers")
@SpringBootApplication(scanBasePackages = {"com.easychat"}) // 定义扫描的包 ,exclude = DataSourceAutoConfiguration.class
@EnableTransactionManagement // 开启事务
@EnableScheduling // 开启定时任务
public class EasyChatApplication {
    public static void main(String[] args){
        SpringApplication.run(EasyChatApplication.class, args);
    }
}
