pipeline {
    agent any

    environment {
        IMAGE_NAME = "exam-online"
        CONTAINER_NAME = "exam-online-container"
        // 建议增加一个变量，方便清理旧镜像
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') { // 将编译和测试合并或分开
            steps {
                // 真正的自动化测试：去掉 -DskipTests
                // 如果 Jenkins 报错找不到 mvn，请写绝对路径或在全局工具配置里设置
                sh 'mvn clean package'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
                // 给镜像打个 latest 标签，方便调试
                sh "docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest"
            }
        }

        stage('Run Container') {
            steps {
                sh """
                docker stop ${CONTAINER_NAME} || true
                docker rm ${CONTAINER_NAME} || true

                docker run -d \
                -p 14808:8080 \
                --name ${CONTAINER_NAME} \
                -v /www/wwwroot/Exam_online/config/application-prod.yml:/app/config/application-prod.yml \
                ${IMAGE_NAME}:${IMAGE_TAG}
                """
            }
        }

        // 建议增加清理环节，防止服务器硬盘被旧镜像撑爆
        stage('Cleanup') {
            steps {
                sh "docker image prune -f"
            }
        }
    }
}