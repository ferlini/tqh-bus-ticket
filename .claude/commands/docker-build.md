---
description: 调用 make docker-build 构建 Docker 镜像，可指定镜像 Tag。
allowed-tools: Bash(docker:*), Bash(make:docker-build)
---
1. 确定镜像 Tag：如果用户提供了参数 `$1`，则使用该值作为 Tag；否则默认使用 `latest`。
2. 执行 `make docker-build IMAGE_TAG=<tag>` 构建镜像。
3. 如果构建成功，执行 `docker images tqh-bus-ticket` 展示镜像信息。
4. 如果构建失败，分析错误日志，定位失败原因，并给出修复建议。
