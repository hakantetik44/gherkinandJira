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
                withCredentials([string(credentialsId: 'xray-api-key', variable: 'XRAY_API_KEY')]) {
                    script {
                        sh """
                            echo "Checking test results..."
                            if [ ! -f "target/cucumber-reports/cucumber.json" ]; then
                                echo "Error: cucumber.json not found!"
                                exit 1
                            fi
                            
                            echo "Preparing test results for Xray..."
                            TEST_EXECUTION_KEY="SMF-2"
                            
                            echo "Uploading results to Xray Test Execution: \${TEST_EXECUTION_KEY}"
                            RESPONSE=\$(curl -v -H "Content-Type: application/json" \
                                 -H "Authorization: Bearer ${XRAY_API_KEY}" \
                                 -X POST \
                                 --data @target/cucumber-reports/cucumber.json \
                                 "https://xray.cloud.getxray.app/api/v2/import/execution/cucumber/\${TEST_EXECUTION_KEY}" 2>&1)
                            
                            echo "\${RESPONSE}" > xray-response.log
                            
                            if echo "\${RESPONSE}" | grep -q "error"; then
                                echo "Error uploading to Xray:"
                                cat xray-response.log
                                exit 1
                            else
                                echo "Successfully uploaded test results to Xray"
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