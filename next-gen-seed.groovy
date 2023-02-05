node {

    withCredentials([file(credentialsId: 'moderne-gcloud-key', variable: 'GC_KEY')]){
        sh '''
                    chmod 600 $GC_KEY
                    cat $GC_KEY | docker login -u _json_key --password-stdin https://us.gcr.io

                    docker pull us.gcr.io/moderne-dev/moderne/moderne-ingestor:latest
                '''
    }

    git url:'https://github.com/Netflix/eureka', branch:'master', credentialsId:'jkschneider-pat'

    environment {
        ARTIFACTORY_CREDS = credentials('artifactory')
    }

    withCredentials([usernamePassword(credentialsId: 'artifactory', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {

        sh 'docker run -v ' + pwd() + ':/repository -e JAVA_VERSION=1.8 -e PUBLISH_URL=https://artifactory.moderne.ninja/artifactory/moderne-private -e PUBLISH_USER=' + USERNAME + ' -e PUBLISH_PWD=' + PASSWORD + ' us.gcr.io/moderne-dev/moderne/moderne-ingestor:latest'

    }
}