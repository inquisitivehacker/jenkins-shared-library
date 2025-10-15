def call(Map config = [:]) {
    pipeline {
        agent { label 'debian-master' }

        tools {
            jdk 'JDK-21'  // Make sure this matches the Jenkins-installed JDK
        }

        environment {
            // SonarQube global token stored in Jenkins credentials
            SONAR_AUTH_TOKEN = credentials('sonar-global-token')  
        }

        options {
            skipDefaultCheckout(true)  // We'll handle checkout manually with credentials
        }

        stages {

            stage('Clean Workspace') {
                steps {
                    deleteDir()  // Ensures no old files or credentials interfere
                }
            }

            stage('Checkout') {
                steps {
                    checkout([$class: 'GitSCM',
                        branches: [[name: '*/master']],
                        doGenerateSubmoduleConfigurations: false,
                        extensions: [],
                        userRemoteConfigs: [[
                            url: 'https://github.com/inquisitivehacker/k6-loadTesting-infrastructure.git',
                            credentialsId: 'github-token'  // Your new GitHub PAT
                        ]]
                    ])
                }
            }

            stage('SonarQube Analysis') {
                steps {
                    script {
                        withSonarQubeEnv('MySonarQube') {
                            // Use Java from tools block explicitly
                            sh """
                                ${env.JAVA_HOME}/bin/sonar-scanner \
                                -Dsonar.login=${SONAR_AUTH_TOKEN}
                            """
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

        post {
            always {
                cleanWs() // Optional: clean workspace after build
            }
        }
    }
}
