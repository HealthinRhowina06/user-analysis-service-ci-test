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

        SERVER_IP   = '122.165.70.116'
        SERVER_PATH = '/home/mani/user-analysis-service'
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
                echo             COMMIT DETAILS
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
                echo            RUN UNIT TESTS
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

                    def total   = result.totalCount
                    def failed  = result.failCount
                    def skipped = result.skipCount
                    def passed  = total - failed - skipped

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

                    currentBuild.description =
                        "Tests: ${passed}/${total} | ${String.format('%.2f', percentage)}%"

                    if (percentage < 80) {
                        error(
                            "TEST QUALITY GATE FAILED - " +
                            "${String.format('%.2f', percentage)}% passed. " +
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
                echo              MAVEN BUILD
                echo ========================================

                mvn package -DskipTests
                '''
            }
        }

        stage('Docker Build') {
            steps {
                bat '''
                echo ========================================
                echo              DOCKER BUILD
                echo ========================================

                "%DOCKER_EXE%" build ^
                -t %IMAGE_NAME%:%BUILD_NUMBER% ^
                -t %IMAGE_NAME%:%IMAGE_TAG% ^
                .

                echo ========================================
                echo DOCKER IMAGE CREATED
                echo %IMAGE_NAME%:%BUILD_NUMBER%
                echo %IMAGE_NAME%:%IMAGE_TAG%
                echo ========================================
                '''
            }
        }

        stage('Docker Save') {
            steps {
                bat '''
                echo ========================================
                echo              DOCKER SAVE
                echo ========================================

                if exist app-image.tar (
                    del /F /Q app-image.tar
                )

                "%DOCKER_EXE%" save ^
                -o app-image.tar ^
                %IMAGE_NAME%:%IMAGE_TAG%

                dir app-image.tar

                echo ========================================
                echo DOCKER IMAGE SAVED AS app-image.tar
                echo ========================================
                '''
            }
        }

        stage('Check SSH Connection') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'analysis-server-ssh-key',
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )
                ]) {
                    bat '''
                    echo ========================================
                    echo          CHECK SSH CONNECTION
                    echo ========================================

                    ssh ^
                    -o BatchMode=yes ^
                    -o StrictHostKeyChecking=no ^
                    -i "%SSH_KEY_FILE%" ^
                    %SSH_USER%@%SERVER_IP% ^
                    "echo SSH CONNECTION SUCCESS"
                    '''
                }
            }
        }

        stage('Transfer Docker Image') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'analysis-server-ssh-key',
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )
                ]) {
                    bat '''
                    echo ========================================
                    echo        TRANSFER DOCKER IMAGE
                    echo ========================================

                    scp ^
                    -o BatchMode=yes ^
                    -o StrictHostKeyChecking=no ^
                    -i "%SSH_KEY_FILE%" ^
                    app-image.tar ^
                    %SSH_USER%@%SERVER_IP%:%SERVER_PATH%/app-image.tar

                    echo ========================================
                    echo IMAGE TRANSFER COMPLETED
                    echo ========================================
                    '''
                }
            }
        }

        stage('Docker Load On Server') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'analysis-server-ssh-key',
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )
                ]) {
                    bat '''
                    echo ========================================
                    echo         DOCKER LOAD ON SERVER
                    echo ========================================

                    ssh ^
                    -o BatchMode=yes ^
                    -o StrictHostKeyChecking=no ^
                    -i "%SSH_KEY_FILE%" ^
                    %SSH_USER%@%SERVER_IP% ^
                    "cd %SERVER_PATH% && docker load -i app-image.tar"
                    '''
                }
            }
        }

        stage('Check Docker Network') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'analysis-server-ssh-key',
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )
                ]) {
                    bat '''
                    echo ========================================
                    echo          CHECK DOCKER NETWORK
                    echo ========================================

                    ssh ^
                    -o BatchMode=yes ^
                    -o StrictHostKeyChecking=no ^
                    -i "%SSH_KEY_FILE%" ^
                    %SSH_USER%@%SERVER_IP% ^
                    "docker network inspect prodmexa >/dev/null 2>&1 || docker network create prodmexa"
                    '''
                }
            }
        }

        stage('Docker Compose Deploy') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'analysis-server-ssh-key',
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )
                ]) {
                    bat '''
                    echo ========================================
                    echo          DOCKER COMPOSE DEPLOY
                    echo ========================================

                    ssh ^
                    -o BatchMode=yes ^
                    -o StrictHostKeyChecking=no ^
                    -i "%SSH_KEY_FILE%" ^
                    %SSH_USER%@%SERVER_IP% ^
                    "cd %SERVER_PATH% && docker compose -f docker_env/prod.yml up -d --force-recreate"
                    '''
                }
            }
        }

        stage('Verify Container') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'analysis-server-ssh-key',
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )
                ]) {
                    bat '''
                    echo ========================================
                    echo           VERIFY CONTAINER
                    echo ========================================

                    ssh ^
                    -o BatchMode=yes ^
                    -o StrictHostKeyChecking=no ^
                    -i "%SSH_KEY_FILE%" ^
                    %SSH_USER%@%SERVER_IP% ^
                    "docker ps --filter name=prodmexaanalysis"

                    echo ========================================
                    '''
                }
            }
        }

        stage('Health Check') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'analysis-server-ssh-key',
                        keyFileVariable: 'SSH_KEY_FILE',
                        usernameVariable: 'SSH_USER'
                    )
                ]) {

                    bat '''
                    echo ========================================
                    echo             HEALTH CHECK
                    echo ========================================

                    ssh ^
                    -o BatchMode=yes ^
                    -o StrictHostKeyChecking=no ^
                    -i "%SSH_KEY_FILE%" ^
                    %SSH_USER%@%SERVER_IP% ^
                    "for i in 1 2 3 4 5 6; do RESPONSE=$(curl -s http://localhost:9035/actuator/health); echo $RESPONSE; echo $RESPONSE | grep -q \\"UP\\" && exit 0; sleep 5; done; echo APPLICATION HEALTH CHECK FAILED; docker logs --tail 50 prodmexaanalysis; exit 1"
                    '''
                }
            }
        }
    }

    post {

        success {
            echo '============================================'
            echo '            CI/CD PIPELINE SUCCESS'
            echo '============================================'
            echo 'Unit Tests          : COMPLETED'
            echo '80 Percent Gate     : PASSED'
            echo 'Maven Build         : SUCCESS'
            echo 'Docker Build        : SUCCESS'
            echo 'Docker Save         : SUCCESS'
            echo 'SSH Connection      : SUCCESS'
            echo 'Server Transfer     : SUCCESS'
            echo 'Docker Load         : SUCCESS'
            echo 'Docker Compose      : SUCCESS'
            echo 'Container           : RUNNING'
            echo 'Application Health  : UP'
            echo '============================================'
        }

        failure {
            echo '============================================'
            echo '             CI/CD PIPELINE FAILED'
            echo '============================================'
            echo 'Check the failed stage above.'
            echo 'If tests are below 80%, deployment stops.'
            echo 'If SSH fails, check Jenkins SSH credential.'
            echo 'If deployment fails, check Docker logs.'
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