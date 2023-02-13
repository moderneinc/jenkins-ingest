def workspaceDir = new File(__FILE__).getParentFile()

folder('cli-ingest') {
    displayName('CLI Ingest Jobs')
}


new File(workspaceDir, 'repos-sample.csv').splitEachLine(',') { tokens ->
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

        steps {
            //requires to enable "Use secret text(s) or file(s)" in the free style JOB and configure $GC_KEY
              scm {
                git {
                    remote {
                        url("https://github.com/${repoName}")
                        branch(repoBranch)
                        credentials('jkschneider-pat')
                    }
                    extensions {
                        localBranch(repoBranch)
                    }
                }
            }
            wrappers {
                credentialsBinding {
                    usernamePassword('ARTIFACTORY_USER', 'ARTIFACTORY_PASSWORD', 'artifactory')
                }
            }
            String workspacePath = SEED_JOB.getWorkspace()
            shell('docker run -v ' + workspacePath + ':/repository'
                    + ' -e JAVA_VERSION='+ repoJavaVersion
                    + ' -e PUBLISH_URL=https://artifactory.moderne.ninja/artifactory/moderne-ingest '
                    + ' -e PUBLISH_USER=$ARTIFACTORY_USER'
                    + ' -e PUBLISH_PWD=$ARTIFACTORY_PASSWORD'
                    + ' moderne/moderne-cli:latest')
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
