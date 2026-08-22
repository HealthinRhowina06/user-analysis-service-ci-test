// pipeline {
//     agent any
//
//     tools {
//         jdk 'JDK-25'
//         maven 'Maven-3.9.16'
//     }
//
//     environment {
//         DOCKER_EXE = 'C:\\Users\\hrhow\\AppData\\Local\\Programs\\DockerDesktop\\resources\\bin\\docker.exe'
//
//         IMAGE_NAME = 'vijayjeyam/prodmexaanalysis'
//         IMAGE_TAG  = 'latest'
//
//         SERVER_USER = 'mani'
//         SERVER_IP   = '122.165.70.116'
//         SERVER_PATH = '/home/mani/user-analysis-service'
//
//         SSH_KEY = 'C:\\Windows\\System32\\config\\systemprofile\\.ssh\\id_ed25519'
//     }
//
//     triggers {
//         githubPush()
//     }
//
//     stages {
//
//         stage('Checkout') {
//             steps {
//                 checkout scm
//             }
//         }
//
//         stage('Commit Details') {
//             steps {
//                 bat '''
//                 @echo off
//
//                 echo ========================================
//                 echo             COMMIT DETAILS
//                 echo ========================================
//
//                 echo Author:
//                 git log -1 --format=%%an
//
//                 echo Email:
//                 git log -1 --format=%%ae
//
//                 echo Commit ID:
//                 git rev-parse HEAD
//
//                 echo Message:
//                 git log -1 --format=%%s
//
//                 echo Commit Date:
//                 git log -1 --format=%%ad
//
//                 echo ========================================
//
//                 git log -1 --format=%%an > .ci_author.txt
//                 git log -1 --format=%%ae > .ci_email.txt
//                 git rev-parse HEAD > .ci_commit.txt
//                 git log -1 --format=%%s > .ci_message.txt
//                 git log -1 --format=%%ad > .ci_date.txt
//                 '''
//             }
//         }
//
//         stage('Run Unit Tests') {
//             steps {
//                 bat '''
//                 echo ========================================
//                 echo             RUN UNIT TESTS
//                 echo ========================================
//
//                 mvn test -Dmaven.test.failure.ignore=true
//                 '''
//             }
//         }
//
//         stage('Calculate Test Results') {
//             steps {
//
//                 writeFile(
//                     file: 'ci-test-summary.ps1',
//                     text: '''
// $ErrorActionPreference = "Stop"
//
// $reportPath = "target\\surefire-reports"
//
// if (-not (Test-Path $reportPath)) {
//     Write-Host "ERROR: Surefire report folder not found."
//     exit 1
// }
//
// $files = Get-ChildItem `
//     -Path $reportPath `
//     -Filter "TEST-*.xml" `
//     -File
//
// if ($files.Count -eq 0) {
//     Write-Host "ERROR: No TEST-*.xml files found."
//     exit 1
// }
//
// $total = 0
// $failures = 0
// $errors = 0
// $skipped = 0
//
// foreach ($file in $files) {
//
//     [xml]$xml = Get-Content $file.FullName
//
//     $suite = $xml.testsuite
//
//     $total += [int]$suite.tests
//     $failures += [int]$suite.failures
//     $errors += [int]$suite.errors
//     $skipped += [int]$suite.skipped
// }
//
// $failed = $failures + $errors
// $passed = $total - $failed - $skipped
//
// if ($total -gt 0) {
//     $percentage = [math]::Round(
//         ($passed * 100.0) / $total,
//         2
//     )
// }
// else {
//     $percentage = 0
// }
//
// if ($percentage -ge 80) {
//     $gate = "PASS"
// }
// else {
//     $gate = "FAIL"
// }
//
// Set-Content ".ci_test_total.txt" $total
// Set-Content ".ci_test_passed.txt" $passed
// Set-Content ".ci_test_failed.txt" $failed
// Set-Content ".ci_test_skipped.txt" $skipped
// Set-Content ".ci_test_percent.txt" $percentage
// Set-Content ".ci_test_gate.txt" $gate
//
// Write-Host ""
// Write-Host "========================================"
// Write-Host "              TEST SUMMARY"
// Write-Host "========================================"
// Write-Host "Total Tests  : $total"
// Write-Host "Passed       : $passed"
// Write-Host "Failed       : $failed"
// Write-Host "Skipped      : $skipped"
// Write-Host "Pass Percent : $percentage%"
// Write-Host "Test Gate    : $gate"
// Write-Host "========================================"
// '''
//                 )
//
//                 bat '''
//                 powershell.exe ^
//                 -NoProfile ^
//                 -ExecutionPolicy Bypass ^
//                 -File ci-test-summary.ps1
//                 '''
//
//                 script {
//                     def total =
//                         readFile('.ci_test_total.txt').trim()
//
//                     def passed =
//                         readFile('.ci_test_passed.txt').trim()
//
//                     def percentage =
//                         readFile('.ci_test_percent.txt').trim()
//
//                     currentBuild.description =
//                         "Tests: ${passed}/${total} | ${percentage}%"
//                 }
//             }
//         }
//
//         stage('Publish Test Report') {
//             steps {
//                 junit(
//                     allowEmptyResults: false,
//                     testResults: 'target/surefire-reports/TEST-*.xml'
//                 )
//             }
//         }
//
//         stage('80 Percent Test Gate') {
//             steps {
//                 bat '''
//                 @echo off
//
//                 echo ========================================
//                 echo           80 PERCENT TEST GATE
//                 echo ========================================
//
//                 echo Total Tests:
//                 type .ci_test_total.txt
//
//                 echo Passed:
//                 type .ci_test_passed.txt
//
//                 echo Failed:
//                 type .ci_test_failed.txt
//
//                 echo Skipped:
//                 type .ci_test_skipped.txt
//
//                 echo Percentage:
//                 type .ci_test_percent.txt
//
//                 echo Gate:
//                 type .ci_test_gate.txt
//
//                 echo ========================================
//
//                 set /p TEST_GATE=<.ci_test_gate.txt
//
//                 if /I "%TEST_GATE%"=="PASS" (
//                     echo TEST QUALITY GATE PASSED
//                     echo TEST RESULT IS 80 PERCENT OR ABOVE
//                     exit /b 0
//                 )
//
//                 echo ========================================
//                 echo TEST QUALITY GATE FAILED
//                 echo TEST RESULT IS BELOW 80 PERCENT
//                 echo DEPLOYMENT STOPPED
//                 echo ========================================
//
//                 exit /b 1
//                 '''
//             }
//         }
//
//         stage('Maven Build') {
//             steps {
//                 bat '''
//                 echo ========================================
//                 echo              MAVEN BUILD
//                 echo ========================================
//
//                 mvn package -DskipTests
//                 '''
//             }
//         }
//
//         stage('Docker Build') {
//             steps {
//                 bat '''
//                 echo ========================================
//                 echo              DOCKER BUILD
//                 echo ========================================
//
//                 "%DOCKER_EXE%" build ^
//                 -t %IMAGE_NAME%:%BUILD_NUMBER% ^
//                 -t %IMAGE_NAME%:%IMAGE_TAG% ^
//                 .
//
//                 echo ========================================
//                 echo DOCKER IMAGE CREATED
//                 echo %IMAGE_NAME%:%BUILD_NUMBER%
//                 echo %IMAGE_NAME%:%IMAGE_TAG%
//                 echo ========================================
//                 '''
//             }
//         }
//
//         stage('Docker Save') {
//             steps {
//                 bat '''
//                 echo ========================================
//                 echo              DOCKER SAVE
//                 echo ========================================
//
//                 if exist app-image.tar (
//                     del /F /Q app-image.tar
//                 )
//
//                 "%DOCKER_EXE%" save ^
//                 -o app-image.tar ^
//                 %IMAGE_NAME%:%IMAGE_TAG%
//
//                 dir app-image.tar
//
//                 echo ========================================
//                 echo DOCKER IMAGE SAVED
//                 echo ========================================
//                 '''
//             }
//         }
//
//         stage('Check SSH Connection') {
//             steps {
//                 bat '''
//                 echo ========================================
//                 echo          CHECK SSH CONNECTION
//                 echo ========================================
//
//                 ssh ^
//                 -o BatchMode=yes ^
//                 -o StrictHostKeyChecking=no ^
//                 -i "%SSH_KEY%" ^
//                 %SERVER_USER%@%SERVER_IP% ^
//                 "echo SSH CONNECTION SUCCESS"
//
//                 echo ========================================
//                 '''
//             }
//         }
//
//         stage('Transfer Docker Image') {
//             steps {
//                 bat '''
//                 echo ========================================
//                 echo         TRANSFER DOCKER IMAGE
//                 echo ========================================
//
//                 scp ^
//                 -o BatchMode=yes ^
//                 -o StrictHostKeyChecking=no ^
//                 -i "%SSH_KEY%" ^
//                 app-image.tar ^
//                 %SERVER_USER%@%SERVER_IP%:%SERVER_PATH%/app-image.tar
//
//                 echo ========================================
//                 echo IMAGE TRANSFER COMPLETED
//                 echo ========================================
//                 '''
//             }
//         }
//
//         stage('Docker Load On Server') {
//             steps {
//                 bat '''
//                 echo ========================================
//                 echo         DOCKER LOAD ON SERVER
//                 echo ========================================
//
//                 ssh ^
//                 -o BatchMode=yes ^
//                 -o StrictHostKeyChecking=no ^
//                 -i "%SSH_KEY%" ^
//                 %SERVER_USER%@%SERVER_IP% ^
//                 "cd %SERVER_PATH% && docker load -i app-image.tar"
//
//                 echo ========================================
//                 echo DOCKER IMAGE LOADED
//                 echo ========================================
//                 '''
//             }
//         }
//
//         stage('Check Docker Network') {
//             steps {
//                 bat '''
//                 echo ========================================
//                 echo          CHECK DOCKER NETWORK
//                 echo ========================================
//
//                 ssh ^
//                 -o BatchMode=yes ^
//                 -o StrictHostKeyChecking=no ^
//                 -i "%SSH_KEY%" ^
//                 %SERVER_USER%@%SERVER_IP% ^
//                 "docker network inspect prodmexa >/dev/null 2>&1 || docker network create prodmexa"
//
//                 echo ========================================
//                 echo DOCKER NETWORK READY
//                 echo ========================================
//                 '''
//             }
//         }
//
//         stage('Docker Compose Deploy') {
//             steps {
//                 bat '''
//                 echo ========================================
//                 echo          DOCKER COMPOSE DEPLOY
//                 echo ========================================
//
//                 ssh ^
//                 -o BatchMode=yes ^
//                 -o StrictHostKeyChecking=no ^
//                 -i "%SSH_KEY%" ^
//                 %SERVER_USER%@%SERVER_IP% ^
//                 "cd %SERVER_PATH% && docker compose -f docker_env/prod.yml up -d --force-recreate"
//
//                 echo DEPLOYED > .ci_deploy.txt
//
//                 echo ========================================
//                 echo DOCKER COMPOSE DEPLOY COMPLETED
//                 echo ========================================
//                 '''
//             }
//         }
//
//         stage('Verify Container') {
//             steps {
//                 bat '''
//                 echo ========================================
//                 echo            VERIFY CONTAINER
//                 echo ========================================
//
//                 ssh ^
//                 -o BatchMode=yes ^
//                 -o StrictHostKeyChecking=no ^
//                 -i "%SSH_KEY%" ^
//                 %SERVER_USER%@%SERVER_IP% ^
//                 "docker ps --filter name=prodmexaanalysis"
//
//                 echo ========================================
//                 '''
//             }
//         }
//
//         stage('Health Check') {
//             steps {
//                 bat '''
//                 echo ========================================
//                 echo              HEALTH CHECK
//                 echo ========================================
//
//                 ssh ^
//                 -o BatchMode=yes ^
//                 -o StrictHostKeyChecking=no ^
//                 -i "%SSH_KEY%" ^
//                 %SERVER_USER%@%SERVER_IP% ^
//                 "for i in 1 2 3 4 5 6; do RESPONSE=$(curl -s http://localhost:9035/actuator/health); echo $RESPONSE; echo $RESPONSE | grep -q \\"UP\\" && exit 0; sleep 5; done; echo APPLICATION HEALTH CHECK FAILED; docker logs --tail 50 prodmexaanalysis; exit 1"
//
//                 echo UP > .ci_health.txt
//
//                 echo ========================================
//                 echo APPLICATION HEALTH CHECK PASSED
//                 echo ========================================
//                 '''
//             }
//         }
//     }
//
//     post {
//
//         always {
//
//             script {
//                 writeFile(
//                     file: '.ci_pipeline_result.txt',
//                     text: "${currentBuild.currentResult}"
//                 )
//             }
//
//             writeFile(
//                 file: 'generate-ci-report.ps1',
//                 text: '''
// $ErrorActionPreference = "Continue"
//
// function ReadValue($path, $default) {
//
//     if (Test-Path $path) {
//         return (Get-Content $path -Raw).Trim()
//     }
//
//     return $default
// }
//
// function HtmlEncode($value) {
//
//     if ($null -eq $value) {
//         return ""
//     }
//
//     return [System.Net.WebUtility]::HtmlEncode(
//         [string]$value
//     )
// }
//
// $author = ReadValue ".ci_author.txt" "UNKNOWN"
// $email = ReadValue ".ci_email.txt" "UNKNOWN"
// $commitId = ReadValue ".ci_commit.txt" "UNKNOWN"
// $message = ReadValue ".ci_message.txt" "UNKNOWN"
// $commitDate = ReadValue ".ci_date.txt" "UNKNOWN"
//
// $total = ReadValue ".ci_test_total.txt" "0"
// $passed = ReadValue ".ci_test_passed.txt" "0"
// $failed = ReadValue ".ci_test_failed.txt" "0"
// $skipped = ReadValue ".ci_test_skipped.txt" "0"
// $percentage = ReadValue ".ci_test_percent.txt" "0"
// $gate = ReadValue ".ci_test_gate.txt" "UNKNOWN"
//
// $result = ReadValue ".ci_pipeline_result.txt" "UNKNOWN"
// $deployment = ReadValue ".ci_deploy.txt" "NOT DEPLOYED"
// $health = ReadValue ".ci_health.txt" "NOT CHECKED"
//
// $buildNumber = $env:BUILD_NUMBER
// $buildUrl = $env:BUILD_URL
//
// $branch = "feature/ci-test"
//
// # =========================================================
// # TXT REPORT
// # =========================================================
//
// $txtFile = "ci-report-$buildNumber.txt"
//
// $txt = @"
// ============================================================
//                      CI/CD BUILD REPORT
// ============================================================
//
// BUILD DETAILS
//
// Build Number    : $buildNumber
// Branch          : $branch
// Build URL       : $buildUrl
//
//
// COMMIT DETAILS
//
// Commit Author   : $author
// Email           : $email
// Commit ID       : $commitId
// Commit Message  : $message
// Commit Date     : $commitDate
//
//
// TEST RESULTS
//
// Total Tests     : $total
// Passed          : $passed
// Failed          : $failed
// Skipped         : $skipped
// Pass Percentage : $percentage%
// Test Gate       : $gate
//
//
// DEPLOYMENT DETAILS
//
// Pipeline Result : $result
// Deployment      : $deployment
// Application     : $health
//
//
// ============================================================
// "@
//
// Set-Content `
//     -Path $txtFile `
//     -Value $txt `
//     -Encoding UTF8
//
//
// # =========================================================
// # HTML REPORT
// # =========================================================
//
// $htmlFile = "ci-report-$buildNumber.html"
//
// $authorHtml = HtmlEncode $author
// $emailHtml = HtmlEncode $email
// $commitHtml = HtmlEncode $commitId
// $messageHtml = HtmlEncode $message
// $dateHtml = HtmlEncode $commitDate
// $resultHtml = HtmlEncode $result
// $deploymentHtml = HtmlEncode $deployment
// $healthHtml = HtmlEncode $health
// $gateHtml = HtmlEncode $gate
// $buildUrlHtml = HtmlEncode $buildUrl
//
// if ($result -eq "SUCCESS") {
//     $resultClass = "success"
// }
// else {
//     $resultClass = "failure"
// }
//
// if ($gate -eq "PASS") {
//     $gateClass = "success"
// }
// else {
//     $gateClass = "failure"
// }
//
// if ($health -eq "UP") {
//     $healthClass = "success"
// }
// else {
//     $healthClass = "failure"
// }
//
// $html = @"
// <!DOCTYPE html>
// <html>
// <head>
//
// <meta charset="UTF-8">
//
// <title>CI/CD Build Report #$buildNumber</title>
//
// <style>
//
// body {
//     font-family: Arial, Helvetica, sans-serif;
//     background: #f4f6f8;
//     margin: 0;
//     padding: 30px;
// }
//
// .container {
//     max-width: 900px;
//     margin: auto;
//     background: white;
//     padding: 30px;
//     border-radius: 10px;
//     box-shadow: 0 2px 10px rgba(0,0,0,0.08);
// }
//
// h1 {
//     text-align: center;
//     margin-bottom: 5px;
// }
//
// .subtitle {
//     text-align: center;
//     color: #666;
//     margin-bottom: 30px;
// }
//
// .section {
//     margin-top: 25px;
// }
//
// .section h2 {
//     border-bottom: 1px solid #ddd;
//     padding-bottom: 8px;
// }
//
// table {
//     width: 100%;
//     border-collapse: collapse;
// }
//
// td {
//     padding: 10px;
//     border-bottom: 1px solid #eee;
// }
//
// td:first-child {
//     font-weight: bold;
//     width: 230px;
// }
//
// .success {
//     color: green;
//     font-weight: bold;
// }
//
// .failure {
//     color: red;
//     font-weight: bold;
// }
//
// .code {
//     font-family: Consolas, monospace;
//     word-break: break-all;
// }
//
// .footer {
//     margin-top: 30px;
//     text-align: center;
//     color: #777;
//     font-size: 13px;
// }
//
// </style>
//
// </head>
//
// <body>
//
// <div class="container">
//
// <h1>CI/CD Build Report</h1>
//
// <div class="subtitle">
// Build #$buildNumber
// </div>
//
//
// <div class="section">
//
// <h2>Build Details</h2>
//
// <table>
//
// <tr>
// <td>Build Number</td>
// <td>$buildNumber</td>
// </tr>
//
// <tr>
// <td>Branch</td>
// <td>$branch</td>
// </tr>
//
// <tr>
// <td>Pipeline Result</td>
// <td class="$resultClass">$resultHtml</td>
// </tr>
//
// <tr>
// <td>Build URL</td>
// <td>
// <a href="$buildUrlHtml">$buildUrlHtml</a>
// </td>
// </tr>
//
// </table>
//
// </div>
//
//
// <div class="section">
//
// <h2>Commit Details</h2>
//
// <table>
//
// <tr>
// <td>Commit Author</td>
// <td>$authorHtml</td>
// </tr>
//
// <tr>
// <td>Email</td>
// <td>$emailHtml</td>
// </tr>
//
// <tr>
// <td>Commit ID</td>
// <td class="code">$commitHtml</td>
// </tr>
//
// <tr>
// <td>Commit Message</td>
// <td>$messageHtml</td>
// </tr>
//
// <tr>
// <td>Commit Date</td>
// <td>$dateHtml</td>
// </tr>
//
// </table>
//
// </div>
//
//
// <div class="section">
//
// <h2>Test Results</h2>
//
// <table>
//
// <tr>
// <td>Total Tests</td>
// <td>$total</td>
// </tr>
//
// <tr>
// <td>Passed</td>
// <td>$passed</td>
// </tr>
//
// <tr>
// <td>Failed</td>
// <td>$failed</td>
// </tr>
//
// <tr>
// <td>Skipped</td>
// <td>$skipped</td>
// </tr>
//
// <tr>
// <td>Pass Percentage</td>
// <td>$percentage%</td>
// </tr>
//
// <tr>
// <td>80% Quality Gate</td>
// <td class="$gateClass">$gateHtml</td>
// </tr>
//
// </table>
//
// </div>
//
//
// <div class="section">
//
// <h2>Deployment</h2>
//
// <table>
//
// <tr>
// <td>Deployment Status</td>
// <td>$deploymentHtml</td>
// </tr>
//
// <tr>
// <td>Application Health</td>
// <td class="$healthClass">$healthHtml</td>
// </tr>
//
// </table>
//
// </div>
//
//
// <div class="footer">
// Generated automatically by Jenkins
// </div>
//
// </div>
//
// </body>
// </html>
// "@
//
// Set-Content `
//     -Path $htmlFile `
//     -Value $html `
//     -Encoding UTF8
//
//
// # =========================================================
// # CONSOLE SUMMARY
// # =========================================================
//
// Write-Host ""
// Write-Host "============================================================"
// Write-Host "                     CI/CD BUILD REPORT"
// Write-Host "============================================================"
//
// Write-Host ""
// Write-Host "Build Number   : $buildNumber"
// Write-Host "Branch         : $branch"
//
// Write-Host ""
// Write-Host "Commit Author  : $author"
// Write-Host "Email          : $email"
// Write-Host "Commit ID      : $commitId"
// Write-Host "Message        : $message"
//
// Write-Host ""
// Write-Host "Total Tests    : $total"
// Write-Host "Passed         : $passed"
// Write-Host "Failed         : $failed"
// Write-Host "Skipped        : $skipped"
// Write-Host "Pass Rate      : $percentage%"
// Write-Host "Test Gate      : $gate"
//
// Write-Host ""
// Write-Host "Pipeline       : $result"
// Write-Host "Deployment     : $deployment"
// Write-Host "Health         : $health"
//
// Write-Host ""
// Write-Host "HTML Report    : $htmlFile"
// Write-Host "TXT Report     : $txtFile"
//
// Write-Host "============================================================"
// '''
//             )
//
//             bat '''
//             powershell.exe ^
//             -NoProfile ^
//             -ExecutionPolicy Bypass ^
//             -File generate-ci-report.ps1
//             '''
//
//             archiveArtifacts(
//                 artifacts: 'target/surefire-reports/**,ci-report-*.html,ci-report-*.txt',
//                 allowEmptyArchive: true
//             )
//         }
//
//         success {
//             echo '============================================'
//             echo '            CI/CD PIPELINE SUCCESS'
//             echo '============================================'
//             echo 'Unit Tests          : COMPLETED'
//             echo '80 Percent Gate     : PASSED'
//             echo 'Maven Build         : SUCCESS'
//             echo 'Docker Build        : SUCCESS'
//             echo 'Docker Save         : SUCCESS'
//             echo 'SSH Connection      : SUCCESS'
//             echo 'Server Transfer     : SUCCESS'
//             echo 'Docker Load         : SUCCESS'
//             echo 'Docker Network      : READY'
//             echo 'Docker Compose      : SUCCESS'
//             echo 'Container           : RUNNING'
//             echo 'Application Health  : UP'
//             echo 'HTML Report         : GENERATED'
//             echo 'TXT Report          : GENERATED'
//             echo '============================================'
//         }
//
//         failure {
//             echo '============================================'
//             echo '             CI/CD PIPELINE FAILED'
//             echo '============================================'
//             echo 'Check the failed stage above.'
//             echo 'If test percentage is below 80%,'
//             echo 'Docker build and deployment will NOT run.'
//             echo 'HTML/TXT failure report will still be saved.'
//             echo '============================================'
//         }
//     }
// }

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

        stage('Last 20 Commit History') {
            steps {
                bat '''
                @echo off

                echo ========================================
                echo          LAST 20 COMMITS
                echo ========================================

                git log -20 ^
                --date=format:"%%Y-%%m-%%d %%H:%%M:%%S" ^
                --pretty=format:"Author: %%an%%nEmail: %%ae%%nCommit ID: %%H%%nMessage: %%s%%nDate: %%ad%%n----------------------------------------" ^
                > last-20-commits.txt

                type last-20-commits.txt

                echo ========================================
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

                echo Percentage:
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
                echo DEPLOYMENT STOPPED
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
                echo DOCKER IMAGE LOADED
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

                echo DEPLOYED > .ci_deploy.txt

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

                echo UP > .ci_health.txt

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
                file: 'generate-ci-report.ps1',
                text: '''
$ErrorActionPreference = "Continue"

function ReadValue($path, $default) {

    if (Test-Path $path) {
        return (Get-Content $path -Raw).Trim()
    }

    return $default
}

function HtmlEncode($value) {

    if ($null -eq $value) {
        return ""
    }

    return [System.Net.WebUtility]::HtmlEncode(
        [string]$value
    )
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
$deployment = ReadValue ".ci_deploy.txt" "NOT DEPLOYED"
$health = ReadValue ".ci_health.txt" "NOT CHECKED"

$buildNumber = $env:BUILD_NUMBER
$buildUrl = $env:BUILD_URL
$branch = "feature/ci-test"

# =========================================================
# TXT REPORT
# =========================================================

$txtFile = "ci-report-$buildNumber.txt"

$txt = @"
============================================================
                     CI/CD BUILD REPORT
============================================================

BUILD DETAILS

Build Number    : $buildNumber
Branch          : $branch
Build URL       : $buildUrl


COMMIT DETAILS

Commit Author   : $author
Email           : $email
Commit ID       : $commitId
Commit Message  : $message
Commit Date     : $commitDate


TEST RESULTS

Total Tests     : $total
Passed          : $passed
Failed          : $failed
Skipped         : $skipped
Pass Percentage : $percentage%
Test Gate       : $gate


DEPLOYMENT DETAILS

Pipeline Result : $result
Deployment      : $deployment
Application     : $health


============================================================
"@

Set-Content `
    -Path $txtFile `
    -Value $txt `
    -Encoding UTF8


# =========================================================
# LAST 20 COMMITS - HTML TABLE
# =========================================================

$commitRows = ""

try {

    $gitOutput = git log -20 `
        --date=format:"%Y-%m-%d %H:%M:%S" `
        --pretty=format:"%an|||%ae|||%H|||%s|||%ad"

    $index = 1

    foreach ($line in $gitOutput) {

        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }

        $parts = $line -split '\\|\\|\\|', 5

        if ($parts.Count -lt 5) {
            continue
        }

        $cAuthor = HtmlEncode $parts[0]
        $cEmail = HtmlEncode $parts[1]
        $cId = HtmlEncode $parts[2]
        $cMessage = HtmlEncode $parts[3]
        $cDate = HtmlEncode $parts[4]

        $commitRows += @"
<tr>
<td>$index</td>
<td>$cAuthor</td>
<td>$cEmail</td>
<td class="code">$cId</td>
<td>$cMessage</td>
<td>$cDate</td>
</tr>
"@

        $index++
    }

}
catch {

    $commitRows = @"
<tr>
<td colspan="6">Could not load commit history</td>
</tr>
"@
}


# =========================================================
# HTML REPORT
# =========================================================

$htmlFile = "ci-report-$buildNumber.html"

$authorHtml = HtmlEncode $author
$emailHtml = HtmlEncode $email
$commitHtml = HtmlEncode $commitId
$messageHtml = HtmlEncode $message
$dateHtml = HtmlEncode $commitDate
$resultHtml = HtmlEncode $result
$deploymentHtml = HtmlEncode $deployment
$healthHtml = HtmlEncode $health
$gateHtml = HtmlEncode $gate
$buildUrlHtml = HtmlEncode $buildUrl

if ($result -eq "SUCCESS") {
    $resultClass = "success"
}
else {
    $resultClass = "failure"
}

if ($gate -eq "PASS") {
    $gateClass = "success"
}
else {
    $gateClass = "failure"
}

if ($health -eq "UP") {
    $healthClass = "success"
}
else {
    $healthClass = "failure"
}

$html = @"
<!DOCTYPE html>
<html>

<head>

<meta charset="UTF-8">

<title>CI/CD Build Report #$buildNumber</title>

<style>

body {
    font-family: Arial, Helvetica, sans-serif;
    background: #f4f6f8;
    margin: 0;
    padding: 30px;
}

.container {
    max-width: 1200px;
    margin: auto;
    background: white;
    padding: 30px;
    border-radius: 10px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.08);
}

h1 {
    text-align: center;
    margin-bottom: 5px;
}

.subtitle {
    text-align: center;
    color: #666;
    margin-bottom: 30px;
}

.section {
    margin-top: 30px;
}

.section h2 {
    border-bottom: 1px solid #ddd;
    padding-bottom: 8px;
}

table {
    width: 100%;
    border-collapse: collapse;
}

td,
th {
    padding: 10px;
    border-bottom: 1px solid #eee;
    text-align: left;
    vertical-align: top;
}

th {
    background: #f6f6f6;
}

.details td:first-child {
    font-weight: bold;
    width: 230px;
}

.success {
    color: green;
    font-weight: bold;
}

.failure {
    color: red;
    font-weight: bold;
}

.code {
    font-family: Consolas, monospace;
    word-break: break-all;
}

.commit-table {
    font-size: 13px;
}

.footer {
    margin-top: 30px;
    text-align: center;
    color: #777;
    font-size: 13px;
}

</style>

</head>

<body>

<div class="container">

<h1>CI/CD Build Report</h1>

<div class="subtitle">
Build #$buildNumber
</div>


<div class="section">

<h2>Build Details</h2>

<table class="details">

<tr>
<td>Build Number</td>
<td>$buildNumber</td>
</tr>

<tr>
<td>Branch</td>
<td>$branch</td>
</tr>

<tr>
<td>Pipeline Result</td>
<td class="$resultClass">$resultHtml</td>
</tr>

<tr>
<td>Build URL</td>
<td>
<a href="$buildUrlHtml">$buildUrlHtml</a>
</td>
</tr>

</table>

</div>


<div class="section">

<h2>Current Commit Details</h2>

<table class="details">

<tr>
<td>Commit Author</td>
<td>$authorHtml</td>
</tr>

<tr>
<td>Email</td>
<td>$emailHtml</td>
</tr>

<tr>
<td>Commit ID</td>
<td class="code">$commitHtml</td>
</tr>

<tr>
<td>Commit Message</td>
<td>$messageHtml</td>
</tr>

<tr>
<td>Commit Date</td>
<td>$dateHtml</td>
</tr>

</table>

</div>


<div class="section">

<h2>Test Results</h2>

<table class="details">

<tr>
<td>Total Tests</td>
<td>$total</td>
</tr>

<tr>
<td>Passed</td>
<td>$passed</td>
</tr>

<tr>
<td>Failed</td>
<td>$failed</td>
</tr>

<tr>
<td>Skipped</td>
<td>$skipped</td>
</tr>

<tr>
<td>Pass Percentage</td>
<td>$percentage%</td>
</tr>

<tr>
<td>80% Quality Gate</td>
<td class="$gateClass">$gateHtml</td>
</tr>

</table>

</div>


<div class="section">

<h2>Deployment</h2>

<table class="details">

<tr>
<td>Deployment Status</td>
<td>$deploymentHtml</td>
</tr>

<tr>
<td>Application Health</td>
<td class="$healthClass">$healthHtml</td>
</tr>

</table>

</div>


<div class="section">

<h2>Last 20 Commit History</h2>

<table class="commit-table">

<thead>

<tr>
<th>#</th>
<th>Author</th>
<th>Email</th>
<th>Commit ID</th>
<th>Message</th>
<th>Date</th>
</tr>

</thead>

<tbody>

$commitRows

</tbody>

</table>

</div>


<div class="footer">
Generated automatically by Jenkins
</div>

</div>

</body>

</html>
"@

Set-Content `
    -Path $htmlFile `
    -Value $html `
    -Encoding UTF8


# =========================================================
# CONSOLE SUMMARY
# =========================================================

Write-Host ""
Write-Host "============================================================"
Write-Host "                     CI/CD BUILD REPORT"
Write-Host "============================================================"

Write-Host ""
Write-Host "Build Number   : $buildNumber"
Write-Host "Branch         : $branch"

Write-Host ""
Write-Host "Commit Author  : $author"
Write-Host "Email          : $email"
Write-Host "Commit ID      : $commitId"
Write-Host "Message        : $message"

Write-Host ""
Write-Host "Total Tests    : $total"
Write-Host "Passed         : $passed"
Write-Host "Failed         : $failed"
Write-Host "Skipped        : $skipped"
Write-Host "Pass Rate      : $percentage%"
Write-Host "Test Gate      : $gate"

Write-Host ""
Write-Host "Pipeline       : $result"
Write-Host "Deployment     : $deployment"
Write-Host "Health         : $health"

Write-Host ""
Write-Host "HTML Report    : $htmlFile"
Write-Host "TXT Report     : $txtFile"
Write-Host "Commit History : last-20-commits.txt"

Write-Host "============================================================"
'''
            )

            bat '''
            powershell.exe ^
            -NoProfile ^
            -ExecutionPolicy Bypass ^
            -File generate-ci-report.ps1
            '''

            archiveArtifacts(
                artifacts: 'target/surefire-reports/**,ci-report-*.html,ci-report-*.txt,last-20-commits.txt',
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
            echo 'HTML Report         : GENERATED'
            echo 'TXT Report          : GENERATED'
            echo 'Last 20 Commits     : GENERATED'
            echo '============================================'
        }

        failure {
            echo '============================================'
            echo '             CI/CD PIPELINE FAILED'
            echo '============================================'
            echo 'Check the failed stage above.'
            echo 'If test percentage is below 80%,'
            echo 'Docker build and deployment will NOT run.'
            echo 'HTML/TXT report will still be generated.'
            echo '============================================'
        }
    }
}