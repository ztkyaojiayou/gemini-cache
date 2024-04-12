package com.mirson.gemini.cache.example.startup; /**
 * @author mirson
 * @date 2021/10/3
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


/**
 * 缓存示例启动程序
 * @author zoutongkun
 */
@SpringBootApplication
//需要把starter所在的包扫描进去，否则无法加载对应的bean，
//不过一般更推荐使用spi的机制导出！
@ComponentScan(basePackages = "com.mirson")
public class GeminiCacheExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeminiCacheExampleApplication.class, args);
    }

}
