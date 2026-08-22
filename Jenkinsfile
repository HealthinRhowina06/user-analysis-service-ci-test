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
        // 2. CAPTURE COMMIT DETAILS
        // =====================================================
        stage('Commit Details') {
            steps {

                bat '''
                @echo off

                git log -1 --pretty=format:%%an > commit_author.txt
                git log -1 --pretty=format:%%ae > commit_email.txt
                git log -1 --pretty=format:%%H > commit_id.txt
                git log -1 --pretty=format:%%s > commit_message.txt
                git log -1 --date=iso --pretty=format:%%ad > commit_date.txt

                powershell -NoProfile -Command "Get-Date -Format 'yyyy-MM-dd HH:mm:ss'" > build_time.txt
                '''

                script {

                    def author =
                        readFile('commit_author.txt').trim()

                    def email =
                        readFile('commit_email.txt').trim()

                    def commitId =
                        readFile('commit_id.txt').trim()

                    def message =
                        readFile('commit_message.txt').trim()

                    def commitDate =
                        readFile('commit_date.txt').trim()

                    def buildTime =
                        readFile('build_time.txt').trim()

                    echo '========================================'
                    echo '             COMMIT DETAILS'
                    echo '========================================'
                    echo "Author      : ${author}"
                    echo "Email       : ${email}"
                    echo "Commit ID   : ${commitId}"
                    echo "Message     : ${message}"
                    echo "Commit Date : ${commitDate}"
                    echo "Build Time  : ${buildTime}"
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
        // 4. TEST RESULT + 80% QUALITY GATE
        // =====================================================
        stage('80 Percent Test Gate') {
            steps {

                junit(
                    allowEmptyResults: false,
                    testResults: 'target/surefire-reports/TEST-*.xml'
                )

                powershell '''
                $files = Get-ChildItem "target\\surefire-reports\\TEST-*.xml"

                $total = 0
                $failures = 0
                $errors = 0
                $skipped = 0

                foreach ($file in $files) {

                    [xml]$xml = Get-Content $file.FullName

                    $suite = $xml.testsuite

                    $total += [int]$suite.tests
                    $failures += [int]$suite.failures
                    $errors += [int]$suite.errors
                    $skipped += [int]$suite.skipped
                }

                $failedTotal = $failures + $errors

                "$total,$failedTotal,$skipped" |
                    Set-Content "test-summary.txt"
                '''

                script {

                    def summary =
                        readFile('test-summary.txt').trim()

                    def values =
                        summary.split(',')

                    def total =
                        values[0].trim().toInteger()

                    def failed =
                        values[1].trim().toInteger()

                    def skipped =
                        values[2].trim().toInteger()

                    def passed =
                        total - failed - skipped

                    def percentage =
                        total > 0
                            ? (passed * 100.0 / total)
                            : 0.0

                    def formattedPercentage =
                        String.format('%.2f', percentage)

                    writeFile(
                        file: 'test-result.txt',
                        text:
                            "${total}\n" +
                            "${passed}\n" +
                            "${failed}\n" +
                            "${skipped}\n" +
                            "${formattedPercentage}\n"
                    )

                    echo '========================================'
                    echo '              TEST SUMMARY'
                    echo '========================================'
                    echo "Total Tests  : ${total}"
                    echo "Passed       : ${passed}"
                    echo "Failed       : ${failed}"
                    echo "Skipped      : ${skipped}"
                    echo "Pass Percent : ${formattedPercentage}%"
                    echo '========================================'

                    currentBuild.description =
                        "Tests: ${passed}/${total} | ${formattedPercentage}%"

                    if (total == 0) {

                        echo '========================================'
                        echo '           NO TESTS FOUND'
                        echo '========================================'
                        echo 'DEV BRANCH WILL NOT BE UPDATED'
                        echo 'DEPLOYMENT WILL NOT START'
                        echo '========================================'

                        error(
                            'QUALITY GATE FAILED - No tests found.'
                        )
                    }

                    if (percentage < 80.0) {

                        echo '========================================'
                        echo '        TEST QUALITY GATE FAILED'
                        echo '========================================'
                        echo 'Required : 80%'
                        echo "Actual   : ${formattedPercentage}%"
                        echo 'DEV BRANCH WILL NOT BE UPDATED'
                        echo 'DEPLOYMENT WILL NOT START'
                        echo '========================================'

                        error(
                            "TEST QUALITY GATE FAILED - " +
                            "${formattedPercentage}% passed. " +
                            "Minimum required is 80%."
                        )
                    }

                    echo '========================================'
                    echo '        TEST QUALITY GATE PASSED'
                    echo '========================================'
                    echo "Pass Percentage : ${formattedPercentage}%"
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
                    @echo off

                    echo ========================================
                    echo        SAVE PASSED CODE TO DEV
                    echo ========================================

                    echo Fetching current DEV branch...

                    git fetch ^
                    https://%GIT_USER%:%GIT_TOKEN%@github.com/HealthinRhowina06/user-analysis-service-ci-test.git ^
                    dev

                    if errorlevel 1 exit /b 1

                    echo.
                    echo Pushing tested commit to DEV...

                    git push ^
                    https://%GIT_USER%:%GIT_TOKEN%@github.com/HealthinRhowina06/user-analysis-service-ci-test.git ^
                    HEAD:dev

                    if errorlevel 1 exit /b 1

                    echo SAVED TO DEV> dev-result.txt

                    echo.
                    echo ========================================
                    echo PASSED CODE SAVED TO DEV SUCCESSFULLY
                    echo ========================================
                    '''
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
                echo DOCKER IMAGE SAVED
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
                echo IMAGE LOADED ON SERVER
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

                writeFile(
                    file: 'deployment-result.txt',
                    text: 'DEPLOYED - HEALTH UP'
                )
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
            echo 'Check the failed stage above.'
            echo 'If tests are below 80%, DEV is NOT updated.'
            echo 'If tests fail quality gate, deployment stops.'
            echo 'If SSH fails, check Jenkins SYSTEM SSH key.'
            echo 'If deployment fails, check Docker logs.'
            echo '============================================'
        }

        always {

            script {

                def author =
                    fileExists('commit_author.txt')
                        ? readFile('commit_author.txt').trim()
                        : 'UNKNOWN'

                def email =
                    fileExists('commit_email.txt')
                        ? readFile('commit_email.txt').trim()
                        : 'UNKNOWN'

                def commitId =
                    fileExists('commit_id.txt')
                        ? readFile('commit_id.txt').trim()
                        : 'UNKNOWN'

                def message =
                    fileExists('commit_message.txt')
                        ? readFile('commit_message.txt').trim()
                        : 'UNKNOWN'

                def commitDate =
                    fileExists('commit_date.txt')
                        ? readFile('commit_date.txt').trim()
                        : 'UNKNOWN'

                def buildTime =
                    fileExists('build_time.txt')
                        ? readFile('build_time.txt').trim()
                        : 'UNKNOWN'

                def total = '0'
                def passed = '0'
                def failed = '0'
                def skipped = '0'
                def percentage = '0.00'

                if (fileExists('test-result.txt')) {

                    def resultLines =
                        readFile('test-result.txt')
                            .readLines()

                    if (resultLines.size() >= 5) {
                        total =
                            resultLines[0].trim()

                        passed =
                            resultLines[1].trim()

                        failed =
                            resultLines[2].trim()

                        skipped =
                            resultLines[3].trim()

                        percentage =
                            resultLines[4].trim()
                    }
                }

                def devResult =
                    fileExists('dev-result.txt')
                        ? readFile('dev-result.txt').trim()
                        : 'NOT SAVED'

                def deploymentResult =
                    fileExists('deployment-result.txt')
                        ? readFile('deployment-result.txt').trim()
                        : 'NOT DEPLOYED'

                def finalResult =
                    currentBuild.currentResult ?: 'UNKNOWN'

                def safeAuthor =
                    author.replace('"', '""')

                def safeEmail =
                    email.replace('"', '""')

                def safeMessage =
                    message
                        .replace('"', '""')
                        .replace('\r', ' ')
                        .replace('\n', ' ')

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
                    "\"${buildTime}\"," +
                    "\"${env.SOURCE_BRANCH}\"," +
                    "\"${safeAuthor}\"," +
                    "\"${safeEmail}\"," +
                    "\"${commitId}\"," +
                    "\"${safeMessage}\"," +
                    "\"${commitDate}\"," +
                    "\"${total}\"," +
                    "\"${passed}\"," +
                    "\"${failed}\"," +
                    "\"${skipped}\"," +
                    "\"${percentage}%\"," +
                    "\"${devResult}\"," +
                    "\"${finalResult}\"," +
                    "\"${deploymentResult}\"," +
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
                echo "Date         : ${buildTime}"
                echo "Author       : ${author}"
                echo "Email        : ${email}"
                echo "Commit ID    : ${commitId}"
                echo "Message      : ${message}"
                echo "Branch       : ${env.SOURCE_BRANCH}"
                echo "Total Tests  : ${total}"
                echo "Passed       : ${passed}"
                echo "Failed       : ${failed}"
                echo "Skipped      : ${skipped}"
                echo "Pass Rate    : ${percentage}%"
                echo "Dev Result   : ${devResult}"
                echo "Pipeline     : ${finalResult}"
                echo "Deployment   : ${deploymentResult}"
                echo '============================================'
            }

            archiveArtifacts(
                artifacts: 'target/surefire-reports/**,ci-history-*.csv',
                allowEmptyArchive: true
            )
        }
    }
}