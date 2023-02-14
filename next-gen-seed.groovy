node {
    cleanWs()

    git url:'https://github.com/Netflix/eureka', branch:'main', credentialsId:'jkschneider-pat'

    environment {
        ARTIFACTORY_CREDS = credentials('artifactory')
    }

    withCredentials([usernamePassword(credentialsId: 'artifactory', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {

        sh 'docker run -v '+ pwd() +':/repository -e JAVA_VERSION=8 -e MODERNE_PUBLISH_URL=https://artifactory.moderne.ninja/artifactory/moderne-ingest -e MODERNE_PUBLISH_USER='+ USERNAME + ' -e MODERNE_PUBLISH_PWD=' + PASSWORD +' moderne/moderne-cli:latest'
    }

}
