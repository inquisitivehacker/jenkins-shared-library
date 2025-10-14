def call(Map config = [:]) {
    pipeline {
        agent { label 'debian-master' } 
         tools {
            jdk 'JDK-21' 
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
                        if (env.CHANGE_ID) {
                            echo "INFO: Pull Request build detected. Running SonarQube PR analysis."
                            withSonarQubeEnv('MySonarQube') {
                                // Force the scanner to use the Java version from the 'tools' block
                                sh """
                                    ${env.JAVA_HOME}/bin/sonar-scanner \
                                    -Dsonar.pullrequest.base=main \
                                    -Dsonar.pullrequest.branch=${env.CHANGE_BRANCH} \
                                    -Dsonar.pullrequest.key=${env.CHANGE_ID}
                                """
                            }
                        } else {
                            echo "INFO: Branch push detected. Running standard SonarQube branch analysis."
                            withSonarQubeEnv('MySonarQube') {
                                // Force the scanner to use the Java version from the 'tools' block
                                sh """
                                    ${env.JAVA_HOME}/bin/sonar-scanner
                                """
                            }
                        }
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