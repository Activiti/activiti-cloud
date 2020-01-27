pipeline {
    options {
        disableConcurrentBuilds()
        quietPeriod(30)
    }
    agent {
        label "jenkins-maven-java11"
    }
    environment {
        ORG               = 'activiti'
        APP_NAME          = 'activiti-cloud-dependencies'
        CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
        PREVIEW_NAMESPACE = "example-$BRANCH_NAME-$BUILD_NUMBER".toLowerCase().replaceAll("[\\-\\+\\.\\^:,]","");
        GLOBAL_GATEWAY_DOMAIN="35.242.205.159.nip.io"
        REALM              = "activiti"
        GATEWAY_HOST       = "gateway.$PREVIEW_NAMESPACE.$GLOBAL_GATEWAY_DOMAIN"
        SSO_HOST           = "identity.$PREVIEW_NAMESPACE.$GLOBAL_GATEWAY_DOMAIN"
        GITHUB_CHARTS_REPO = "https://github.com/Activiti/activiti-cloud-helm-charts.git"
        RELEASE_BRANCH     = "develop"
        RELEASE_TAG_REGEX  = "*M*"
    }
    stages {
        stage('Configure Git') {
            steps {
                container('maven') {
                    sh "git config --global credential.helper store"
                    sh "jx step git credentials"
                }
            }
        }
        stage('Set Versions') {
            parallel {
                stage('Preview Version') {
                    when {
                        branch 'PR-*'
                    }
                    environment {
                        VERSION = "$PREVIEW_NAMESPACE"
                    }
                    steps {
                        container('maven') {
                            echo "VERSION=$VERSION"
                            // so we can retrieve the version in later steps
                            sh "echo $VERSION > VERSION"
                        }
                    }
                }
                stage('Release Version') {
                    when {
                        branch "$RELEASE_BRANCH"
                    }
                    environment {
                        VERSION = jx_release_version()
                    }
                    steps {
                        container('maven') {
                            echo "VERSION=$VERSION"
                            // so we can retrieve the version in later steps
                            sh "echo $VERSION > VERSION"

                            // ensure we're not on a detached head
                            sh "git checkout $RELEASE_BRANCH"
                            sh "git fetch --tags"

                        }
                    }
                }
                stage('Tag Version') {
                    when {
                        tag "$RELEASE_TAG_REGEX"
                    }
                    environment {
                        VERSION = "$TAG_NAME"
                    }
                    steps {
                        container('maven') {
                            echo "VERSION=$VERSION"
                            sh "git checkout $VERSION"
                            sh "git fetch --all --tags --prune"
                            sh "git checkout tags/$VERSION -b $VERSION"

                            sh "echo $VERSION > VERSION"
                        }
                    }
                }                
            }
        }
        stage('Build Artifacts') {
            environment {
                VERSION = version()
            }
            steps {
                container('maven') {
                    echo "VERSION=$VERSION"
                    sh "mvn versions:set -DnewVersion=$VERSION"
                    sh "mvn clean install"
                    sh "make updatebot/push-version-dry"
                }
            }
        }
        stage('Tag Helm Chart') {
            when {
                tag "$RELEASE_TAG_REGEX"
            }
            steps {
                container('maven') {
                    sh "make replace-release-full-chart-names"
                }
            }
        }
        stage('Deploy Helm Chart') {
            parallel {
                stage('Build & Deploy Helm Chart') {
                    steps {
                        container('maven') {
                            sh "make prepare-helm-chart"
                            sh "make run-helm-chart"
                            sh "sleep 30"
                        }
                    }
                }
                stage("Build Acceptance Scenarios") {
                    steps {
                        container('maven') {
                            sh "make activiti-cloud-acceptance-scenarios"
                        }
                    }
                }
            }
        }
        stage("Run Acceptance Scenarios") {
            parallel {
                stage("Modeling Acceptance Tests") {
                    steps {
                        container('maven') {
                            sh "make modeling-acceptance-tests"
                        }
                    }
                    post {
                        failure {
                            script {
                                def GIT_COMMIT_DETAILS = sh (
                                    script: 'mvn dependency:tree -pl activiti-cloud-dependencies',
                                    returnStdout: true
                                ).trim()

                                println GIT_COMMIT_DETAILS

                                slackSend(channel: "#activiti-community-builds", 
                                    color: "danger",
                                    message: "Modeling Acceptance Tests had failed: $BUILD_URL \n${GIT_COMMIT_DETAILS}" , 
                                    sendAsText: true
                                )
                            }
                        }
                    }
                }
                stage("Runtime Acceptance Scenarios") {
                    steps {
                        container('maven') {
                            sh "make runtime-acceptance-tests"
                        }
                    }
                    post {
                        failure {
                            script {
                                def GIT_COMMIT_DETAILS = sh (
                                    script: 'mvn dependency:tree -pl activiti-cloud-dependencies',
                                    returnStdout: true
                                ).trim()

                                println GIT_COMMIT_DETAILS

                                slackSend(channel: "#activiti-community-builds", 
                                    color: "danger",
                                    message: "Runtime Acceptance Tests had failed: $BUILD_URL \n${GIT_COMMIT_DETAILS}" , 
                                    sendAsText: true
                                )
                            }
                        }
                    }
                }
            }
            post {
                success {
                    slackSend(
                        channel: "#activiti-community-builds",
                        message: "Activiti Cloud Dependendencies Acceptance Tests passed: $BUILD_URL"
                    )
                }
            }
        }
        stage('Tag Release') {
            when {
                branch "$RELEASE_BRANCH"
            }
            environment {
                VERSION = version()
            }
            steps {
                container('maven') {
                    retry(5){
                        sh "git add --all"
                        sh "git commit -m \"Release $VERSION\" --allow-empty"
                        sh "git tag -fa v$VERSION -m \"Release version $VERSION\""
                        sh "git push origin v$VERSION"
                    }
                }
            }
        }
        stage('Publish Release') {
            when {
                branch "$RELEASE_BRANCH"
            }
            environment {
                VERSION = version()
            }
            steps {
                container('maven') {
                    sh 'mvn clean deploy'

                    sh "jx step git credentials"

                    retry(2){
                        sh "make updatebot/push-version"
                    }
                    script {
                        def GIT_COMMIT_DETAILS = sh (
                            script: 'mvn dependency:tree -pl activiti-cloud-dependencies',
                            returnStdout: true
                        ).trim()

                        println GIT_COMMIT_DETAILS

                        slackSend(channel: "#activiti-community-builds", 
                            message: "New BoM release version $VERSION propagated to AAE https://github.com/Alfresco/alfresco-process-parent/pulls \n${GIT_COMMIT_DETAILS}" , 
                            sendAsText: true)
                    }
                }
            }
            post {
                failure {
                    slackSend(
                        channel: "#activiti-community-builds",
                        color: "danger",
                        message: "$RELEASE_BRANCH deploy release has failed: $BUILD_URL"
                    )
                }
            }
        }
        stage('Publish Helm Release') {
            when {
                tag "$RELEASE_TAG_REGEX"
            }
            steps {
                container('maven') {
                    sh "make github"
                    sh "make tag"
                }
            }
            post {
                success {
                    script {
                        slackSend(channel: "#activiti-community-builds", message: "New Helm Chart verison $TAG_NAME released." , sendAsText: true)
                    }                    
                }
            }
        }
    }
    post {
        always {
            delete_deployment()
            cleanWs()
        }
    }
}

def delete_deployment() {
    container('maven') {
        sh "make delete"
        sh "kubectl delete namespace $PREVIEW_NAMESPACE|echo 'try to remove namespace '$PREVIEW_NAMESPACE "
    }
}

def version() {
    container('maven') {
        return sh( script: "echo \$(cat VERSION)", returnStdout: true).trim()
    }
}

def jx_release_version() {
    container('maven') {
        return sh( script: "echo \$(jx-release-version)", returnStdout: true).trim()
    }
}
