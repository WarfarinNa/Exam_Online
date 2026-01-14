# 使用 JDK 运行环境
FROM eclipse-temurin:21-jre

# 容器内工作目录
WORKDIR /app

# 拷贝 jar 包
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
