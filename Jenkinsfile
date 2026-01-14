pipeline {
    agent any

    environment {
        IMAGE_NAME = "springboot-demo"
        CONTAINER_NAME = "springboot-demo-container"
    }

    stages {
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        stage('Build Jar') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t $IMAGE_NAME:$BUILD_NUMBER .'
            }
        }

        stage('Run Container') {
            steps {
                sh '''
                docker stop $CONTAINER_NAME || true
                docker rm $CONTAINER_NAME || true

                docker run -d \
                -p 8081:8080 \
                --name $CONTAINER_NAME \
                $IMAGE_NAME:$BUILD_NUMBER
                '''
            }
        }
    }
}
