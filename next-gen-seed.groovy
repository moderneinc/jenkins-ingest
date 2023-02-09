node {
    cleanWs()

    git url:'https://github.com/Netflix/eureka', branch:'main', credentialsId:'jkschneider-pat'

    environment {
        ARTIFACTORY_CREDS = credentials('artifactory')
    }

    withCredentials([usernamePassword(credentialsId: 'artifactory', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {

        sh 'docker run -v '+ pwd() +':/repository -e JAVA_VERSION=1.8 -e PUBLISH_URL=https://artifactory.moderne.ninja/artifactory/moderne-ingest -e PUBLISH_USER='+ USERNAME + ' -e PUBLISH_PWD=' + PASSWORD +' moderne/moderne-cli:latest'
    }

}
