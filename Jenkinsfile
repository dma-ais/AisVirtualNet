pipeline {
    agent any

    tools {
        maven 'M3.3.9'
    }

    triggers {
        pollSCM('0 * * * *')
    }

    stages {
        stage('checkout') {
            steps {
                checkout scm
            }
        }

        stage('build') {
            steps {
                withMaven() {
                    sh 'mvn -e -U -DskipTests -DincludeSrcJavadocs clean source:jar install checkstyle:check'
                }
            }
        }
    }

//    post {
//        failure {
//            // notify users when the Pipeline fails
//            mail to: 'steen@lundogbendsen.dk',
//                    subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
//                    body: "Something is wrong with ${env.BUILD_URL}"
//        }
//    }
}

