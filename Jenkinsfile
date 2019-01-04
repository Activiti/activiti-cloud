pipeline {
  options {
      disableConcurrentBuilds()
  }
  agent {
    label "jenkins-maven"
  }
  environment {
    ORG = 'activiti'
    APP_NAME = 'activiti-cloud-acceptance-scenarios'
    CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
    //this 3 env for test execution
    GATEWAY_HOST = "activiti-cloud-gateway.jx-staging.35.228.195.195.nip.io"
    SSO_HOST = "activiti-keycloak.jx-staging.35.228.195.195.nip.io"
    REALM = "activiti"
    
    PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
    PREVIEW_NAMESPACE = "scenarios-$BRANCH_NAME-$BUILD_NUMBER".toLowerCase()
    HELM_RELEASE = "$PREVIEW_NAMESPACE".toLowerCase()
  }
  stages {
    stage('CI Build and push snapshot') {
      when {
        branch 'PR-*'
      }
      steps {
        container('maven') {
          sh "mvn clean install -DskipTests"
        }
      }
    }

     stage('Build Release') {
       when {
         branch 'master'
       }
       steps {
         container('maven') {
           // ensure we're not on a detached head
           sh "git checkout master"
           sh "git config --global credential.helper store"
           sh "jx step git credentials"
           sh "mvn clean install -DskipTests"
          }
      }
     }
  }
  post {
        always {
          cleanWs()
        }
  }
}
