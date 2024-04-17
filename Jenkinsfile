pipeline {
    agent any
    stages {
        stage('Build') { 
            steps {
                sh './mvnw -V -B clean verify' 
            }
        }
    }
}
