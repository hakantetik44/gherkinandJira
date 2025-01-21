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
                sh "mvn clean test"
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
                withCredentials([usernamePassword(
                    credentialsId: 'jira-api',
                    usernameVariable: 'JIRA_USER',
                    passwordVariable: 'JIRA_TOKEN'
                )]) {
                    sh '''
                        # Base64 encode credentials
                        echo -n "${JIRA_USER}:${JIRA_TOKEN}" | base64 > auth.txt
                        AUTH=$(cat auth.txt)
                        
                        # Update test execution status in Jira
                        curl -X PUT \\
                        -H "Authorization: Basic $AUTH" \\
                        -H "Content-Type: application/json" \\
                        "https://somfycucumber.atlassian.net/rest/api/2/issue/SMF2-2/transitions" \\
                        -d '{"transition": {"id": "11"}}'  # 11 is the ID for "In Progress"
                        
                        # Import test results to Xray
                        curl -X POST \\
                        -H "Authorization: Basic $AUTH" \\
                        -H "Content-Type: application/json" \\
                        --data-binary @target/cucumber-reports/cucumber.json \\
                        "https://somfycucumber.atlassian.net/rest/raven/1.0/import/execution/cucumber?projectKey=SMF2&testExecKey=SMF2-2"
                        
                        # Update test execution status based on test results
                        if [ $? -eq 0 ]; then
                            # If tests passed, transition to Done
                            curl -X PUT \\
                            -H "Authorization: Basic $AUTH" \\
                            -H "Content-Type: application/json" \\
                            "https://somfycucumber.atlassian.net/rest/api/2/issue/SMF2-2/transitions" \\
                            -d '{"transition": {"id": "31"}}'  # 31 is the ID for "Done"
                        else
                            # If tests failed, transition to Failed
                            curl -X PUT \\
                            -H "Authorization: Basic $AUTH" \\
                            -H "Content-Type: application/json" \\
                            "https://somfycucumber.atlassian.net/rest/api/2/issue/SMF2-2/transitions" \\
                            -d '{"transition": {"id": "41"}}'  # 41 is the ID for "Failed"
                        fi
                        
                        rm auth.txt
                    '''
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
    }
} 