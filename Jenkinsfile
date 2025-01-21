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

        stage('Upload to Xray') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'jira-api',
                    usernameVariable: 'JIRA_USER',
                    passwordVariable: 'JIRA_TOKEN'
                )]) {
                    sh '''
                        echo -n "${JIRA_USER}:${JIRA_TOKEN}" | base64 > auth.txt
                        AUTH=$(cat auth.txt)
                        
                        # Create test execution
                        curl -v -X POST \\
                        -H "Authorization: Basic $AUTH" \\
                        -H "Content-Type: application/json" \\
                        --data '{
                            "fields": {
                                "project": {
                                    "key": "SMF2"
                                },
                                "summary": "Test Execution - $(date '+%Y-%m-%d %H:%M:%S')",
                                "description": "Automated test execution",
                                "issuetype": {
                                    "name": "Test Execution"
                                }
                            }
                        }' \\
                        "https://somfycucumber.atlassian.net/rest/api/2/issue" > execution.json

                        # Get execution key
                        EXECUTION_KEY=$(cat execution.json | grep -o '"key":"[^"]*' | cut -d'"' -f4)
                        
                        # Upload test results
                        curl -v -X POST \\
                        -H "Authorization: Basic $AUTH" \\
                        -H "Content-Type: application/json" \\
                        --data-binary @target/cucumber-reports/cucumber.json \\
                        "https://somfycucumber.atlassian.net/rest/raven/1.0/import/execution/cucumber?projectKey=SMF2&testExecKey=${EXECUTION_KEY}&testPlanKey=SMF2-2"

                        rm auth.txt execution.json
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