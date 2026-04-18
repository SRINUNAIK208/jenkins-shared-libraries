def call(Map configMap){
    pipeline {
    agent {
        label 'AGENT-1'
    }
    environment{
        appVersion = ''
        REGION = 'us-east-1'
        PROJECT = 'roboshop'
        COMPONENT = 'catalogue'
        ACCOUNT_ID = '824333137275'
    }
    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        // ansiColor('xterm')
    }
    parameters{
        string(name: 'appVersion', description: 'Image version of the application')
        choice(name: 'deploy_to', choices: ['dev', 'qa','prod'], description: 'pick the environment')

    }
    stages{
        stage('Deploy'){
            steps{
                script{
                    withAWS(credentials: 'aws-auth', region: 'us-east-1'){
                     sh """
                       aws eks update-kubeconfig --region $REGION --name "$PROJECT"
                       kubectl get nodes
                       sed -i "s/IMAGE_VERSION/${params.appVersion}/g" values-${params.deploy_to}.yaml
                       helm upgrade --install $COMPONENT -n roboshop -f values-${params.deploy_to}.yaml .
                     """
                    }

                }
              
            }

        
        }
        stage('check the status'){
            steps{
                script{
                    withAWS(credentials: 'aws-auth', region: 'us-east-1'){
                      def deploymentStatus = sh(returnStdout: true, script: "kubectl rollout status deployment/$COMPONENT -n roboshop --timeout=30s || echo FAILED").trim();
                      if(deploymentStatus.contains('succssfully rollout'))
                      {
                         echo "deployment is success"
                      }
                      else
                      {
                        sh """
                          helm rollback $COMPONENT -n roboshop
                          sleep 20
                        """
                         def rollbackStatus  = sh(returnStdout: true, script: "kubectl rollout status deployment/$COMPONENT -n roboshop --timeout=30s || echo FAILED").trim();
                         if(rollbackStatus .contains('successfully rolled out'))
                         {
                            error "rollback is success deployment is failure"
                          }
                          else{
                            error "rollback is failure and deploymenet also failure"
                          }
                      }
                    }
                }
            }
        }
       
    }
}
}