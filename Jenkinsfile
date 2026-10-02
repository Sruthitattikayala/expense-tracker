pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code from GitHub...'
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                echo 'Compiling the application with Maven...'
                script {
                    if (isUnix()) {
                        sh 'mvn clean compile'
                    } else {
                        bat 'mvn clean compile'
                    }
                }
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                script {
                    if (isUnix()) {
                        sh 'mvn test'
                    } else {
                        bat 'mvn test'
                    }
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging the application into a JAR...'
                script {
                    if (isUnix()) {
                        sh 'mvn package -DskipTests'
                    } else {
                        bat 'mvn package -DskipTests'
                    }
                }
            }
        }

        stage('Archive Artifact') {
            steps {
                echo 'Archiving built JAR artifact...'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Build Docker Image') {
            steps {
                echo 'Building Docker image siri57/expense-tracker:1.2...'
                script {
                    if (isUnix()) {
                        sh 'docker build -t siri57/expense-tracker:1.2 .'
                    } else {
                        bat 'docker build -t siri57/expense-tracker:1.2 .'
                    }
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                echo 'Logging in and pushing Docker image to Docker Hub...'
                withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    script {
                        if (isUnix()) {
                            sh 'echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin'
                            sh 'docker push siri57/expense-tracker:1.2'
                        } else {
                            bat 'echo %DOCKER_PASS%| docker login -u %DOCKER_USER% --password-stdin'
                            bat 'docker push siri57/expense-tracker:1.2'
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'Jenkins CI/CD Pipeline executed successfully! JAR is archived and Docker image is pushed to Docker Hub.'
        }
        failure {
            echo 'Jenkins CI/CD Pipeline failed. Please check the console output for errors.'
        }
    }
}
