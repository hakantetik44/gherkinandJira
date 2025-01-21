pipeline {
    agent any

    tools {
        maven 'maven'
        jdk 'JDK17'
    }

    environment {
        JIRA_CREDENTIALS = credentials('jira-credentials')
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
                withCredentials([usernamePassword(credentialsId: 'jira-credentials', 
                                                usernameVariable: 'JIRA_USER', 
                                                passwordVariable: 'JIRA_PASS')]) {
                    script {
                        def cucumberJson = readFile 'target/cucumber-reports/cucumber.json'
                        def response = httpRequest(
                            url: 'https://somfycucumber.atlassian.net/rest/raven/2.0/import/execution/cucumber',
                            httpMode: 'POST',
                            customHeaders: [[name: 'Authorization', value: "Basic ${JIRA_PASS}"]], 
                            requestBody: cucumberJson
                        )
                        println("Xray Response: ${response.status}")
                    }
                }
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: [
                    'test-reports.zip',
                    'target/cucumber-reports/**/*'
                ].join(', '), fingerprint: true, allowEmptyArchive: true
                
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
            node(null) {
                cleanWs()
            }
        }

        success {
            node(null) {
                echo "✅ Tests completed successfully"
            }
        }

        failure {
            node(null) {
                echo "❌ Tests failed"
            }
        }
    }
} 