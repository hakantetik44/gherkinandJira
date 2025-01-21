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
                    def testsPassed = fileExists 'target/cucumber-reports/cucumber.json'
                    
                    withCredentials([usernamePassword(
                        credentialsId: 'jira-api',
                        usernameVariable: 'JIRA_USER',
                        passwordVariable: 'JIRA_TOKEN'
                    )]) {
                        sh """
                            # Base64 encode credentials
                            echo -n "${JIRA_USER}:${JIRA_TOKEN}" | base64 > auth.txt
                            AUTH=\$(cat auth.txt)
                            
                            # Start test execution
                            curl -X PUT \\
                            -H "Authorization: Basic \$AUTH" \\
                            -H "Content-Type: application/json" \\
                            "https://somfycucumber.atlassian.net/rest/api/2/issue/SMF2-2/transitions" \\
                            -d '{"transition": {"id": "11"}}'
                            
                            # Import test results
                            if [ -f "target/cucumber-reports/cucumber.json" ]; then
                                curl -X POST \\
                                -H "Authorization: Basic \$AUTH" \\
                                -H "Content-Type: application/json" \\
                                --data-binary @target/cucumber-reports/cucumber.json \\
                                "https://somfycucumber.atlassian.net/rest/raven/1.0/import/execution/cucumber?projectKey=SMF2&testExecKey=SMF2-2"
                                
                                # Update status based on test results
                                if [ \$? -eq 0 ]; then
                                    curl -X PUT \\
                                    -H "Authorization: Basic \$AUTH" \\
                                    -H "Content-Type: application/json" \\
                                    "https://somfycucumber.atlassian.net/rest/api/2/issue/SMF2-2/transitions" \\
                                    -d '{"transition": {"id": "31"}}'
                                else
                                    curl -X PUT \\
                                    -H "Authorization: Basic \$AUTH" \\
                                    -H "Content-Type: application/json" \\
                                    "https://somfycucumber.atlassian.net/rest/api/2/issue/SMF2-2/transitions" \\
                                    -d '{"transition": {"id": "41"}}'
                                fi
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