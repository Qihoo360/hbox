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
                        sh './mvnw -V -B sortpom:verify -Dsort.verifyFail=STOP'
                    }
                }
                stage('Lint: Check Maven Plugins') {
                    steps {
                        sh './mvnw -V -B artifact:check-buildplan'
                    }
                }
            }
        }
        stage('Build') {
            stages {
                stage('Build: Maven Verify') {
                    steps {
                        sh './mvnw -B clean verify'
                    }
                }
                stage('Build: Reproducible on tags') {
                    when {
                        buildingTag()
                    }
                    steps {
                        sh '''
                            set -eux
                            ./mvnw -B clean install -Dmaven.test.skip=true -DskipTests -Dinvoker.skip -Dbuildinfo.detect.skip=false
                            ./mvnw -B clean
                            mkdir -p target

                            true artifact:compare should not contain warning or error
                            trap 'cat target/build.log' ERR
                            ./mvnw -B -l target/build.log package artifact:compare -Dmaven.test.skip=true -DskipTests -Dinvoker.skip -Dbuildinfo.detect.skip=false
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
    }
}
