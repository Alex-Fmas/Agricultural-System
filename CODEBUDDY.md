# CODEBUDDY.md

This file provides guidance to CodeBuddy Code when working with code in this repository.

## 项目现状

设施棚室监测系统（greenhouse monitoring）的 Spring Boot 骨架工程，处于极早期阶段：只有启动类、一个纯静态占位页和两个连接冒烟测试，**尚无 controller / service / mapper / entity**。Maven artifact 名为 `Agricultural-System`，但运行时资源（数据库、容器、Redis key 前缀）统一使用 `greenhouse` 命名。

## 常用命令

```bash
# 启动基础设施：MySQL 8.0 (3306) + Redis 7 (6379)
docker compose up -d

# 运行应用，默认 http://localhost:8080（访问 / 即静态首页）
./mvnw spring-boot:run

# 打包
./mvnw clean package

# 全部测试
./mvnw test

# 单个测试类
./mvnw test -Dtest=ConnectionTest

# 单个测试方法
./mvnw test -Dtest=ConnectionTest#testMySQLConnection

# 离线编译（依赖已在本地 ~/.m2 时更快）
./mvnw -o -B -DskipTests compile

# 销毁容器并清空数据卷（重建数据库/重新执行初始化 SQL 时必须这样）
docker compose down -v
```

- Maven Wrapper 使用 `only-script` 分发模式，Maven 3.10.0，无需本地安装 Maven。
- **无** checkstyle / spotless / 格式化插件，也没有配置 lint 流程；构建的成功标准就是 `compile` 与 `test`。
- 未引入 `spring-boot-devtools`，改代码不会热重载。

## 架构

单个 Maven 模块，Java 17 + Spring Boot 4.0.9（parent），依赖面很窄：

| 能力 | 依赖 | 说明 |
|---|---|---|
| Web | `spring-boot-starter-webmvc` | Boot 4 中取代 `spring-boot-starter-web` 的 starter；提供静态资源与 MVC |
| 持久层 | `mybatis-spring-boot-starter` 4.0.1 + `mysql-connector-j` | 同时带入 `JdbcTemplate`（测试中直接用它查库） |
| 缓存 | `spring-boot-starter-data-redis` | `StringRedisTemplate` 可直接注入 |
| 其他 | Lombok（`optional`） | 需 compiler 的 `annotationProcessorPaths` 才能生效，已在 pom 中配置 |

关键点：

- **包结构**：唯一入口 `com.ssm.agriculturalsystem.AgriculturalSystemApplication`。所有新代码必须放在该包及其子包下，否则不被组件扫描。
- **MyBatis 双配置耦合**：`application.yaml` 指定 `mapper-locations: classpath:mapper/**/*.xml`（对应 `src/main/resources/mapper/`，当前目录不存在，需自行创建），并开启 `map-underscore-to-camel-case`。工程**没有** `@MapperScan`，因此新增 Mapper 接口需要加 `@Mapper`，或在启动类补 `@MapperScan("com.ssm.agriculturalsystem.mapper")`——二选一，否则启动时找不到 Mapper Bean。
- **前端无构建链**：直接由 Spring MVC 提供 `src/main/resources/static/index.html`。当前是单文件静态页（中文文案，主题色 `#246b45`），CSS 内联在 `<style>` 中，无 JS、无接口调用。新增页面同样是「丢进 static 目录即可生效」，没有 npm / 打包步骤。
- **配置与编排强耦合**：`application.yaml` 中的 `greenhouse` 库名、`greenhouse_user` / `greenhouse_pass`、`localhost:3306`、`localhost:6379` 必须与 `docker-compose.yml` 保持一致，改一处要同步改另一处。

## 测试与依赖服务的约束

两个测试类都是 `@SpringBootTest`，**运行前必须先 `docker compose up -d`**，否则上下文加载因数据源/Redis 不可达而失败：

- `AgriculturalSystemApplicationTests#contextLoads`：仅验证上下文可启动。
- `ConnectionTest`：真实集成检查，不是 mock。`testMySQLConnection` 断言 `SELECT DATABASE()` 返回 `greenhouse`；`testRedisConnection` 写入 UUID 键、读回校验后在 `finally` 中删除。

由于 `ConnectionTest` 断言了库名，测试只能跑在 compose 提供的实例上；换库或用内存数据库替代都需要同步修改断言。

## 数据库初始化

`docker-compose.yml` 把 `./sql` 挂到 MySQL 的 `/docker-entrypoint-initdb.d`，但该目录目前为空。**初始化脚本只在数据卷首次创建时执行**：新增 `.sql` 后必须 `docker compose down -v && docker compose up -d`，否则脚本不会被执行。

## 其他注意事项

- `HELP.md` 是 Spring Initializr 的模板说明，已被 `.gitignore` 忽略；仓库说明以 `CODEBUDDY.md` 为准。
- IDEA 工程 SDK 指向 `homebrew-25`，而 pom 中 `java.version=17`；以 pom 为准，若出现编译告警优先检查 toolchain 是否混用。
- Git 仓库尚无任何提交（`main` 分支为空）。
