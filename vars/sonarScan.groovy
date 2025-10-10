// vars/sonarScan.groovy
def call() {
    script {
        if (env.CHANGE_ID) { // This variable is set by Jenkins for pull requests
            echo "INFO: Pull Request build detected. Running SonarQube PR analysis."
            withSonarQubeEnv('MySonarQube') {
                sh """
                    sonar-scanner \
                    -Dsonar.pullrequest.base=main \
                    -Dsonar.pullrequest.branch=${env.CHANGE_BRANCH} \
                    -Dsonar.pullrequest.key=${env.CHANGE_ID}
                """
            }
        } else {
            echo "INFO: Branch push detected. Running standard SonarQube branch analysis."
            withSonarQubeEnv('MySonarQube') {
                sh "sonar-scanner"
            }
        }
    }
}