IMAGE_NAME  := tqh-bus-ticket
IMAGE_TAG   := latest
CONTAINER   := tqh-bus-ticket

.PHONY: package test docker-build docker-run docker-stop clean

## 打包（跳过测试）
package:
	./mvnw package -B -DskipTests

## 运行所有单元测试
test:
	./mvnw test -B

## 构建 Docker 镜像
docker-build:
	docker build -t $(IMAGE_NAME):$(IMAGE_TAG) .

## 本地运行容器（前台 detach 模式，映射 8080 端口）
docker-run:
	docker run -d --name $(CONTAINER) -p 8080:8080 $(IMAGE_NAME):$(IMAGE_TAG)

## 停止并删除容器
docker-stop:
	docker stop $(CONTAINER) && docker rm $(CONTAINER)

## 清理所有构建产物
clean:
	./mvnw clean -B
	-docker rmi $(IMAGE_NAME):$(IMAGE_TAG) 2>/dev/null || true
