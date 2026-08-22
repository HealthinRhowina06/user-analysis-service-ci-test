pipeline {
    agent any

    tools {
        jdk 'JDK-25'
        maven 'Maven-3.9.16'
    }

    environment {
        DOCKER_EXE = 'C:\\Users\\hrhow\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin\\docker.exe'

        IMAGE_NAME = 'vijayjeyam/prodmexaanalysis'
        IMAGE_TAG  = 'latest'

        SERVER_USER = 'mani'
        SERVER_IP   = '122.165.70.116'

        SERVER_PATH = '/home/mani/user-analysis-service'

        SSH_KEY = 'C:\\Users\\hrhow\\.ssh\\id_ed25519'
    }

    triggers {
        githubPush()
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
                echo ========================================
                echo            COMMIT DETAILS
                echo ========================================

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

                echo ========================================
                '''
            }
        }

        stage('Run Unit Tests') {
            steps {
                bat '''
                echo ========================================
                echo           RUNNING UNIT TESTS
                echo ========================================

                mvn test -Dmaven.test.failure.ignore=true
                '''
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

                    echo '========================================'
                    echo '              TEST SUMMARY'
                    echo '========================================'

                    echo "Total Tests  : ${total}"
                    echo "Passed       : ${passed}"
                    echo "Failed       : ${failed}"
                    echo "Skipped      : ${skipped}"
                    echo "Pass Percent : ${String.format('%.2f', percentage)}%"

                    echo '========================================'

                    if (percentage < 80) {

                        error(
                            "TEST QUALITY GATE FAILED. " +
                            "Only ${String.format('%.2f', percentage)}% tests passed. " +
                            "Minimum required is 80%."
                        )
                    }

                    echo 'TEST QUALITY GATE PASSED'
                    echo 'PASS PERCENTAGE IS 80% OR ABOVE'
                }
            }
        }

        stage('Maven Build') {
            steps {
                bat '''
                echo ========================================
                echo             MAVEN BUILD
                echo ========================================

                mvn clean package -DskipTests
                '''
            }
        }

        stage('Docker Build') {
            steps {
                bat '''
                echo ========================================
                echo            DOCKER BUILD
                echo ========================================

                "%DOCKER_EXE%" build ^
                -t %IMAGE_NAME%:%BUILD_NUMBER% ^
                -t %IMAGE_NAME%:%IMAGE_TAG% ^
                .
                '''
            }
        }

        stage('Docker Save') {
            steps {
                bat '''
                echo ========================================
                echo             DOCKER SAVE
                echo ========================================

                if exist app-image.tar (
                    del /F /Q app-image.tar
                )

                "%DOCKER_EXE%" save ^
                -o app-image.tar ^
                %IMAGE_NAME%:%IMAGE_TAG%

                dir app-image.tar
                '''
            }
        }

        stage('Check SSH Connection') {
            steps {
                bat '''
                echo ========================================
                echo          CHECK SSH CONNECTION
                echo ========================================

                ssh ^
                -o BatchMode=yes ^
                -o StrictHostKeyChecking=no ^
                -i "%SSH_KEY%" ^
                %SERVER_USER%@%SERVER_IP% ^
                "echo SSH CONNECTION SUCCESS"
                '''
            }
        }

        stage('Transfer Docker Image') {
            steps {
                bat '''
                echo ========================================
                echo       TRANSFER IMAGE TO SERVER
                echo ========================================

                scp ^
                -o BatchMode=yes ^
                -o StrictHostKeyChecking=no ^
                -i "%SSH_KEY%" ^
                app-image.tar ^
                %SERVER_USER%@%SERVER_IP%:%SERVER_PATH%/app-image.tar

                echo IMAGE TRANSFER COMPLETED
                '''
            }
        }

        stage('Docker Load On Server') {
            steps {
                bat '''
                echo ========================================
                echo        LOAD IMAGE ON SERVER
                echo ========================================

                ssh ^
                -o BatchMode=yes ^
                -o StrictHostKeyChecking=no ^
                -i "%SSH_KEY%" ^
                %SERVER_USER%@%SERVER_IP% ^
                "cd %SERVER_PATH% && docker load -i app-image.tar"
                '''
            }
        }

        stage('Check Docker Network') {
            steps {
                bat '''
                echo ========================================
                echo          CHECK DOCKER NETWORK
                echo ========================================

                ssh ^
                -o BatchMode=yes ^
                -o StrictHostKeyChecking=no ^
                -i "%SSH_KEY%" ^
                %SERVER_USER%@%SERVER_IP% ^
                "docker network inspect prodmexa >/dev/null 2>&1 || docker network create prodmexa"
                '''
            }
        }

        stage('Docker Compose Deploy') {
            steps {
                bat '''
                echo ========================================
                echo          DOCKER COMPOSE DEPLOY
                echo ========================================

                ssh ^
                -o BatchMode=yes ^
                -o StrictHostKeyChecking=no ^
                -i "%SSH_KEY%" ^
                %SERVER_USER%@%SERVER_IP% ^
                "cd %SERVER_PATH% && docker compose -f docker_env/prod.yml up -d"
                '''
            }
        }

        stage('Verify Container') {
            steps {
                bat '''
                echo ========================================
                echo          VERIFY CONTAINER
                echo ========================================

                ssh ^
                -o BatchMode=yes ^
                -o StrictHostKeyChecking=no ^
                -i "%SSH_KEY%" ^
                %SERVER_USER%@%SERVER_IP% ^
                "docker ps --filter name=prodmexaanalysis"

                echo ========================================
                '''
            }
        }
    }

    post {

        success {

            echo '============================================'
            echo '          CI/CD PIPELINE SUCCESS'
            echo '============================================'
            echo 'Tests >= 80%        : PASSED'
            echo 'Maven Build         : SUCCESS'
            echo 'Docker Build        : SUCCESS'
            echo 'Docker Save         : SUCCESS'
            echo 'Server Transfer     : SUCCESS'
            echo 'Docker Load         : SUCCESS'
            echo 'Docker Compose      : SUCCESS'
            echo 'Container Deployment: SUCCESS'
            echo '============================================'
        }

        failure {

            echo '============================================'
            echo '           CI/CD PIPELINE FAILED'
            echo '============================================'
            echo 'Check the failed Jenkins stage.'
            echo 'If tests are below 80%, deployment is stopped.'
            echo 'If deployment fails, check SSH/Docker logs.'
            echo '============================================'
        }

        always {

            archiveArtifacts(
                artifacts: 'target/surefire-reports/**',
                allowEmptyArchive: true
            )
        }
    }
}