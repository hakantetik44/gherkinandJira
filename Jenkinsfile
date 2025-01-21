pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'JDK17'
    }

    stages {
        stage('Initialize') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        stage('Run Tests') {
            steps {
                sh "mvn clean test"
            }
        }

        stage('Generate Reports') {
            steps {
                sh """
                    mkdir -p test-reports
                    cp -r target/cucumber-reports/* test-reports/ || true
                    cp -r target/surefire-reports test-reports/ || true
                    cp -r target/allure-results test-reports/ || true
                    zip -r test-reports.zip test-reports/
                """
            }
        }

        stage('Update Xray Test Results') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'jira-api',
                    usernameVariable: 'JIRA_USER',
                    passwordVariable: 'JIRA_TOKEN'
                )]) {
                    sh '''
                        echo -n "${JIRA_USER}:${JIRA_TOKEN}" | base64 > auth.txt
                        AUTH=$(cat auth.txt)
                        
                        # Get Xray Cloud API token
                        XRAY_TOKEN=$(curl -H "Content-Type: application/json" -X POST --data "{ \\"client_id\\": \\"${JIRA_USER}\\", \\"client_secret\\": \\"${JIRA_TOKEN}\\" }" https://xray.cloud.getxray.app/api/v2/authenticate)
                        
                        # Update test execution results
                        curl -X POST \\
                        -H "Content-Type: application/json" \\
                        -H "Authorization: Bearer ${XRAY_TOKEN}" \\
                        --data-binary @target/cucumber-reports/cucumber.json \\
                        "https://xray.cloud.getxray.app/api/v2/import/execution/cucumber/multipart?testExecKey=SMF2-2"

                        rm auth.txt
                    '''
                }
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts(
                    artifacts: 'test-reports.zip,target/cucumber-reports/**/*',
                    allowEmptyArchive: true
                )
                
                allure([
                    reportBuildPolicy: 'ALWAYS',
                    results: [[path: 'target/allure-results']]
                ])

                cucumber(
                    buildStatus: 'UNSTABLE',
                    fileIncludePattern: '**/cucumber.json',
                    jsonReportDirectory: 'target/cucumber-reports',
                    reportTitle: 'Cucumber Test Raporu'
                )
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "✅ Tests completed successfully"
        }
        failure {
            echo "❌ Tests failed"
        }
    }
} 