pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'JDK17'
    }

    options {
        skipDefaultCheckout(false)
    }

    stages {
        stage('Initialize') {
            steps {
                cleanWs()
                git branch: 'main',
                    url: 'https://github.com/hakantetik44/gherkinandJira.git'
            }
        }

        stage('Run Tests') {
            steps {
                script {
                    try {
                        sh "mvn clean test"
                    } catch (Exception e) {
                        unstable("Test execution failed: ${e.message}")
                        return
                    }
                }
            }
        }

        stage('Generate Reports') {
            when {
                not {
                    equals expected: 'FAILURE', actual: currentBuild.result
                }
            }
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
            when {
                not {
                    equals expected: 'FAILURE', actual: currentBuild.result
                }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'jira-api',
                    usernameVariable: 'JIRA_USER',
                    passwordVariable: 'JIRA_TOKEN'
                )]) {
                    script {
                        try {
                            def cucumberJson = readFile(file: 'target/cucumber-reports/cucumber.json')
                            def encodedAuth = "${JIRA_USER}:${JIRA_TOKEN}".bytes.encodeBase64().toString()
                            
                            def response = sh(
                                script: """
                                    curl -X POST \
                                    -H 'Authorization: Basic ${encodedAuth}' \
                                    -H 'Content-Type: application/json' \
                                    --data-binary @target/cucumber-reports/cucumber.json \
                                    'https://somfycucumber.atlassian.net/rest/raven/2.0/import/execution/cucumber'
                                """,
                                returnStdout: true
                            )
                            echo "Xray Response: ${response}"
                        } catch (Exception e) {
                            echo "Warning: Failed to upload results to Xray: ${e.message}"
                            unstable("Xray upload failed")
                        }
                    }
                }
            }
        }

        stage('Archive Reports') {
            when {
                not {
                    equals expected: 'FAILURE', actual: currentBuild.result
                }
            }
            steps {
                archiveArtifacts(
                    artifacts: 'test-reports.zip,target/cucumber-reports/**/*',
                    fingerprint: true,
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
        unstable {
            echo "⚠️ Build is unstable"
        }
        failure {
            echo "❌ Tests failed"
        }
    }
} 