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

        stage('GitHub Status - Pending') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'github-status-token',
                        variable: 'GITHUB_TOKEN'
                    )
                ]) {
                    bat '''
                    curl.exe -L -X POST ^
                    -H "Authorization: Bearer %GITHUB_TOKEN%" ^
                    -H "Accept: application/vnd.github+json" ^
                    -H "X-GitHub-Api-Version: 2022-11-28" ^
                    https://api.github.com/repos/HealthinRhowina06/user-analysis-service-ci-test/statuses/%GIT_COMMIT% ^
                    -d "{\\"state\\":\\"pending\\",\\"context\\":\\"ci/jenkins\\",\\"description\\":\\"Jenkins CI is running\\"}"
                    '''
                }
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
            withCredentials([
                string(
                    credentialsId: 'github-status-token',
                    variable: 'GITHUB_TOKEN'
                )
            ]) {
                bat '''
                curl.exe -L -X POST ^
                -H "Authorization: Bearer %GITHUB_TOKEN%" ^
                -H "Accept: application/vnd.github+json" ^
                -H "X-GitHub-Api-Version: 2022-11-28" ^
                https://api.github.com/repos/HealthinRhowina06/user-analysis-service-ci-test/statuses/%GIT_COMMIT% ^
                -d "{\\"state\\":\\"success\\",\\"context\\":\\"ci/jenkins\\",\\"description\\":\\"Build and unit tests passed\\"}"
                '''
            }

            echo 'CI PIPELINE SUCCESS'
        }

        failure {
            withCredentials([
                string(
                    credentialsId: 'github-status-token',
                    variable: 'GITHUB_TOKEN'
                )
            ]) {
                bat '''
                curl.exe -L -X POST ^
                -H "Authorization: Bearer %GITHUB_TOKEN%" ^
                -H "Accept: application/vnd.github+json" ^
                -H "X-GitHub-Api-Version: 2022-11-28" ^
                https://api.github.com/repos/HealthinRhowina06/user-analysis-service-ci-test/statuses/%GIT_COMMIT% ^
                -d "{\\"state\\":\\"failure\\",\\"context\\":\\"ci/jenkins\\",\\"description\\":\\"Build or unit tests failed\\"}"
                '''
            }

            echo 'TEST/BUILD FAILED - MERGE AND DEPLOYMENT MUST STOP'
        }
    }
}