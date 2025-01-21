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
                script {
                    try {
                        sh "mvn clean test"
                        currentBuild.result = 'SUCCESS'
                    } catch (Exception e) {
                        currentBuild.result = 'UNSTABLE'
                        echo "Test execution failed: ${e.message}"
                    }
                }
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
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'jira-api',
                        usernameVariable: 'JIRA_USER',
                        passwordVariable: 'JIRA_TOKEN'
                    )]) {
                        sh """
                            # Base64 encode credentials
                            echo -n "${JIRA_USER}:${JIRA_TOKEN}" | base64 > auth.txt
                            AUTH=\$(cat auth.txt)
                            
                            # Create test execution
                            echo "Creating test execution..."
                            EXEC_RESPONSE=\$(curl -s -X POST \\
                            -H "Authorization: Basic \$AUTH" \\
                            -H "Content-Type: application/json" \\
                            "https://somfycucumber.atlassian.net/rest/api/2/issue" \\
                            -d '{
                                "fields": {
                                    "project": {"key": "SMF2"},
                                    "summary": "Test Execution - '"\$(date +%Y-%m-%d_%H-%M-%S)"'",
                                    "description": "Automated test execution from Jenkins",
                                    "issuetype": {"name": "Test Execution"}
                                }
                            }')
                            echo "Execution Response: \$EXEC_RESPONSE"
                            
                            # Get execution key
                            EXEC_KEY=\$(echo \$EXEC_RESPONSE | grep -o '"key":"[^"]*' | cut -d'"' -f4)
                            echo "Test Execution Key: \$EXEC_KEY"
                            
                            # Link test to execution
                            echo "Linking test to execution..."
                            curl -s -X POST \\
                            -H "Authorization: Basic \$AUTH" \\
                            -H "Content-Type: application/json" \\
                            "https://somfycucumber.atlassian.net/rest/api/2/issue/\$EXEC_KEY/links" \\
                            -d '{
                                "type": {"name": "Test"},
                                "inwardIssue": {"key": "SMF2-1"}
                            }'
                            
                            # Upload test results
                            echo "Uploading test results..."
                            RESULT_RESPONSE=\$(curl -s -X POST \\
                            -H "Authorization: Basic \$AUTH" \\
                            -H "Content-Type: application/json" \\
                            --data-binary @target/cucumber-reports/cucumber.json \\
                            "https://somfycucumber.atlassian.net/rest/raven/1.0/import/execution/cucumber?projectKey=SMF2&testExecKey=\$EXEC_KEY")
                            echo "Result Response: \$RESULT_RESPONSE"
                            
                            # Update test status
                            if grep -q '"status": "passed"' target/cucumber-reports/cucumber.json; then
                                echo "Tests passed, updating status..."
                                curl -s -X PUT \\
                                -H "Authorization: Basic \$AUTH" \\
                                -H "Content-Type: application/json" \\
                                "https://somfycucumber.atlassian.net/rest/api/2/issue/SMF2-1" \\
                                -d '{
                                    "fields": {
                                        "status": {"name": "Done"}
                                    }
                                }'
                            else
                                echo "Tests failed, updating status..."
                                curl -s -X PUT \\
                                -H "Authorization: Basic \$AUTH" \\
                                -H "Content-Type: application/json" \\
                                "https://somfycucumber.atlassian.net/rest/api/2/issue/SMF2-1" \\
                                -d '{
                                    "fields": {
                                        "status": {"name": "Failed"}
                                    }
                                }'
                            fi
                            
                            rm auth.txt
                        """
                    }
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
        unstable {
            echo "⚠️ Tests are unstable"
        }
    }
} 