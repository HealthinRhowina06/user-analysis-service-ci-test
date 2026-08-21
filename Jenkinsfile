pipeline {
    agent any

    tools {
        maven 'Maven-3.9.16'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Commit Details') {
            steps {
                bat '''
                echo ===== COMMIT DETAILS =====
                git log -1 --pretty=format:"Author: %%an"
                echo.
                git log -1 --pretty=format:"Email: %%ae"
                echo.
                git log -1 --pretty=format:"Commit ID: %%H"
                echo.
                git log -1 --pretty=format:"Message: %%s"
                echo.
                git log -1 --pretty=format:"Date: %%ad"
                echo.
                echo ==========================
                '''
            }
        }
        stage('Build') {
            steps {
                bat 'mvn clean compile'
            }
        }

        stage('Test') {
            steps {
                bat 'mvn test'
            }
        }

        stage('Docker Build') {
            when {
                branch 'main'
            }
            steps {
                bat 'docker build -t user-analysis-service:%BUILD_NUMBER% .'
            }
        }
    }

    post {

        always {
            junit allowEmptyResults: true,
                  testResults: 'target/surefire-reports/*.xml'
        }

        success {
            echo 'CI PIPELINE SUCCESS'
        }

        failure {
            echo 'TEST/BUILD FAILED - MERGE AND DEPLOYMENT MUST STOP'
        }
    }
}