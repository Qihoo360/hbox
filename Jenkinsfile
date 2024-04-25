pipeline {
    agent any
    stages {
        stage('Lint') {
            failFast true
            parallel {
                stage('Lint: ShellCheck') {
                    steps {
                        sh '''
                            set -eux
                            shellcheck core/bin/hbox-* core/sbin/start-history-server.sh core/libexec/hbox-common-env.sh
                            find tests -name '*.sh' | xargs shellcheck
                        '''
                    }
                }
                stage('Lint: Maven Pom Format') {
                    steps {
                        sh './mvnw -V -B -Dmirror.of.aliyun=central sortpom:verify -Dsort.verifyFail=STOP'
                    }
                }
                stage('Lint: Check Maven Plugins') {
                    steps {
                        sh './mvnw -V -B -Dmirror.of.aliyun=central artifact:check-buildplan'
                    }
                }
            }
        }
        stage('Build') {
            stages {
                stage('Build: Maven Verify') {
                    steps {
                        sh './mvnw -B -Dmirror.of.aliyun=central clean verify'
                    }
                }
                stage('Build: Reproducible on tags') {
                    when {
                        buildingTag()
                    }
                    steps {
                        sh '''
                            set -eux
                            ./mvnw -B -Dmirror.of.aliyun=central clean install -Dmaven.test.skip=true -DskipTests -Dinvoker.skip -Dbuildinfo.detect.skip=false
                            ./mvnw -B -Dmirror.of.aliyun=central clean
                            mkdir -p target

                            true artifact:compare should not contain warning or error
                            trap 'cat target/build.log' ERR
                            ./mvnw -B -Dmirror.of.aliyun='*' -l target/build.log package artifact:compare -Dmaven.test.skip=true -DskipTests -Dinvoker.skip -Dbuildinfo.detect.skip=false
                            test 0 = "$(sed -n '/^\\[INFO\\] --- maven-artifact-plugin:[^:][^:]*:compare/,/^\\[INFO\\] ---/ p' target/build.log | grep -c '^\\[\\(WARNING\\|ERROR\\)\\]')"

                            true all files should be ok
                            trap 'find . -name "*.buildcompare" -print0 | xargs -0 cat' ERR
                            find . -name '*.buildcompare' -print0 | xargs -0 grep -q '^ko=0$'
                            trap '' ERR

                            find . -name "*.buildcompare" -print0 | xargs -0 cat
                        '''
                    }
                }
            }
        }
        stage('Deploy .tar.gz on tags') {
            when {
                buildingTag()
                tag pattern: "v\\d+\\.\\d+\\.\\d+.*", comparator: "REGEXP"
                environment name: 'GIT_URL', value: 'git@adgit.src.corp.qihoo.net:deep-learning/hbox.git'
            }
            steps {
                sh './mvnw -B -Dmirror.of.aliyun=central deploy -Dmaven.test.skip=true -DskipTests -Dinvoker.skip -Dbuildinfo.detect.skip=false'
            }
        }
    }
}
