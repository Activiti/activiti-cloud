pipeline {
    agent {
      kubernetes {
              // Change the name of jenkins-maven label to be able to use yaml configuration snippet
              label "maven-dind"
              // Inherit from Jx Maven pod template
              inheritFrom "maven"
              // Add pod configuration to Jenkins builder pod template
              yamlFile "maven-dind.yaml"
      }

    }
    environment {
      ORG               = 'activiti'
      APP_NAME          = 'runtime-bundle'
      VERSION           = jx_release_version()
      CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
      GITHUB_CHARTS_REPO   = "https://github.com/Activiti/activiti-cloud-helm-charts.git"
      GITHUB_HELM_REPO_URL = "https://activiti.github.io/activiti-cloud-helm-charts/"
    }
    stages {
      stage('CI Build and push snapshot') {
        when {
          branch 'PR-*'
        }
        environment {
          PROJECT_VERSION   = maven_project_version()
          VERSION           = "$PROJECT_VERSION".replaceAll("SNAPSHOT","$BRANCH_NAME-$BUILD_NUMBER")
          PREVIEW_NAMESPACE = "$APP_NAME-$BRANCH_NAME".toLowerCase()
          HELM_RELEASE      = "$PREVIEW_NAMESPACE".toLowerCase()
        }
        steps {
          container('maven') {
            sh "mvn versions:set -DnewVersion=$VERSION"
            sh "mvn install"
            sh 'export VERSION=$VERSION && skaffold build -f skaffold.yaml'

            // sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:$VERSION"

            dir("./charts/$APP_NAME") {
              sh "make build"
            }
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
            // so we can retrieve the version in later steps
            sh "echo $VERSION > VERSION"
            sh "mvn versions:set -DnewVersion=$VERSION"

            dir ("./charts/$APP_NAME") {
                retry(5){  
                  sh "make tag"
                }
            }
            sh 'mvn clean deploy'

            sh 'export VERSION=$VERSION && skaffold build -f skaffold.yaml'

            sh "jx step post build --image $DOCKER_REGISTRY/$ORG/$APP_NAME:$VERSION"
          }
        }
      }
      stage('Promote to Environments') {
        when {
          branch 'master'
        }
        steps {
          container('maven') {
            dir ("./charts/$APP_NAME") {
              // it is failing looks like a bug in JX 
              // sh 'jx step changelog --version v$VERSION'

              // release the helm chart
              retry(5) {  
                sh 'make github'
              }
              sh 'sleep 10'
              retry(5) {  
                sh 'make updatebot/push-version'
              }

            }
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
        return sh( script: "echo \$(jx-release-version)", returnStdout: true).trim().toString()
    }
}

def maven_project_version() {
    container('maven') {
        return sh( script: "echo \$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout -f pom.xml)", returnStdout: true).trim().toString()
    }
}