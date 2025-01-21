pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'JDK17'
    }

    options {
        skipDefaultCheckout(false)
    }

    environment {
        JIRA_CREDS = credentials('jira-credentials')
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
                        currentBuild.result = 'FAILURE'
                        error("Test execution failed: ${e.message}")
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

        stage('Upload to Xray') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'jira-credentials',
                        usernameVariable: 'JIRA_USER',
                        passwordVariable: 'JIRA_TOKEN'
                    )]) {
                        def auth = "${JIRA_USER}:${JIRA_TOKEN}".bytes.encodeBase64().toString()
                        def response = httpRequest(
                            url: 'https://somfycucumber.atlassian.net/rest/raven/2.0/import/execution/cucumber',
                            httpMode: 'POST',
                            customHeaders: [[name: 'Authorization', value: "Basic ${auth}"]], 
                            requestBody: readFile('target/cucumber-reports/cucumber.json')
                        )
                        echo "Xray Response: ${response.status}"
                    }
                }
            }
        }

        stage('Archive Reports') {
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
        failure {
            echo "❌ Tests failed"
        }
    }
} 