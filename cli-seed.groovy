

folder('cli-ingest') {
    displayName('Ingest Jobs')
}


new File('repos-sample.csv').splitEachLine(',') { tokens ->
    if (tokens[0].startsWith('repoName')) {
        return
    }
    def repoName = tokens[0]
    def repoBranch = tokens[1]
    def repoJavaVersion = tokens[2]
    def repoSkip = tokens[6]

    if ("true" == repoSkip) {
        return
    }

    def repoJobName = repoName.replaceAll('/', '_')

    println("creating job $repoJobName")

    job("cli-ingest/$repoJobName") {

        withCredentials([file(credentialsId: 'moderne-gcloud-key', variable: 'GC_KEY')]){
            sh '''
                chmod 600 $GC_KEY
                cat $GC_KEY | docker login -u _json_key --password-stdin https://us.gcr.io

                docker pull us.gcr.io/moderne-dev/moderne/moderne-ingestor:latest
            '''
        }
        git url:"https://github.com/${repoName}", branch:repoBranch, credentialsId:'jkschneider-pat'

        environment {
            ARTIFACTORY_CREDS = credentials('artifactory')
        }

        withCredentials([usernamePassword(credentialsId: 'artifactory', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {

            sh 'docker run -v ' + pwd() + ':/repository -e JAVA_VERSION=1.'+ repoJavaVersion +' -e PUBLISH_URL=https://artifactory.moderne.ninja/artifactory/moderne-ingest -e PUBLISH_USER=' + USERNAME + ' -e PUBLISH_PWD=' + PASSWORD + ' us.gcr.io/moderne-dev/moderne/moderne-ingestor:latest'

        }

        logRotator {
            daysToKeep(7)
        }


        triggers {
            cron('H 4 * * *')
        }

        publishers {
            cleanWs()
        }
    }
    return
}
