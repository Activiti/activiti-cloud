pipeline {
  agent {
    label "jenkins-maven"
  }
  environment {
    ORG = 'almerico'
    APP_NAME = 'activiti-cloud-acceptance-scenarios'
    CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
    GATEWAY_HOST = "activiti-cloud-gateway.jx-staging.35.228.195.195.nip.io"
    SSO_HOST = "activiti-keycloak.jx-staging.35.228.195.195.nip.io"
    REALM = "activiti"
    
    PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
    PREVIEW_NAMESPACE = "$APP_NAME-$BRANCH_NAME".toLowerCase()
    HELM_RELEASE = "$PREVIEW_NAMESPACE".toLowerCase()
  }
  stages {
    stage('CI Build and push snapshot') {
      when {
        branch 'PR-*'
      }

      steps {
        container('maven') {
          // sh "mvn versions:set -DnewVersion=$PREVIEW_VERSION"

          sh "echo $PREVIEW_VERSION"
          dir('charts/activiti-cloud-acceptance-scenarios') {
          //install helm chart for full example
            sh "make install" 
          }
          sh 'sleep 120'
          sh "mvn clean install -DskipTests && mvn -pl '!apps-acceptance-tests,!multiple-runtime-acceptance-tests,!security-policies-acceptance-tests' clean verify"
          //sh "mvn clean install -DskipTests"
        }
      }
    }

     stage('Build Release') {
       when {
         branch 'master'
       }
       steps {
         container('maven') {
          dir('charts/activiti-cloud-acceptance-scenarios') {
          //install helm chart for full example
            sh "make install" 
          }
            sh 'sleep 120'
           // ensure we're not on a detached head
           sh "git checkout master"
           sh "git config --global credential.helper store"
           sh "jx step git credentials"
           //sh "mvn clean install -DskipTests"
           sh "mvn clean install -DskipTests && mvn -pl '!apps-acceptance-tests,!multiple-runtime-acceptance-tests,!security-policies-acceptance-tests' clean verify"
          }
      }
     }
  }
  post {
        always {
          container('maven') {
            dir('charts/activiti-cloud-acceptance-scenarios') {
               sh "make delete" 
            }
            sh "kubectl delete namespace $PREVIEW_NAMESPACE" 
          }
          cleanWs()
        }
  }
}
