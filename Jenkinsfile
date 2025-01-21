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
                        sh "mvn clean test"
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
                script {
                    sh '''
                        mkdir -p xray-results
                        echo '{
                            "testExecutionKey": "SMF2-2",
                            "info": {
                                "summary": "Test Execution Results",
                                "description": "Results from Jenkins Pipeline",
                                "project": "SMF2",
                                "version": "1.0"
                            },
                            "tests": [
                        ' > xray-results/xray-import.json
                        
                        cat target/cucumber-reports/cucumber.json >> xray-results/xray-import.json
                        
                        echo ']}' >> xray-results/xray-import.json
                    '''
                    
                    archiveArtifacts artifacts: 'xray-results/xray-import.json', allowEmptyArchive: true
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