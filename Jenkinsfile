pipeline {
    agent {
      label "jenkins-maven-java11"
    }
    environment {
      ORG               = 'activiti'
      APP_NAME          = 'activiti-cloud-dependencies'
      CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
      PREVIEW_NAMESPACE = "example-$BRANCH_NAME-$BUILD_NUMBER".toLowerCase()
      GLOBAL_GATEWAY_DOMAIN="35.242.205.159.nip.io"
      REALM = "activiti"
      GATEWAY_HOST = "gateway.$PREVIEW_NAMESPACE.$GLOBAL_GATEWAY_DOMAIN"
      SSO_HOST = "identity.$PREVIEW_NAMESPACE.$GLOBAL_GATEWAY_DOMAIN"
      GITHUB_CHARTS_REPO = "https://github.com/Activiti/activiti-cloud-helm-charts.git"
      //GITLAB_TOKEN = credentials('GITLAB_TOKEN')


    }
    stages {
      stage('helm chart release') {
              when {
                branch '*M*'
              }
              environment {
                //TAG_NAME = sh(returnStdout: true, script: 'git describe --always').trim()
                //HELM_ACTIVITI_VERSION = "$TAG_NAME"
                //APP_ACTIVITI_VERSION = "$TAG_NAME"
                //HELM_ACTIVITI_VERSION = "$BRANCH_NAME" //with dash
                APP_ACTIVITI_VERSION = "$BRANCH_NAME"
              }
              steps {
               // script {
               //     env.HELM_ACTIVITI_VERSION = sh(returnStdout: true, script: "echo '${BRANCH_NAME}'|rev|sed 's/\\./-/'|rev").trim()
               // }

                container('maven') {
                    sh "echo $APP_ACTIVITI_VERSION >VERSION"
                    sh "git checkout $APP_ACTIVITI_VERSION"
                    sh "git config --global credential.helper store"
                    sh "jx step git credentials"
                    sh "make retag-docker-images"
                    sh "make push-docker-images"
                    sh "make updatebot/push-version-dry"
                    sh "make prepare-release-full-chart"
                    sh "make run-helm-chart"
                    sh "make acc-tests"
                    sh "make release-full-chart"
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

