def call(Map configMap){
   pipeline {
    agent {
        label "AGENT-1"
    }
    environment {
        appVersion = ''
        REGION = 'us-east-1'
        ACCOUNT_ID = '824333137275'
        PROJECT = configMap.get('project')
        COMPONENT = configMap.get('component')
    }
    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }
    parameters{
        booleanParam(name: 'deploy', defaultValue: false, description: 'Toggle this value')
    }

    stages {
        stage ('Read package.json') {
            steps {
                script {
                   def packageJson = readJSON file: 'package.json'
                   appVersion = packageJson.version
                   echo "package version: ${appVersion}"
                }

            }
        }
        stage ('install dependencies') {
            steps {
                script {
                    sh """
                       npm install
                       echo "dependencies installed"
                    """
                }
            }
        }
          stage ('UNIT TESTING') {
            steps {
                script {
                    sh """
                       echo "unit testing"
                    """
                }
            }
        }
        // stage('sonar scan'){
        //     environment {
        //         scannerHome = tool 'sonar-8.0'
        //     }
        //     steps{
        //         script{
        //             withSonarQubeEnv(installationName: 'sonar-8.0'){
        //                 sh "${scannerHome}/bin/sonar-scaner"
        //             }
        //         }
        //     }
        // }
         stage ('Docker build') {
            steps {
                script {
                    withAWS(credentials: 'aws-auth', region: 'us-east-1') {
                        sh """
                          export DOCKER_BUILDKIT=0
                          aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com
                          docker build -t ${ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                          docker push ${ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                        """
                    }
                }
            }

        }
        // stage('check ecr scan'){
        //     steps{
        //         script{
        //             withAWS(credentials: 'aws-auth', region: 'us-east-1') {
        //                 sh """
        //                   sleep 30
        //                   aws ecr describe-image-scan-findings \
        //                    --repository-name ${PROJECT}/${COMPONENT} \
        //                    --image-id imageTag=${appVersion} \
        //                    --region us-east-1 \
        //                    --query 'imageScanFindings.findingSeverityCounts' \
        //                    --output json > scan.json
        //                 """
        //             }
        //         }   
        //     }
        // }
        // stage('Quality gate'){
        //     steps{
        //         script{
        //             def scan = readJSON file: 'scan.json'
        //             def high = scan.HIGH ?: 0
        //             def critical = scan.CRITICAL ?: 0

        //     if (high> 0 || critical > 0) {
        //         error "❌ Vulnerabilities found: HIGH=${high}, CRITICAL=${critical}"
        //     } else {
        //         echo "✅ Image is safe"
        //     }
        //         }
        //     }
        // }

        stage("Trigger deploy"){
            when{
                expression { params.deploy }
            }
            steps{
                script{
                   build job: 'catalogue-cd',
                   parameters: [
                     string(name: 'appVersion', value: "${appVersion}"),
                     string(name:'deploy_to', value: 'dev')
                   ],
                   propagate: false,
                   wait: false
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