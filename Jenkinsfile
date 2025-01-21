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
                        sh """
                            mkdir -p target/cucumber-reports
                            mvn clean test
                        """
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

        stage('Generate Xray Results') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'jira-credentials',
                    usernameVariable: 'JIRA_USER',
                    passwordVariable: 'JIRA_TOKEN'
                )]) {
                    script {
                        sh '''
                            mkdir -p xray-results
                            
                            # Create base64 auth
                            AUTH=$(echo -n "$JIRA_USER:$JIRA_TOKEN" | base64)
                            
                            # Test Jira authentication
                            echo "Testing Jira authentication..."
                            curl -v -H "Authorization: Basic $AUTH" \
                                 -H "Content-Type: application/json" \
                                 "https://somfycucumber.atlassian.net/rest/api/2/myself" > auth-test.log
                            
                            # Test execution check
                            echo "Checking test execution..."
                            curl -v -H "Authorization: Basic $AUTH" \
                                 -H "Content-Type: application/json" \
                                 "https://somfycucumber.atlassian.net/rest/api/2/issue/SMF2-2" > execution-test.log
                            
                            if [ -f "target/cucumber-reports/cucumber.json" ]; then
                                echo "Creating Xray import file..."
                                
                                # Get test result status from cucumber.json
                                TEST_STATUS=$(cat target/cucumber-reports/cucumber.json | grep -o '"status": "[^"]*"' | head -1 | cut -d'"' -f4)
                                if [ "$TEST_STATUS" = "passed" ]; then
                                    STATUS="PASS"
                                else
                                    STATUS="FAIL"
                                fi
                                
                                # Create test payload
                                echo '{
                                    "info": {
                                        "summary": "Test Execution Results from Jenkins",
                                        "description": "Automated test execution",
                                        "project": "SMF2",
                                        "user": "'$JIRA_USER'"
                                    },
                                    "testExecutionKey": "SMF2-2",
                                    "tests": [
                                        {
                                            "testKey": "SMF2-1",
                                            "status": "'$STATUS'",
                                            "comment": "Executed from Jenkins Pipeline",
                                            "evidence": "'$(cat target/cucumber-reports/cucumber.json | base64)'"
                                        }
                                    ]
                                }' > xray-results/xray-import.json
                                
                                # Debug output
                                echo "Uploading results to Xray..."
                                echo "Using credentials: $JIRA_USER"
                                echo "Auth header: Basic $AUTH"
                                
                                # Test Xray API
                                curl -v -X POST \
                                     -H "Authorization: Basic $AUTH" \
                                     -H "Content-Type: application/json" \
                                     -H "Accept: application/json" \
                                     --data @xray-results/xray-import.json \
                                     "https://somfycucumber.atlassian.net/rest/raven/1.0/import/execution/cucumber" 2>&1 | tee xray-response.log
                                
                                # Check responses
                                echo "=== Authentication Test Response ==="
                                cat auth-test.log
                                echo "=== Test Execution Check Response ==="
                                cat execution-test.log
                                echo "=== Xray Import Response ==="
                                cat xray-response.log
                            else
                                echo "No cucumber.json file found!"
                                exit 1
                            fi
                        '''
                        archiveArtifacts artifacts: 'xray-results/xray-import.json,*.log', allowEmptyArchive: true
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