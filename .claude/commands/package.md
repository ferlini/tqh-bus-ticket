---
description: 调用 make package 构建项目，构建失败时自动分析错误原因。
allowed-tools: Bash(make:package)
---
1. 执行 `make package` 进行项目打包。
2. 如果构建成功，输出简短的成功提示。
3. 如果构建失败，分析错误日志，定位失败原因，并给出修复建议。
