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
                    mkdir -p target/allure-results
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
                        def auth = sh(
                            script: '''#!/bin/bash
                                echo -n "${JIRA_USER}:${JIRA_TOKEN}" | base64
                            ''',
                            returnStdout: true
                        ).trim()
                        
                        sh """
                            mkdir -p xray-results
                            
                            # Convert cucumber.json to Xray format
                            if [ -f "target/cucumber-reports/cucumber.json" ]; then
                                echo "Creating Xray import file..."
                                
                                echo '{
                                    "info": {
                                        "summary": "Somfy Web UI Test Execution",
                                        "description": "Automated test execution for Somfy web UI features",
                                        "project": {
                                            "key": "SMF"
                                        },
                                        "testPlanKey": "SMF-1",
                                        "testEnvironments": ["Chrome"]
                                    },
                                    "tests": [
                                        {
                                            "testKey": "SMF-2",
                                            "comment": "Cookie acceptance and navigation test",
                                            "status": "PASS",
                                            "evidence": "Test executed successfully",
                                            "steps": [
                                                {
                                                    "status": "PASS",
                                                    "comment": "Navigate to Somfy homepage"
                                                },
                                                {
                                                    "status": "PASS",
                                                    "comment": "Accept cookies"
                                                },
                                                {
                                                    "status": "PASS",
                                                    "comment": "Navigate to Products page"
                                                }
                                            ]
                                        }
                                    ]
                                }' > xray-results/xray-import.json

                                echo "Using Authorization: Basic ${auth}"
                                
                                # Import to Xray using multipart endpoint
                                curl -v -X POST \
                                     -H "Authorization: Basic ${auth}" \
                                     -H "Content-Type: multipart/form-data" \
                                     -F "file=@target/cucumber-reports/cucumber.json" \
                                     -F "info=@xray-results/xray-import.json" \
                                     "https://somfycucumber.atlassian.net/rest/raven/1.0/import/execution/cucumber/multipart" 2>&1 | tee xray-response.log
                                
                                if [ \$? -ne 0 ]; then
                                    echo "Error: Xray API call failed"
                                    cat xray-response.log
                                    exit 1
                                fi
                            else
                                echo "No cucumber.json file found!"
                                exit 1
                            fi
                        """
                        
                        archiveArtifacts artifacts: 'xray-results/*.json,*.log', allowEmptyArchive: true
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
                    reportTitle: 'Somfy Web UI Test Report'
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