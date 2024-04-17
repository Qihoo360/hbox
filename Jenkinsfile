pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './mvnw -V -B -Dmirror.of.aliyun=central clean verify'
            }
        }
    }
}
