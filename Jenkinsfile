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
                        sh """
                            # Get Xray Cloud API token
                            XRAY_TOKEN=\$(curl -H "Content-Type: application/json" \
                                -X POST \
                                --data '{"client_id": "${JIRA_USER}","client_secret": "${JIRA_TOKEN}"}' \
                                https://xray.cloud.getxray.app/api/v2/authenticate)
                            
                            echo "Xray token obtained: \${XRAY_TOKEN}"
                            
                            if [ -f "target/cucumber-reports/cucumber.json" ]; then
                                # Upload test results to Xray Cloud
                                curl -X POST \
                                     -H "Content-Type: application/json" \
                                     -H "Authorization: Bearer \${XRAY_TOKEN}" \
                                     --data @target/cucumber-reports/cucumber.json \
                                     "https://xray.cloud.getxray.app/api/v2/import/execution/cucumber" | tee xray-response.log
                                
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