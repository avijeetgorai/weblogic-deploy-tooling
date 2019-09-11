pipeline {
    agent {
        docker {
            alwaysPull true
            reuseNode true
            image 'phx.ocir.io/weblogick8s/oracle/fmw-infrastructure:12.2.1.3'
        }
    }

    stages {
        stage ('Environment') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "JAVA_HOME = ${JAVA_HOME}"
                    echo "M2_HOME = ${M2_HOME}"
                    mvn --version
                '''
            }
        }
        stage ('Build') {
            steps {
                sh '''
                    mvn -B -DskipTests -Dunit-test-wlst-dir=${WLST_DIR} clean package
                '''
            }
        }
        stage ('Verify') {
            steps {
                sh 'mvn -Dunit-test-wlst-dir=${WLST_DIR} -Dmw_home=${ORACLE_HOME} verify'
            }
            post {
                always {
                    junit 'core/target/surefire-reports/*.xml'
                }
            }
        }
    }
}
