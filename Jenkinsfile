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
                        error(
                            "TEST QUALITY GATE FAILED. " +
                            "Pass percentage is ${String.format('%.2f', percentage)}%"
                        )
                    }

                    echo 'TEST QUALITY GATE PASSED'
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
                bat '''
                "C:\\Users\\hrhow\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin\\docker.exe" build ^
                -t user-analysis-service:latest .
                '''
            }
        }

        stage('Tag Docker Image') {
            steps {
                bat '''
                "C:\\Users\\hrhow\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin\\docker.exe" tag ^
                user-analysis-service:latest ^
                vijayjeyam/prodmexaanalysis:latest
                '''
            }
        }

        stage('Save Docker Image') {
            steps {
                bat '''
                if exist app-image.tar del /F /Q app-image.tar

                "C:\\Users\\hrhow\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin\\docker.exe" save ^
                -o app-image.tar ^
                vijayjeyam/prodmexaanalysis:latest
                '''
            }
        }

        stage('Copy Image To Server') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'analysis-server-ssh',
                        usernameVariable: 'SSH_USER',
                        passwordVariable: 'SSH_PASS'
                    )
                ]) {
                    bat '''
                    pscp -batch ^
                    -pw "%SSH_PASS%" ^
                    app-image.tar ^
                    %SSH_USER%@122.165.70.116:/home/mani/user-analysis-service/app-image.tar
                    '''
                }
            }
        }

        stage('Deploy On Server') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'analysis-server-ssh',
                        usernameVariable: 'SSH_USER',
                        passwordVariable: 'SSH_PASS'
                    )
                ]) {
                    bat '''
                    plink -batch ^
                    -pw "%SSH_PASS%" ^
                    %SSH_USER%@122.165.70.116 ^
                    "cd /home/mani/user-analysis-service && docker load -i app-image.tar && docker compose -f docker_env/prod.yml up -d"
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'analysis-server-ssh',
                        usernameVariable: 'SSH_USER',
                        passwordVariable: 'SSH_PASS'
                    )
                ]) {
                    bat '''
                    plink -batch ^
                    -pw "%SSH_PASS%" ^
                    %SSH_USER%@122.165.70.116 ^
                    "docker ps --filter name=prodmexaanalysis"
                    '''
                }
            }
        }
    }

    post {

        success {
            echo '========================================='
            echo 'CI/CD PIPELINE SUCCESS'
            echo 'TEST PASS RATE IS 80% OR ABOVE'
            echo 'DOCKER IMAGE BUILT'
            echo 'IMAGE COPIED TO SERVER'
            echo 'APPLICATION DEPLOYED'
            echo '========================================='
        }

        failure {
            echo '========================================='
            echo 'CI/CD PIPELINE FAILED'
            echo 'CHECK TEST RESULT / BUILD / DOCKER / DEPLOYMENT ERROR'
            echo '========================================='
        }
    }
}