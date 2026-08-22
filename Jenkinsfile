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
                @echo off

                echo ========================================
                echo             COMMIT DETAILS
                echo ========================================

                echo Author:
                git log -1 --format=%%an

                echo Email:
                git log -1 --format=%%ae

                echo Commit ID:
                git rev-parse HEAD

                echo Message:
                git log -1 --format=%%s

                echo Commit Date:
                git log -1 --format=%%ad

                echo ========================================

                git log -1 --format=%%an > .ci_author.txt
                git log -1 --format=%%ae > .ci_email.txt
                git rev-parse HEAD > .ci_commit.txt
                git log -1 --format=%%s > .ci_message.txt
                git log -1 --format=%%ad > .ci_date.txt
                '''
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

                writeFile(
                    file: 'ci-test-summary.ps1',
                    text: '''
$ErrorActionPreference = "Stop"

$reportPath = "target\\surefire-reports"

if (-not (Test-Path $reportPath)) {
    Write-Host "ERROR: Surefire report folder not found."
    exit 1
}

$files = Get-ChildItem `
    -Path $reportPath `
    -Filter "TEST-*.xml" `
    -File

if ($files.Count -eq 0) {
    Write-Host "ERROR: No TEST-*.xml files found."
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
    $percentage = [math]::Round(
        ($passed * 100.0) / $total,
        2
    )
}
else {
    $percentage = 0
}

if ($percentage -ge 80) {
    $gate = "PASS"
}
else {
    $gate = "FAIL"
}

Set-Content ".ci_test_total.txt" $total
Set-Content ".ci_test_passed.txt" $passed
Set-Content ".ci_test_failed.txt" $failed
Set-Content ".ci_test_skipped.txt" $skipped
Set-Content ".ci_test_percent.txt" $percentage
Set-Content ".ci_test_gate.txt" $gate

Write-Host ""
Write-Host "========================================"
Write-Host "              TEST SUMMARY"
Write-Host "========================================"
Write-Host "Total Tests  : $total"
Write-Host "Passed       : $passed"
Write-Host "Failed       : $failed"
Write-Host "Skipped      : $skipped"
Write-Host "Pass Percent : $percentage%"
Write-Host "Test Gate    : $gate"
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
                    def total =
                        readFile('.ci_test_total.txt').trim()

                    def passed =
                        readFile('.ci_test_passed.txt').trim()

                    def percentage =
                        readFile('.ci_test_percent.txt').trim()

                    currentBuild.description =
                        "Tests: ${passed}/${total} | ${percentage}%"

                    echo "Jenkins Card: Tests ${passed}/${total} | ${percentage}%"
                }
            }
        }

        stage('Publish Test Report') {
            steps {
                junit(
                    allowEmptyResults: false,
                    testResults: 'target/surefire-reports/TEST-*.xml'
                )
            }
        }

        stage('80 Percent Test Gate') {
            steps {

                bat '''
                @echo off

                echo ========================================
                echo           80 PERCENT TEST GATE
                echo ========================================

                echo Total Tests:
                type .ci_test_total.txt

                echo Passed:
                type .ci_test_passed.txt

                echo Failed:
                type .ci_test_failed.txt

                echo Skipped:
                type .ci_test_skipped.txt

                echo Pass Percentage:
                type .ci_test_percent.txt

                echo Gate:
                type .ci_test_gate.txt

                echo ========================================

                set /p TEST_GATE=<.ci_test_gate.txt

                if /I "%TEST_GATE%"=="PASS" (
                    echo TEST QUALITY GATE PASSED
                    echo TEST RESULT IS 80 PERCENT OR ABOVE
                    exit /b 0
                )

                echo ========================================
                echo TEST QUALITY GATE FAILED
                echo TEST RESULT IS BELOW 80 PERCENT
                echo DEPLOYMENT WILL NOT RUN
                echo ========================================

                exit /b 1
                '''
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
                echo DOCKER IMAGE SAVED
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

                echo IMAGE TRANSFER COMPLETED
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

                echo DOCKER IMAGE LOADED ON SERVER
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

                echo DOCKER NETWORK READY
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

                echo DOCKER COMPOSE DEPLOY COMPLETED
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

        always {

            script {
                writeFile(
                    file: '.ci_pipeline_result.txt',
                    text: "${currentBuild.currentResult}"
                )
            }

            writeFile(
                file: 'generate-ci-history.ps1',
                text: '''
$ErrorActionPreference = "Continue"

function ReadValue($path, $default) {

    if (Test-Path $path) {
        return (Get-Content $path -Raw).Trim()
    }

    return $default
}

$author = ReadValue ".ci_author.txt" "UNKNOWN"
$email = ReadValue ".ci_email.txt" "UNKNOWN"
$commitId = ReadValue ".ci_commit.txt" "UNKNOWN"
$message = ReadValue ".ci_message.txt" "UNKNOWN"
$commitDate = ReadValue ".ci_date.txt" "UNKNOWN"

$total = ReadValue ".ci_test_total.txt" "0"
$passed = ReadValue ".ci_test_passed.txt" "0"
$failed = ReadValue ".ci_test_failed.txt" "0"
$skipped = ReadValue ".ci_test_skipped.txt" "0"
$percentage = ReadValue ".ci_test_percent.txt" "0"
$gate = ReadValue ".ci_test_gate.txt" "UNKNOWN"

$result = ReadValue ".ci_pipeline_result.txt" "UNKNOWN"

if ($result -eq "SUCCESS") {
    $deployment = "DEPLOYED"
}
else {
    $deployment = "NOT DEPLOYED / FAILED"
}

function CsvSafe($value) {
    return '"' + ($value -replace '"','""') + '"'
}

$header = @(
    "Build Number",
    "Branch",
    "Author",
    "Email",
    "Commit ID",
    "Commit Message",
    "Commit Date",
    "Total Tests",
    "Passed",
    "Failed",
    "Skipped",
    "Pass Percentage",
    "Test Gate",
    "Pipeline Result",
    "Deployment Result",
    "Build URL"
) -join ","

$row = @(
    (CsvSafe $env:BUILD_NUMBER),
    (CsvSafe "feature/ci-test"),
    (CsvSafe $author),
    (CsvSafe $email),
    (CsvSafe $commitId),
    (CsvSafe $message),
    (CsvSafe $commitDate),
    (CsvSafe $total),
    (CsvSafe $passed),
    (CsvSafe $failed),
    (CsvSafe $skipped),
    (CsvSafe "$percentage%"),
    (CsvSafe $gate),
    (CsvSafe $result),
    (CsvSafe $deployment),
    (CsvSafe $env:BUILD_URL)
) -join ","

$file = "ci-history-$($env:BUILD_NUMBER).csv"

Set-Content $file $header
Add-Content $file $row

Write-Host ""
Write-Host "============================================"
Write-Host "              CI HISTORY RECORD"
Write-Host "============================================"

Write-Host "Build No    : $($env:BUILD_NUMBER)"
Write-Host "Author      : $author"
Write-Host "Email       : $email"
Write-Host "Commit ID   : $commitId"
Write-Host "Message     : $message"
Write-Host "Commit Date : $commitDate"

Write-Host "Total Tests : $total"
Write-Host "Passed      : $passed"
Write-Host "Failed      : $failed"
Write-Host "Skipped     : $skipped"
Write-Host "Pass Rate   : $percentage%"
Write-Host "Test Gate   : $gate"

Write-Host "Result      : $result"
Write-Host "Deployment  : $deployment"

Write-Host "============================================"
'''
            )

            bat '''
            powershell.exe ^
            -NoProfile ^
            -ExecutionPolicy Bypass ^
            -File generate-ci-history.ps1
            '''

            archiveArtifacts(
                artifacts: 'target/surefire-reports/**,ci-history-*.csv',
                allowEmptyArchive: true
            )
        }

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
            echo 'If test pass percentage is below 80%,'
            echo 'Docker build and deployment are stopped.'
            echo '============================================'
        }
    }
}