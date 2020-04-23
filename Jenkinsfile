
properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '1')), disableConcurrentBuilds(), disableResume(), copyArtifactPermission('*'), durabilityHint('PERFORMANCE_OPTIMIZED')])

pipeline {
    agent { node { label 'maven' } }
    stages {
        stage('Build Plugin') { steps {
            sh 'mvn package'
        } }
        stage('Archive') { steps {
            archiveArtifacts artifacts: '**/target/anka-build.hpi', onlyIfSuccessful: true
        } }
    }
}