pipeline {
    agent {
        kubernetes {
            // Change the name of jenkins-maven label to be able to use yaml configuration snippet
            label "maven-dind-java11"
            // Inherit from Jx Maven pod template
            inheritFrom "maven-java11"
            // Add pod configuration to Jenkins builder pod template
            yamlFile "maven-dind.yaml"
        }
    }
    environment {
      ORG                 = 'activiti'
      APP_NAME            = 'activiti-cloud-messages-service'
      CHARTMUSEUM_CREDS   = credentials('jenkins-x-chartmuseum')
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
		    sh "echo PREVIEW_VERSION=$PREVIEW_VERSION"
            sh "mvn versions:set -DnewVersion=$PREVIEW_VERSION"
            sh "mvn install -DskipITs=false"
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
		    sh "echo VERSION=$VERSION"
		                     
            // ensure we're not on a detached head
            sh "git checkout $RELEASE_BRANCH"
            sh "git config --global credential.helper store"

            sh "jx step git credentials"
            // so we can retrieve the version in later steps
            sh "echo $VERSION > VERSION"
            sh "mvn versions:set -DnewVersion=$VERSION"
            sh "mvn clean verify -DskipITs=false"

            retry(5){
              sh "git add --all"
              sh "git commit -m \"Release $VERSION\" --allow-empty"
              sh "git tag -fa v$VERSION -m \"Release version $VERSION\""
              sh "git push origin v$VERSION"
            }
          }
          container('maven') {
            sh "mvn clean deploy -DskipTests"

            sh "jx step git credentials"
            retry(2){
              sh "updatebot push-version --kind maven org.activiti.cloud.messages:activiti-cloud-messages-dependencies $VERSION"
              sh "rm -rf .updatebot-repos/"
              sh "sleep \$((RANDOM % 10))"
              sh "updatebot push-version --kind maven org.activiti.cloud.messages:activiti-cloud-messages-dependencies $VERSION"
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
             message: "activiti-cloud-messages-service branch=$BRANCH_NAME is failed http://jenkins.jx.35.242.205.159.nip.io/job/Activiti/job/activiti-cloud-messages-service/"
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