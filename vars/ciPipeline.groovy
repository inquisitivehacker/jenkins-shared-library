def call(Map config = [:]) {
    pipeline {
        agent { label 'debian-master' } 

        environment {
            // Jenkins credential ID for your global SonarQube token
            SONAR_AUTH_TOKEN = credentials('sonar-global-token')
            // SonarQube server URL
            SONARQUBE_URL = 'http://127.0.0.1:9000'
            // Docker image for SonarScanner
            SONAR_SCANNER_IMAGE = 'sonarsource/sonar-scanner-cli:11.4.0.2044_7.2.0'
        }

        stages {
            stage('Checkout') {
                steps {
                    checkout scm
                }
            }

            stage('SonarQube Analysis') {
                steps {
                    script {
                        echo "Running SonarQube analysis using Dockerized scanner..."
                        sh """
                        docker run --rm -e SONAR_HOST_URL=$SONARQUBE_URL -e SONAR_LOGIN=$SONAR_AUTH_TOKEN \
                            -v \$(pwd):/usr/src -w /usr/src $SONAR_SCANNER_IMAGE \
                            sonar-scanner
                        """
                    }
                }
            }

            stage('Build Project') {
                steps {
                    script {
                        if (config.buildCommands) {
                            echo "Running custom build commands..."
                            config.buildCommands.each { command ->
                                sh command
                            }
                        } else {
                            echo "No build commands provided. Skipping."
                        }
                    }
                }
            }
        }
    }
}
