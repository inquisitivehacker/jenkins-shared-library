// This is the corrected, final version of your shared library script
def call(Map config = [:]) {
    pipeline {
        agent { label 'debian-master' }

        environment {
            SONAR_AUTH_TOKEN = credentials('sonar-global-token')
        }

        stages {
            stage('Checkout') {
                steps {
                    // Securely check out the code using your GitHub token
                    checkout([$class: 'GitSCM',
                        branches: [[name: '*/master']], // Or '*/main'
                        userRemoteConfigs: [[
                            url: "https://github.com/${env.CHANGE_REPO}.git", // Dynamically get repo URL
                            credentialsId: 'github-token'
                        ]]
                    ])
                }
            }

            stage('SonarQube Analysis') {
                steps {
                    script {
                        withSonarQubeEnv('MySonarQube') {
                            // ** RESTORED PULL REQUEST LOGIC **
                            if (env.CHANGE_ID) {
                                echo "INFO: Pull Request build detected. Running SonarQube PR analysis."
                                sh """
                                    sonar-scanner \
                                    -Dsonar.login=${SONAR_AUTH_TOKEN} \
                                    -Dsonar.pullrequest.base=${env.CHANGE_TARGET} \
                                    -Dsonar.pullrequest.branch=${env.BRANCH_NAME} \
                                    -Dsonar.pullrequest.key=${env.CHANGE_ID}
                                """
                            } else {
                                echo "INFO: Branch push detected. Running standard SonarQube branch analysis."
                                sh "sonar-scanner -Dsonar.login=${SONAR_AUTH_TOKEN}"
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