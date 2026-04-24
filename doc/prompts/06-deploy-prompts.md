# 构建与交付

## 容器化
你现在是一位资深的DevOps工程师，专精于 SpringBoot 应用的容器化。

请为我们的`tqh-bus-ticket`项目，编写一个`Dockerfile`。如果`Dockerfile`已存在，请审查并重写我们项目现有的`Dockerfile`。

**必须遵循以下生产级最佳实践：**

1.  **多阶段构建 (Multi-stage Build):**
    *   使用一个包含`Maven`的`builder`阶段来缓存jar包和构建应用。
    *   使用一个精简的`eclipse-temurin`的`jre`作为运行环境，只拷贝打包好的 jar 文件，以实现最小化的镜像体积。
2.  **依赖缓存:** 优化`go mod`相关指令的顺序，确保依赖层能够被Docker有效缓存，加速后续构建。
3.  **安全性:**
    *   在最终阶段，使用一个非root用户来运行应用。
    *   确保最终镜像不包含任何源代码或构建工具。

## 使用 Makefile 进行构建
很好。现在，请继续扮演DevOps工程师的角色，为我们的项目创建一个 `Makefile`。

这份 `Makefile` 需要包含以下几个核心目标 (targets):

*   `package`: 打包（跳过测试）。
*   `test`: 运行所有的单元测试。
*   `docker-build`: 使用我们刚才创建的`Dockerfile`来构建容器镜像，镜像tag应为`tqh-bus-ticket:latest`。
*   `docker-run`: 本地运行容器。
*   `docker-stop`: 停止并删除容器。
*   `clean`: 清理所有构建产物。

如果`Makefile`已存在，也要按上面核心目标对其进行审查，如有不符，请重写并覆盖当前`Makefile`。

请确保 `Makefile` 的编写遵循最佳实践，例如使用`.PHONY`来声明伪目标。

## 使用 Claude Code 进行 Makefile
### /package
```
请帮我在创建一个自定义的项目级的slash command文件 `.claude/commands/package.md`。

这个指令的作用是：调用`make package`来构建项目。如果构建失败，它应该自动分析错误原因。

`allowed-tools` 应该包含 `Bash(make:package)`。
```

### /docker-build
再创建一个 `/.claude/commands/docker-build.md`。

这个指令的作用是：调用 `make docker-build`。它应该接受一个参数 `$1` 作为镜像的 Tag。如果用户没有提供参数，默认使用 `latest`。

`allowed-tools` 应该包含 `Bash(docker:*)`, `Bash(make:docker-build)`。

## CI
非常棒！最后一步，我们需要为这个项目设置一套CI/CD流水线。

请为我们创建一个GitHub Actions工作流配置文件，路径为`.github/workflows/ci.yml`。

**这个流水线需要实现以下功能：**
1.  **触发条件：** 当有代码被推送到`main`分支，或者有新的Pull Request被创建时触发。
2.  **核心任务：** 在一个Ubuntu环境中，它需要依次执行以下步骤：
    *   Checkout代码。
    *   设置Java环境。
    *   运行`make test`。
    *   运行`make package`。
3.  **健壮性：** 确保任何一步失败，整个流水线都会失败。