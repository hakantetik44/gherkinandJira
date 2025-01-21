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

        stage('Upload to Xray') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'jira-api',
                    usernameVariable: 'JIRA_USER',
                    passwordVariable: 'JIRA_TOKEN'
                )]) {
                    sh '''
                        echo -n "${JIRA_USER}:${JIRA_TOKEN}" | base64 > auth.txt
                        AUTH=$(cat auth.txt)
                        
                        # Test execution bilgilerini içeren JSON oluştur
                        cat > execution.json << EOF
                        {
                            "info": {
                                "summary": "Automated test execution from Jenkins",
                                "description": "Test results from Jenkins pipeline",
                                "project": "SMF2",
                                "testPlanKey": "SMF2-2",
                                "testEnvironments": ["QA"]
                            },
                            "tests": [$(cat target/cucumber-reports/cucumber.json)]
                        }
EOF
                        
                        # Xray'e gönder
                        curl -v -X POST \\
                        -H "Authorization: Basic $AUTH" \\
                        -H "Content-Type: application/json" \\
                        --data @execution.json \\
                        "https://somfycucumber.atlassian.net/rest/raven/2.0/import/execution/cucumber"
                        
                        rm auth.txt execution.json
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