pipeline {
    agent any

    tools {
        jdk 'JDK-25'
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

        stage('Run Unit Tests') {
            steps {
                script {
                    bat 'mvn test -Dmaven.test.failure.ignore=true'
                }
            }
        }

        stage('80 Percent Test Gate') {
            steps {
                script {
                    def result = junit(
                        allowEmptyResults: false,
                        testResults: 'target/surefire-reports/*.xml'
                    )

                    def total = result.totalCount
                    def failed = result.failCount
                    def skipped = result.skipCount
                    def passed = total - failed - skipped

                    def percentage = total > 0
                        ? (passed * 100.0 / total)
                        : 0

                    echo "===== TEST SUMMARY ====="
                    echo "Total Tests   : ${total}"
                    echo "Passed        : ${passed}"
                    echo "Failed        : ${failed}"
                    echo "Skipped       : ${skipped}"
                    echo "Pass Percent  : ${String.format('%.2f', percentage)}%"
                    echo "========================"

                    if (percentage < 80) {
                        error("TEST QUALITY GATE FAILED. Pass percentage is ${String.format('%.2f', percentage)}%")
                    }

                    echo "TEST QUALITY GATE PASSED"
                }
            }
        }

        stage('Build') {
            steps {
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Docker Build') {
            steps {
                bat '"C:\\Users\\hrhow\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin\\docker.exe" build -t user-analysis-service:latest .'
            }
        }
    }

    post {
        success {
            echo 'CI PIPELINE SUCCESS - TEST PASS RATE IS 80% OR ABOVE'
        }

        failure {
            echo 'CI PIPELINE FAILED - CHECK TEST RESULT / FAILURE REASON'
        }
    }
}