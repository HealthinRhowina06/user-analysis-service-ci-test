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

        COMMIT_AUTHOR  = ''
        COMMIT_EMAIL   = ''
        COMMIT_ID      = ''
        COMMIT_MESSAGE = ''
        COMMIT_DATE    = ''

        TEST_TOTAL   = '0'
        TEST_PASSED  = '0'
        TEST_FAILED  = '0'
        TEST_SKIPPED = '0'
        TEST_PERCENT = '0.00'

        TEST_GATE = 'UNKNOWN'
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

                /*
                 * Write Git details into files first.
                 * This avoids the previous bat(returnStdout) -> null issue.
                 */
                bat '''
                @echo off

                git log -1 --format=%%an > .ci_author.txt
                git log -1 --format=%%ae > .ci_email.txt
                git rev-parse HEAD > .ci_commit.txt
                git log -1 --format=%%s > .ci_message.txt
                git log -1 --format=%%ad > .ci_date.txt
                '''

                script {

                    env.COMMIT_AUTHOR =
                        readFile('.ci_author.txt').trim()

                    env.COMMIT_EMAIL =
                        readFile('.ci_email.txt').trim()

                    env.COMMIT_ID =
                        readFile('.ci_commit.txt').trim()

                    env.COMMIT_MESSAGE =
                        readFile('.ci_message.txt').trim()

                    env.COMMIT_DATE =
                        readFile('.ci_date.txt').trim()

                    echo '========================================'
                    echo '             COMMIT DETAILS'
                    echo '========================================'
                    echo "Author      : ${env.COMMIT_AUTHOR}"
                    echo "Email       : ${env.COMMIT_EMAIL}"
                    echo "Commit ID   : ${env.COMMIT_ID}"
                    echo "Message     : ${env.COMMIT_MESSAGE}"
                    echo "Commit Date : ${env.COMMIT_DATE}"
                    echo '========================================'
                }
            }
        }

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

        stage('Calculate Test Results') {
            steps {

                /*
                 * Parse Maven Surefire XML directly.
                 *
                 * This fixes:
                 * Tests: 0/0 | 0%
                 *
                 * We don't depend on junit() return counters anymore.
                 */

                writeFile(
                    file: 'ci-test-summary.ps1',
                    text: '''
$ErrorActionPreference = "Stop"

$reportPath = "target\\surefire-reports"

if (-not (Test-Path $reportPath)) {
    Write-Host "Surefire report directory not found."
    exit 1
}

$files = Get-ChildItem `
    -Path $reportPath `
    -Filter "TEST-*.xml" `
    -File

if ($files.Count -eq 0) {
    Write-Host "No Surefire XML test reports found."
    exit 1
}

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

$failed = $failures + $errors
$passed = $total - $failed - $skipped

if ($total -gt 0) {

    $percentage =
        [math]::Round(
            ($passed * 100.0) / $total,
            2
        )

} else {

    $percentage = 0
}

Set-Content `
    -Path ".ci_test_total.txt" `
    -Value $total

Set-Content `
    -Path ".ci_test_passed.txt" `
    -Value $passed

Set-Content `
    -Path ".ci_test_failed.txt" `
    -Value $failed

Set-Content `
    -Path ".ci_test_skipped.txt" `
    -Value $skipped

Set-Content `
    -Path ".ci_test_percent.txt" `
    -Value $percentage

if ($percentage -ge 80) {

    Set-Content `
        -Path ".ci_test_gate.txt" `
        -Value "PASS"

} else {

    Set-Content `
        -Path ".ci_test_gate.txt" `
        -Value "FAIL"
}

Write-Host "========================================"
Write-Host "       POWERSHELL TEST CALCULATION"
Write-Host "========================================"
Write-Host "Total   : $total"
Write-Host "Passed  : $passed"
Write-Host "Failed  : $failed"
Write-Host "Skipped : $skipped"
Write-Host "Percent : $percentage%"
Write-Host "========================================"
'''
                )

                bat '''
                powershell.exe ^
                -NoProfile ^
                -ExecutionPolicy Bypass ^
                -File ci-test-summary.ps1
                '''

                script {

                    env.TEST_TOTAL =
                        readFile('.ci_test_total.txt').trim()

                    env.TEST_PASSED =
                        readFile('.ci_test_passed.txt').trim()

                    env.TEST_FAILED =
                        readFile('.ci_test_failed.txt').trim()

                    env.TEST_SKIPPED =
                        readFile('.ci_test_skipped.txt').trim()

                    env.TEST_PERCENT =
                        readFile('.ci_test_percent.txt').trim()

                    env.TEST_GATE =
                        readFile('.ci_test_gate.txt').trim()

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
                        "Tests: ${env.TEST_PASSED}/${env.TEST_TOTAL} | ${env.TEST_PERCENT}%"
                }
            }
        }

        stage('Publish Test Report') {
            steps {

                /*
                 * junit is still used to show the test report
                 * inside Jenkins.
                 *
                 * But we do NOT use its returned counts.
                 */

                junit(
                    allowEmptyResults: false,
                    testResults: 'target/surefire-reports/TEST-*.xml'
                )
            }
        }

        stage('80 Percent Test Gate') {
            steps {

                script {

                    echo '========================================'
                    echo '           80 PERCENT TEST GATE'
                    echo '========================================'

                    echo "Total Tests : ${env.TEST_TOTAL}"
                    echo "Passed      : ${env.TEST_PASSED}"
                    echo "Failed      : ${env.TEST_FAILED}"
                    echo "Skipped     : ${env.TEST_SKIPPED}"
                    echo "Pass Rate   : ${env.TEST_PERCENT}%"

                    echo '========================================'

                    if (env.TEST_GATE == 'FAIL') {

                        error(
                            "TEST QUALITY GATE FAILED - " +
                            "${env.TEST_PERCENT}% tests passed. " +
                            "Minimum required is 80%. " +
                            "DEPLOYMENT STOPPED."
                        )
                    }

                    if (env.TEST_GATE != 'PASS') {

                        error(
                            "TEST RESULT COULD NOT BE DETERMINED. " +
                            "DEPLOYMENT STOPPED."
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
            }
        }
    }

    post {

        success {

            echo '============================================'
            echo '            CI/CD PIPELINE SUCCESS'
            echo '============================================'

            echo "Author              : ${env.COMMIT_AUTHOR}"
            echo "Commit ID           : ${env.COMMIT_ID}"
            echo "Tests               : ${env.TEST_PASSED}/${env.TEST_TOTAL}"
            echo "Test Percentage     : ${env.TEST_PERCENT}%"

            echo '80 Percent Gate     : PASSED'
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

            echo "Author          : ${env.COMMIT_AUTHOR}"
            echo "Commit ID       : ${env.COMMIT_ID}"
            echo "Tests           : ${env.TEST_PASSED}/${env.TEST_TOTAL}"
            echo "Test Percentage : ${env.TEST_PERCENT}%"

            echo ''
            echo 'If tests are below 80%, deployment is stopped.'
            echo 'Check the failed Jenkins stage.'

            echo '============================================'
        }

        always {

            script {

                def finalResult =
                    currentBuild.currentResult ?: 'UNKNOWN'

                def deploymentResult =
                    finalResult == 'SUCCESS'
                        ? 'DEPLOYED'
                        : 'NOT DEPLOYED / FAILED'

                def branchName =
                    env.BRANCH_NAME ?: 'feature/ci-test'

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

                def safeCommitDate =
                    (env.COMMIT_DATE ?: '')
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
                    '"Test Gate",' +
                    '"Pipeline Result",' +
                    '"Deployment Result",' +
                    '"Build URL"'

                def row =
                    "\"${env.BUILD_NUMBER}\"," +
                    "\"${env.BUILD_ID ?: ''}\"," +
                    "\"${branchName}\"," +
                    "\"${safeAuthor}\"," +
                    "\"${safeEmail}\"," +
                    "\"${env.COMMIT_ID ?: ''}\"," +
                    "\"${safeMessage}\"," +
                    "\"${safeCommitDate}\"," +
                    "\"${env.TEST_TOTAL ?: '0'}\"," +
                    "\"${env.TEST_PASSED ?: '0'}\"," +
                    "\"${env.TEST_FAILED ?: '0'}\"," +
                    "\"${env.TEST_SKIPPED ?: '0'}\"," +
                    "\"${env.TEST_PERCENT ?: '0'}%\"," +
                    "\"${env.TEST_GATE ?: 'UNKNOWN'}\"," +
                    "\"${finalResult}\"," +
                    "\"${deploymentResult}\"," +
                    "\"${env.BUILD_URL ?: ''}\""

                def historyFile =
                    "ci-history-${env.BUILD_NUMBER}.csv"

                writeFile(
                    file: historyFile,
                    text: "${header}\r\n${row}\r\n"
                )

                echo '============================================'
                echo '              CI HISTORY RECORD'
                echo '============================================'

                echo "Build No     : ${env.BUILD_NUMBER}"
                echo "Author       : ${env.COMMIT_AUTHOR}"
                echo "Email        : ${env.COMMIT_EMAIL}"
                echo "Commit ID    : ${env.COMMIT_ID}"
                echo "Message      : ${env.COMMIT_MESSAGE}"
                echo "Branch       : ${branchName}"

                echo "Total Tests  : ${env.TEST_TOTAL}"
                echo "Passed       : ${env.TEST_PASSED}"
                echo "Failed       : ${env.TEST_FAILED}"
                echo "Skipped      : ${env.TEST_SKIPPED}"
                echo "Pass Rate    : ${env.TEST_PERCENT}%"
                echo "Test Gate    : ${env.TEST_GATE}"

                echo "Result       : ${finalResult}"
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