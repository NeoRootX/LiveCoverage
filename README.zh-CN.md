# Live Coverage 插件

**语言：** [English](README.md) · [简体中文](README.zh-CN.md) · [日本語](README.ja.md) · [한국어](README.ko.md)

基于 JaCoCo 的 IntelliJ IDEA 实时代码覆盖可视化插件。

## 功能特性

### 功能
- **实时覆盖可视化**：在应用运行时自动高亮已执行代码
- **累计覆盖模式**：持续累计执行覆盖；可通过「Clear Coverage」清空并重新开始
- **内置 JaCoCo Agent**：插件已内置 agent JAR，无需额外下载
- **工具窗口**：显示覆盖统计与方法级执行信息
- **手动控制**：支持刷新、清空（重置）、启停轮询
- **多模块支持**：支持添加多组 `Source output path` + `Classes output path`

### 解决业务问题
- 面向复杂且难维护的遗留业务代码
- 支持增量开发与需求变更的影响范围分析
- 回归测试前的测试数据准备与 bug 定位
- 快速回答：「这次请求实际执行了哪些业务代码？」

### 核心价值
- **请求到代码路径可视化，降低理解成本**：快速定位被执行代码块
- **回归测试效率提升**：基于真实执行路径分析代码及需求，准备测试数据
- **业务 bug 定位**：更快理解遗留系统行为，快速分析定位业务出错的代码片段

## 安装

请从 [JetBrains Marketplace](https://plugins.jetbrains.com) 安装（搜索 **Live Coverage**）。提供 30 天试用和按月订阅。

本地开发构建见下方 [构建](#构建) 章节。

## 配置

1. **配置插件设置**：
   - 进入 `Settings` -> `Tools` -> `Live Coverage`
   - 设置 TCP 地址（默认：127.0.0.1）
   - 设置 TCP 端口（默认：6300）
   - 添加一组或多组路径对：
     - `Source output path`（Java 源码根目录绝对路径）
     - `Classes output path`（编译后 `.class` 文件目录绝对路径）

2. **运行应用**：
   - **自动注入**：JVM 参数会自动加入运行配置
   - 在运行/调试 Java 应用时，插件会自动注入 JaCoCo JVM 参数
   - 无需手动配置

## 使用说明

### 自动模式
打开项目后插件会自动开始轮询。代码执行过程中覆盖会实时累计。

### 手动控制
可通过 `Tools` -> `Live Coverage` 使用：
- **Refresh Coverage**：手动触发一次覆盖更新
- **Clear Coverage**：清空所有高亮并重置 JaCoCo agent，用于重新开始
- **Pause/Resume Coverage**：启动/停止自动轮询

### 工具窗口
打开底部「Request Coverage」工具窗口可查看：
- 覆盖统计
- 方法级执行信息
- 连接状态
- `Clear Coverage` 和 `Pause/Resume Coverage` 按钮

### 关键能力
- ✅ **内置 Agent**：无需手动下载 JaCoCo agent JAR
- ✅ **自动 JVM 参数**：自动加入运行配置，零手工设置
- ✅ **累计模式**：覆盖自动累计
- ✅ **手动重置**：随时清空重新开始
- ✅ **实时更新**：执行路径实时可见
- ✅ **工程化设计**：线程安全、资源受控的架构

## 构建

```bash
./gradlew buildPlugin
```

插件产物位于 `build/distributions/`。

## 开发

### 项目结构
- **源代码**：`src/main/java/`
- **资源文件**：`src/main/resources/`
- **构建配置**：`build.gradle.kts`

### 代码规范
- 遵循 IntelliJ Platform 编码规范
- 使用 Java 21
- 所有服务均为线程安全设计
- 使用 `@NotNull`/`@Nullable` 做空安全约束

## 故障排查

### 没有覆盖高亮
1. 确认 JaCoCo agent 已启动且可访问
2. 检查设置中的 TCP 地址与端口
3. 确认 classes output path 正确
4. 确认 source output path 与源码结构匹配

### 连接错误
- 检查 JaCoCo agent 是否以正确 TCP 配置启动
- 检查防火墙/网络配置
- 查看 IDE 日志（Help -> Show Log in Finder/Explorer）

## 许可证

Copyright (c) 2026 Showen。用户许可协议：[EULA.md](EULA.md)。

## 致谢

基于以下技术构建：
- IntelliJ Platform SDK
- JaCoCo Code Coverage Library
