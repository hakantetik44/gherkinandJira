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
                            echo "Using auth: Basic ${auth}"
                            
                            if [ -f "target/cucumber-reports/cucumber.json" ]; then
                                echo "Uploading test results..."
                                curl -v -X POST \
                                     -H "Authorization: Basic ${auth}" \
                                     -H "Content-Type: application/json" \
                                     -H "Accept: application/json" \
                                     --data @target/cucumber-reports/cucumber.json \
                                     "https://somfycucumber.atlassian.net/rest/raven/1.0/import/execution/cucumber" 2>&1 | tee xray-response.log
                                
                                echo "Xray response:"
                                cat xray-response.log
                            else
                                echo "No cucumber.json file found!"
                                exit 1
                            fi
                        """
                        
                        archiveArtifacts artifacts: 'xray-response.log', allowEmptyArchive: true
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