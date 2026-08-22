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

        SSH_KEY = 'C:\\Windows\\System32\\config\\systemprofile\\.ssh\\id_ed25519'

        SOURCE_BRANCH = 'feature/ci-test'

        COMMIT_AUTHOR  = ''
        COMMIT_EMAIL   = ''
        COMMIT_ID      = ''
        COMMIT_MESSAGE = ''
        COMMIT_DATE    = ''
        BUILD_TIME     = ''

        TEST_TOTAL     = '0'
        TEST_PASSED    = '0'
        TEST_FAILED    = '0'
        TEST_SKIPPED   = '0'
        TEST_PERCENT   = '0.00'

        DEV_SAVE_RESULT    = 'NOT SAVED'
        DEPLOYMENT_RESULT  = 'NOT DEPLOYED'
    }

    triggers {
        githubPush()
    }

    stages {

        // =====================================================
        // 1. CHECKOUT
        // =====================================================

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // =====================================================
        // 2. COMMIT DETAILS
        // =====================================================

        stage('Commit Details') {
            steps {

                bat '''
                git log -1 --pretty=format:"%%an" > commit_author.txt
                git log -1 --pretty=format:"%%ae" > commit_email.txt
                git log -1 --pretty=format:"%%H"  > commit_id.txt
                git log -1 --pretty=format:"%%s"  > commit_message.txt
                git log -1 --date=iso --pretty=format:"%%ad" > commit_date.txt

                powershell -NoProfile -Command "Get-Date -Format 'yyyy-MM-dd HH:mm:ss'" > build_time.txt
                '''

                script {

                    env.COMMIT_AUTHOR =
                        readFile('commit_author.txt').trim()

                    env.COMMIT_EMAIL =
                        readFile('commit_email.txt').trim()

                    env.COMMIT_ID =
                        readFile('commit_id.txt').trim()

                    env.COMMIT_MESSAGE =
                        readFile('commit_message.txt').trim()

                    env.COMMIT_DATE =
                        readFile('commit_date.txt').trim()

                    env.BUILD_TIME =
                        readFile('build_time.txt').trim()

                    echo '========================================'
                    echo '             COMMIT DETAILS'
                    echo '========================================'
                    echo "Author      : ${env.COMMIT_AUTHOR}"
                    echo "Email       : ${env.COMMIT_EMAIL}"
                    echo "Commit ID   : ${env.COMMIT_ID}"
                    echo "Message     : ${env.COMMIT_MESSAGE}"
                    echo "Commit Date : ${env.COMMIT_DATE}"
                    echo "Build Time  : ${env.BUILD_TIME}"
                    echo '========================================'
                }
            }
        }

        // =====================================================
        // 3. RUN UNIT TESTS
        // =====================================================

        stage('Run Unit Tests') {
            steps {

                bat '''
                echo ========================================
                echo             RUN UNIT TESTS
                echo ========================================

                mvn test -Dmaven.test.failure.ignore=true
                '''
            }
        }

        // =====================================================
        // 4. READ TEST RESULT + 80% QUALITY GATE
        // =====================================================

        stage('80 Percent Test Gate') {
            steps {

                // Publish test report in Jenkins
                junit(
                    allowEmptyResults: false,
                    testResults: 'target/surefire-reports/*.xml'
                )

                // Read actual Maven Surefire XML values.
                // This avoids the previous Jenkins 0/0 issue.
                bat '''
                powershell -NoProfile -Command ^
                "$files = Get-ChildItem 'target\\surefire-reports\\TEST-*.xml'; ^
                $total = 0; ^
                $failures = 0; ^
                $errors = 0; ^
                $skipped = 0; ^
                foreach ($f in $files) { ^
                    [xml]$xml = Get-Content $f.FullName; ^
                    $suite = $xml.testsuite; ^
                    $total += [int]$suite.tests; ^
                    $failures += [int]$suite.failures; ^
                    $errors += [int]$suite.errors; ^
                    $skipped += [int]$suite.skipped; ^
                }; ^
                $failedTotal = $failures + $errors; ^
                Set-Content -Path 'test-summary.txt' -Value ($total.ToString() + ',' + $failedTotal.ToString() + ',' + $skipped.ToString())"
                '''

                script {

                    def summary =
                        readFile('test-summary.txt').trim()

                    def parts =
                        summary.split(',')

                    def total =
                        parts[0].trim().toInteger()

                    def failed =
                        parts[1].trim().toInteger()

                    def skipped =
                        parts[2].trim().toInteger()

                    def passed =
                        total - failed - skipped

                    def percentage =
                        total > 0
                            ? (passed * 100.0 / total)
                            : 0.0

                    env.TEST_TOTAL =
                        total.toString()

                    env.TEST_PASSED =
                        passed.toString()

                    env.TEST_FAILED =
                        failed.toString()

                    env.TEST_SKIPPED =
                        skipped.toString()

                    env.TEST_PERCENT =
                        String.format('%.2f', percentage)

                    echo '========================================'
                    echo '              TEST SUMMARY'
                    echo '========================================'
                    echo "Total Tests  : ${env.TEST_TOTAL}"
                    echo "Passed       : ${env.TEST_PASSED}"
                    echo "Failed       : ${env.TEST_FAILED}"
                    echo "Skipped      : ${env.TEST_SKIPPED}"
                    echo "Pass Percent : ${env.TEST_PERCENT}%"
                    echo '========================================'

                    currentBuild.description =
                        "Tests: ${passed}/${total} | ${env.TEST_PERCENT}%"

                    // No tests itself is treated as failure.
                    if (total == 0) {

                        echo '========================================'
                        echo '          NO TEST RESULTS FOUND'
                        echo '========================================'
                        echo 'DEV BRANCH WILL NOT BE UPDATED'
                        echo 'DEPLOYMENT WILL NOT START'
                        echo '========================================'

                        error(
                            'QUALITY GATE FAILED - No tests were found.'
                        )
                    }

                    if (percentage < 80) {

                        echo '========================================'
                        echo '        TEST QUALITY GATE FAILED'
                        echo '========================================'
                        echo 'Required : 80%'
                        echo "Actual   : ${env.TEST_PERCENT}%"
                        echo 'DEV BRANCH WILL NOT BE UPDATED'
                        echo 'DEPLOYMENT WILL NOT START'
                        echo '========================================'

                        error(
                            "TEST QUALITY GATE FAILED - " +
                            "${env.TEST_PERCENT}% passed. " +
                            "Minimum required is 80%."
                        )
                    }

                    echo '========================================'
                    echo '        TEST QUALITY GATE PASSED'
                    echo '========================================'
                    echo "Pass Percentage : ${env.TEST_PERCENT}%"
                    echo 'Code can now be saved to DEV.'
                    echo '========================================'
                }
            }
        }

        // =====================================================
        // 5. SAVE ONLY PASSED CODE TO DEV
        // =====================================================

        stage('Save Passed Code To Dev') {
            steps {

                withCredentials([
                    usernamePassword(
                        credentialsId: 'token',
                        usernameVariable: 'GIT_USER',
                        passwordVariable: 'GIT_TOKEN'
                    )
                ]) {

                    bat '''
                    echo ========================================
                    echo        SAVE PASSED CODE TO DEV
                    echo ========================================

                    echo Fetching current DEV branch...

                    git fetch ^
                    https://%GIT_USER%:%GIT_TOKEN%@github.com/HealthinRhowina06/user-analysis-service-ci-test.git ^
                    dev

                    echo.
                    echo Pushing tested commit to DEV...

                    git push ^
                    https://%GIT_USER%:%GIT_TOKEN%@github.com/HealthinRhowina06/user-analysis-service-ci-test.git ^
                    HEAD:dev

                    echo.
                    echo ========================================
                    echo PASSED CODE SAVED TO DEV SUCCESSFULLY
                    echo ========================================
                    '''
                }

                script {
                    env.DEV_SAVE_RESULT = 'SAVED TO DEV'
                }
            }
        }

        // =====================================================
        // 6. MAVEN BUILD
        // =====================================================

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

        // =====================================================
        // 7. DOCKER BUILD
        // =====================================================

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

        // =====================================================
        // 8. DOCKER SAVE
        // =====================================================

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

        // =====================================================
        // 9. CHECK SSH CONNECTION
        // =====================================================

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

                echo ========================================
                '''
            }
        }

        // =====================================================
        // 10. TRANSFER DOCKER IMAGE
        // =====================================================

        stage('Transfer Docker Image') {
            steps {

                bat '''
                echo ========================================
                echo         TRANSFER DOCKER IMAGE
                echo ========================================

                scp ^
                -o BatchMode=yes ^
                -o StrictHostKeyChecking=no ^
                -i "%SSH_KEY%" ^
                app-image.tar ^
                %SERVER_USER%@%SERVER_IP%:%SERVER_PATH%/app-image.tar

                echo ========================================
                echo IMAGE TRANSFER COMPLETED
                echo ========================================
                '''
            }
        }

        // =====================================================
        // 11. DOCKER LOAD ON SERVER
        // =====================================================

        stage('Docker Load On Server') {
            steps {

                bat '''
                echo ========================================
                echo         DOCKER LOAD ON SERVER
                echo ========================================

                ssh ^
                -o BatchMode=yes ^
                -o StrictHostKeyChecking=no ^
                -i "%SSH_KEY%" ^
                %SERVER_USER%@%SERVER_IP% ^
                "cd %SERVER_PATH% && docker load -i app-image.tar"

                echo ========================================
                echo DOCKER IMAGE LOADED ON SERVER
                echo ========================================
                '''
            }
        }

        // =====================================================
        // 12. CHECK DOCKER NETWORK
        // =====================================================

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

                echo ========================================
                echo DOCKER NETWORK READY
                echo ========================================
                '''
            }
        }

        // =====================================================
        // 13. DOCKER COMPOSE DEPLOY
        // =====================================================

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
                "cd %SERVER_PATH% && docker compose -f docker_env/prod.yml up -d --force-recreate"

                echo ========================================
                echo DOCKER COMPOSE DEPLOY COMPLETED
                echo ========================================
                '''
            }
        }

        // =====================================================
        // 14. VERIFY CONTAINER
        // =====================================================

        stage('Verify Container') {
            steps {

                bat '''
                echo ========================================
                echo            VERIFY CONTAINER
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

        // =====================================================
        // 15. HEALTH CHECK
        // =====================================================

        stage('Health Check') {
            steps {

                bat '''
                echo ========================================
                echo              HEALTH CHECK
                echo ========================================

                ssh ^
                -o BatchMode=yes ^
                -o StrictHostKeyChecking=no ^
                -i "%SSH_KEY%" ^
                %SERVER_USER%@%SERVER_IP% ^
                "for i in 1 2 3 4 5 6; do RESPONSE=$(curl -s http://localhost:9035/actuator/health); echo $RESPONSE; echo $RESPONSE | grep -q \\"UP\\" && exit 0; sleep 5; done; echo APPLICATION HEALTH CHECK FAILED; docker logs --tail 50 prodmexaanalysis; exit 1"

                echo ========================================
                echo APPLICATION HEALTH CHECK PASSED
                echo ========================================
                '''

                script {
                    env.DEPLOYMENT_RESULT = 'DEPLOYED - HEALTH UP'
                }
            }
        }
    }

    // =========================================================
    // POST ACTIONS
    // =========================================================

    post {

        success {

            echo '============================================'
            echo '            CI/CD PIPELINE SUCCESS'
            echo '============================================'
            echo 'Unit Tests          : COMPLETED'
            echo '80 Percent Gate     : PASSED'
            echo 'Code Saved To Dev   : SUCCESS'
            echo 'Maven Build         : SUCCESS'
            echo 'Docker Build        : SUCCESS'
            echo 'Docker Save         : SUCCESS'
            echo 'SSH Connection      : SUCCESS'
            echo 'Server Transfer     : SUCCESS'
            echo 'Docker Load         : SUCCESS'
            echo 'Docker Network      : READY'
            echo 'Docker Compose      : SUCCESS'
            echo 'Container           : RUNNING'
            echo 'Application Health  : UP'
            echo '============================================'
        }

        failure {

            echo '============================================'
            echo '             CI/CD PIPELINE FAILED'
            echo '============================================'
            echo "Author       : ${env.COMMIT_AUTHOR}"
            echo "Email        : ${env.COMMIT_EMAIL}"
            echo "Commit       : ${env.COMMIT_ID}"
            echo "Test Percent : ${env.TEST_PERCENT}%"
            echo "Dev Result   : ${env.DEV_SAVE_RESULT}"
            echo "Deployment   : ${env.DEPLOYMENT_RESULT}"
            echo ''
            echo 'Check the failed stage above.'
            echo 'If tests are below 80%, DEV is NOT updated.'
            echo 'If tests are below 80%, deployment stops.'
            echo 'If SSH fails, check Jenkins SYSTEM SSH key.'
            echo 'If deployment fails, check Docker logs.'
            echo '============================================'
        }

        always {

            script {

                def finalResult =
                    currentBuild.currentResult ?: 'UNKNOWN'

                def safeMessage =
                    (env.COMMIT_MESSAGE ?: '')
                        .replace('"', '""')
                        .replace('\r', ' ')
                        .replace('\n', ' ')

                def safeAuthor =
                    (env.COMMIT_AUTHOR ?: '')
                        .replace('"', '""')

                def safeEmail =
                    (env.COMMIT_EMAIL ?: '')
                        .replace('"', '""')

                def header =
                    '"Build Number",' +
                    '"Date Time",' +
                    '"Branch",' +
                    '"Author",' +
                    '"Email",' +
                    '"Commit ID",' +
                    '"Commit Message",' +
                    '"Commit Date",' +
                    '"Total Tests",' +
                    '"Passed",' +
                    '"Failed",' +
                    '"Skipped",' +
                    '"Pass Percentage",' +
                    '"Dev Save Result",' +
                    '"Pipeline Result",' +
                    '"Deployment Result",' +
                    '"Build URL"'

                def row =
                    "\"${env.BUILD_NUMBER}\"," +
                    "\"${env.BUILD_TIME ?: ''}\"," +
                    "\"${env.SOURCE_BRANCH}\"," +
                    "\"${safeAuthor}\"," +
                    "\"${safeEmail}\"," +
                    "\"${env.COMMIT_ID ?: ''}\"," +
                    "\"${safeMessage}\"," +
                    "\"${env.COMMIT_DATE ?: ''}\"," +
                    "\"${env.TEST_TOTAL ?: '0'}\"," +
                    "\"${env.TEST_PASSED ?: '0'}\"," +
                    "\"${env.TEST_FAILED ?: '0'}\"," +
                    "\"${env.TEST_SKIPPED ?: '0'}\"," +
                    "\"${env.TEST_PERCENT ?: '0.00'}%\"," +
                    "\"${env.DEV_SAVE_RESULT}\"," +
                    "\"${finalResult}\"," +
                    "\"${env.DEPLOYMENT_RESULT}\"," +
                    "\"${env.BUILD_URL ?: ''}\""

                def historyFile =
                    "ci-history-${env.BUILD_NUMBER}.csv"

                writeFile(
                    file: historyFile,
                    text: header + "\r\n" + row + "\r\n"
                )

                echo '============================================'
                echo '              CI HISTORY RECORD'
                echo '============================================'
                echo "Build No     : ${env.BUILD_NUMBER}"
                echo "Date         : ${env.BUILD_TIME}"
                echo "Author       : ${env.COMMIT_AUTHOR}"
                echo "Email        : ${env.COMMIT_EMAIL}"
                echo "Commit ID    : ${env.COMMIT_ID}"
                echo "Message      : ${env.COMMIT_MESSAGE}"
                echo "Branch       : ${env.SOURCE_BRANCH}"
                echo "Total Tests  : ${env.TEST_TOTAL}"
                echo "Passed       : ${env.TEST_PASSED}"
                echo "Failed       : ${env.TEST_FAILED}"
                echo "Skipped      : ${env.TEST_SKIPPED}"
                echo "Pass Rate    : ${env.TEST_PERCENT}%"
                echo "Dev Result   : ${env.DEV_SAVE_RESULT}"
                echo "Pipeline     : ${finalResult}"
                echo "Deployment   : ${env.DEPLOYMENT_RESULT}"
                echo '============================================'
            }

            archiveArtifacts(
                artifacts: 'target/surefire-reports/**,ci-history-*.csv',
                allowEmptyArchive: true
            )
        }
    }
}