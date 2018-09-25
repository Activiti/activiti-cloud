pipeline {
    agent {
      label "jenkins-maven"
    }
    environment {
      ORG               = 'activiti'
      APP_NAME          = 'activiti-cloud-api'
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
            sh 'export VERSION=$PREVIEW_VERSION' // && skaffold build -f skaffold.yaml'


          //  sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:$PREVIEW_VERSION"
          }

          //dir ('./charts/preview') {
          // container('maven') {
          //   sh "make preview"
          //   sh "jx preview --app $APP_NAME --dir ../.."
          // }
          //}
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
            // so we can retrieve the version in later steps
            sh "echo \$(jx-release-version) > VERSION"
            sh "mvn versions:set -DnewVersion=\$(cat VERSION)"
          }
          //dir ('./charts/activiti-cloud-api') {
          //  container('maven') {
          //    sh "make tag"
          //  }
          //}
          container('maven') {
            sh 'mvn clean deploy'

            sh 'export VERSION=`cat VERSION`' // && skaffold build -f skaffold.yaml'
            sh "updatebot push --ref develop"
            sh "updatebot update"
            sh "updatebot update-loop"

            //sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:\$(cat VERSION)"
          }
        }
      }
      stage('Build Release from Tag') {
        when {
          tag '*RELEASE'
        }
        steps {
          container('maven') {
            // ensure we're not on a detached head
            sh "git checkout $TAG_NAME"
            sh "git config --global credential.helper store"

            sh "jx step git credentials"
            // so we can retrieve the version in later steps
            sh "echo \$TAG_NAME > VERSION"
            sh "mvn versions:set -DnewVersion=\$(cat VERSION)"
          }
          container('maven') {
            sh '''
              mvn clean deploy -P !alfresco -P central
              '''

            sh 'export VERSION=`cat VERSION`'// && skaffold build -f skaffold.yaml'

            sh "git config --global credential.helper store"

            sh "jx step git credentials"
            //sh "updatebot push"
            //sh "updatebot update"

            sh "echo pushing with update using version \$(cat VERSION)"

            sh "updatebot push-version --kind maven org.activiti.cloud.api:activiti-cloud-api \$(cat VERSION)"
            sh "updatebot push-version --kind maven org.activiti.cloud.api:activiti-cloud-api-model-shared \$(cat VERSION)"
            sh "updatebot push-version --kind maven org.activiti.cloud.api:activiti-api-task-model \$(cat VERSION)"
            sh "updatebot push-version --kind maven org.activiti.cloud.api:activiti-cloud-api-process-model \$(cat VERSION)"
            sh "updatebot push-version --kind maven org.activiti.cloud.api:activiti-cloud-api-model-shared-impl \$(cat VERSION)"
            sh "updatebot push-version --kind maven org.activiti.cloud.api:activiti-cloud-api-task-model-impl \$(cat VERSION)"
            sh "updatebot push-version --kind maven org.activiti.cloud.api:activiti-cloud-api-model-shared-impl \$(cat VERSION)"
            sh "updatebot push-version --kind maven org.activiti.cloud.api:activiti-cloud-api-dependencies \$(cat VERSION)"
            sh "updatebot update-loop"

        //    sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:\$(cat VERSION)"
          }
        }
      }
      // stage('Promote to Environments') {
      //   when {
      //     branch 'develop'
      //   }
      //   steps {
      //     dir ('./charts/activiti-cloud-api') {
      //       container('maven') {
      //         sh 'jx step changelog --version v\$(cat ../../VERSION)'

      //         // release the helm chart
      //         sh 'jx step helm release'

      //         // promote through all 'Auto' promotion Environments
      //         sh 'jx promote -b --all-auto --timeout 1h --version \$(cat ../../VERSION)'
      //       }
      //     }
      //   }
      // }
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
