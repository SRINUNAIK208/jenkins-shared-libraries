def call(Map configMap){
   pipeline {
    agent {
        label "AGENT-1"
    }
    environment {
        appVersion = ''
        REGION = 'us-east-1'
        ACCOUNT_ID = '824333137275'
        PROJECT = 'roboshop'
        COMPONENT = 'catalogue'
    }
    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }
    parameters{
        booleanParam(name: 'deploy', defaultValue: false, description: 'Toggle this value')
    }

    stages{
          stage ('UNIT TESTING') {
            steps {
                script {
                    sh """
                       echo "unit testing"
                    """
                }
            }
        }
      

       
   
        stage('Build') {
            steps{
                script {
                  sh """
                     echo "hello"
                  """
                }
            }
        }
     }
   }
}

