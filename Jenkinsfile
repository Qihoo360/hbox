pipeline {
    agent any
    stages {
        stage('ShellCheck') {
            steps {
              sh """
                  set -eux
                  which shellcheck
                  shellcheck core/bin/hbox-* core/sbin/start-history-server.sh core/conf/hbox-common-env.sh
                  find tests -name '*.sh' | xargs shellcheck
              """
            }
        }

        stage('Build') {
            steps {
                sh './mvnw -V -B -Dmirror.of.aliyun=central clean verify'
            }
        }
    }
}
