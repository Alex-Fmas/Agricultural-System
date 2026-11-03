package com.ssm.agriculturalsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ConnectionTest {

    // MySQL 操作工具
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Redis 操作工具
    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 测试 MySQL 连接
     */
    @Test
    void testMySQLConnection() {

        // 查询当前连接的数据库名称
        String databaseName = jdbcTemplate.queryForObject(
                "SELECT DATABASE()",
                String.class
        );

        // 验证连接的是 greenhouse 数据库
        assertEquals("greenhouse", databaseName);

        System.out.println("========== MySQL 测试成功 ==========");
        System.out.println("当前数据库：" + databaseName);
    }


    /**
     * 测试 Redis 连接
     */
    @Test
    void testRedisConnection() {

        // 创建唯一测试 Key，避免覆盖其他数据
        String key = "greenhouse:test:" + UUID.randomUUID();

        boolean saved = false;

        try {
            // 写入 Redis
            stringRedisTemplate.opsForValue()
                    .set(key, "connection-success", Duration.ofSeconds(30));

            saved = true;

            // 读取 Redis
            String value = stringRedisTemplate.opsForValue().get(key);

            // 验证读取结果
            assertEquals("connection-success", value);

            System.out.println("========== Redis 测试成功 ==========");
            System.out.println("测试 Key：" + key);
            System.out.println("读取 Value：" + value);

        } finally {
            // 删除测试数据
            if (saved) {
                stringRedisTemplate.delete(key);
            }
        }
    }
}
