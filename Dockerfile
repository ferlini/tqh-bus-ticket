# ============================================
# Stage 1: Build
# ============================================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# 1) 先拷贝 Maven Wrapper 和 pom.xml，利用 Docker 层缓存下载依赖
COPY pom.xml mvnw ./
COPY .mvn .mvn

# 下载所有依赖（不编译源码），形成独立的缓存层
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw dependency:go-offline -B

# 2) 再拷贝源码并打包（依赖未变时，上面的层直接命中缓存）
COPY src src

RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw package -B -DskipTests \
    && cp target/tqh-bus-ticket-*.jar target/app.jar

# ============================================
# Stage 2: Runtime
# ============================================
FROM eclipse-temurin:21-jre

# 创建非 root 用户
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup --no-create-home appuser

WORKDIR /app

# 只拷贝打包好的 jar，不携带源码和构建工具
COPY --from=builder /build/target/app.jar app.jar

# 以非 root 用户运行
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
