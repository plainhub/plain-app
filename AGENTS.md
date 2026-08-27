# AGENTS.md — 项目工作规则

> 本文件约束 AI Agent 在本仓库内的全部行为。改动前先读这里。

## 1. 提交规则（最高优先级）
- commit message 用英语，格式：`类型: 简要说明`（类型如 `feat` `fix` `refactor` `docs`）。
- 示例：`feat: 新增 SettingsPage 设置页`

## 2. 依赖版本
- **库版本始终用最新稳定版**，不要限定旧版本。

## 3. 工作方式
- 只能用英语注释，少加注释
- plain-desktop源码在../plain-desktop
- 动手前先读相关现有代码，避免重复实现或破坏既有逻辑。
- 改动完成必须本地构建通过（`./gradlew :app:assembleDebug`）。
- 本机终端 PATH 可能缺系统目录，构建前先：
  `export PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH`

## 4. Token 纪律
- 禁止浪费 token：能只读就不整段粘贴、能一次批量读就不分段读、能用一条命令就不拆多条。