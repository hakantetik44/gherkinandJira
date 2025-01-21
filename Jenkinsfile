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
                    credentialsId: 'somfy',
                    usernameVariable: 'JIRA_USER',
                    passwordVariable: 'JIRA_TOKEN'
                )]) {
                    script {
                        sh '''
                            mkdir -p xray-results
                            if [ -f "target/cucumber-reports/cucumber.json" ]; then
                                echo "Creating Xray import file..."
                                
                                # Get test result status from cucumber.json
                                TEST_STATUS=$(cat target/cucumber-reports/cucumber.json | grep -o '"status": "[^"]*"' | head -1 | cut -d'"' -f4)
                                if [ "$TEST_STATUS" = "passed" ]; then
                                    STATUS="PASS"
                                else
                                    STATUS="FAIL"
                                fi
                                
                                # Create base64 auth
                                AUTH=$(echo -n "$JIRA_USER:$JIRA_TOKEN" | base64)
                                
                                # Create Xray import file with Cucumber results
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
                                
                                # Upload to Xray with detailed output
                                curl -v -X POST \
                                    -H "Authorization: Basic $AUTH" \
                                    -H "Content-Type: application/json" \
                                    -H "Accept: application/json" \
                                    --data @xray-results/xray-import.json \
                                    "https://somfycucumber.atlassian.net/rest/raven/1.0/import/execution/cucumber" 2>&1 | tee xray-response.log
                                
                                # Check response
                                if [ -f xray-response.log ]; then
                                    echo "Xray API Response:"
                                    cat xray-response.log
                                fi
                            else
                                echo "No cucumber.json file found!"
                                exit 1
                            fi
                        '''
                        archiveArtifacts artifacts: 'xray-results/xray-import.json,xray-response.log', allowEmptyArchive: true
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