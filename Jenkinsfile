pipeline {
    agent {
      label "jenkins-maven"
    }
    environment {
      ORG               = 'activiti'
      APP_NAME          = 'activiti-cloud-acceptance-tests'
      CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
    }
    stages {
      stage('CI Build and push snapshot') {
        when {
          branch 'PR-*'
        }
        environment {
          PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
          PREVIEW_NAMESPACE = "$APP_NAME-$BRANCH_NAME".toLowerCase()
          HELM_RELEASE = "$PREVIEW_NAMESPACE".toLowerCase()
        }
        steps {
          container('maven') {
            sh "mvn versions:set -DnewVersion=$PREVIEW_VERSION"
            sh "mvn install"
          }

        }
      }
      stage('Build Release') {
        when {
          branch 'develop'
        }
        steps {
          container('maven') {
            // ensure we're not on a detached head
            sh "git checkout develop"
            sh "git config --global credential.helper store"

            sh "jx step git credentials"
          }
          container('maven') {
            sh "export GATEWAY_HOST=activiti-cloud-gateway:8080 && export SSO_HOST=youngling-boxer-keyc-http && export REALM=activiti && mvn clean install -DskipTests && mvn -pl '!modeling-acceptance-tests,!apps-acceptance-tests,!multiple-runtime-acceptance-tests,!security-policies-acceptance-tests,!shared-acceptance-tests' clean verify"

          }
        }
      }
    }
    post {
        always {
            cleanWs()
        }
        failure {
            input """Pipeline failed. 
We will keep the build pod around to help you diagnose any failures. 

Select Proceed or Abort to terminate the build pod"""
        }
    }
  }
