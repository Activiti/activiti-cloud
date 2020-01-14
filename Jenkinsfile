pipeline {
    agent {
      label "jenkins-maven-java11"
    }
    environment {
      ORG               = 'activiti'
      APP_NAME          = 'activiti-cloud-runtime-bundle-service'
      CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
      RELEASE_BRANCH      = "develop"
    }
    stages {
      stage('CI Build and push snapshot') {
        when {
          branch 'PR-*'
        }
        environment {
          PROJECT_VERSION = maven_project_version()
          PREVIEW_VERSION = "$PROJECT_VERSION".replaceAll("SNAPSHOT","$BRANCH_NAME-$BUILD_NUMBER-SNAPSHOT")
          PREVIEW_NAMESPACE = "$APP_NAME-$BRANCH_NAME".toLowerCase()
          HELM_RELEASE = "$PREVIEW_NAMESPACE".toLowerCase()
        }
        steps {
          container('maven') {
            sh "mvn versions:set -DnewVersion=$PREVIEW_VERSION"
            sh "mvn install"
            sh "mvn deploy -DskipTests"
          }
        }
      }
      stage('Build Release') {
        when {
          branch "$RELEASE_BRANCH"
        }
        environment {
          VERSION = jx_release_version()      
        }
        steps {
          container('maven') {
            // ensure we're not on a detached head
            sh "git checkout $RELEASE_BRANCH"
            sh "git config --global credential.helper store"

            sh "jx step git credentials"
            // so we can retrieve the version in later steps
            sh "echo $VERSION > VERSION"
            sh "mvn versions:set -DnewVersion=$VERSION"
            sh "mvn clean verify"

            retry(5) {
              sh "git add --all"
              sh "git commit -m \"Release $VERSION\" --allow-empty"
              sh "git tag -fa v$VERSION -m \"Release version $VERSION\""
              sh "git push origin v$VERSION"
            }

            sh "mvn clean deploy -DskipTests"

            retry(2) {
              sh "updatebot push-version --kind maven org.activiti.cloud.rb:activiti-cloud-runtime-bundle-dependencies $VERSION"
              sh "rm -rf .updatebot-repos/"
              sh "sleep \$((RANDOM % 10))"
              sh "updatebot push-version --kind maven org.activiti.cloud.rb:activiti-cloud-runtime-bundle-dependencies $VERSION"
            }
            
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

            sh "updatebot push-version --kind maven org.activiti.cloud.rb:activiti-cloud-runtime-bundle-dependencies \$(cat VERSION)"
          }
        }
      }
    }
    post {
        failure {
           slackSend(
             channel: "#activiti-community-builds",
             color: "danger",
             message: "$BUILD_URL"
           )
        } 
        always {
            cleanWs()
        }
    }
}

def jx_release_version() {
    container('maven') {
        return sh( script: "echo \$(jx-release-version)", returnStdout: true).trim()
    }
}

def maven_project_version() {
    container('maven') {
        return sh( script: "echo \$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout -f pom.xml)", returnStdout: true).trim()
    }
}  