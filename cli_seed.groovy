def workspaceDir = new File(__FILE__).getParentFile()

def mavenIngestSettingsXmlFileId = "maven-ingest-settings-credentials"
def mavenIngestSettingsXmlRepoFile = ".mvn/ingest-settings.xml"

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
    def repoStyle = tokens[3]
    def repoBuildAction = tokens[5]
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
                    usernamePassword('MODERNE_PUBLISH_USER', 'MODERNE_PUBLISH_PWD', 'artifactory')
                }
                configFiles {
                    file(mavenIngestSettingsXmlFileId) {
                        targetLocation(mavenIngestSettingsXmlRepoFile)
                    }
                }
            }
            shell('docker run -v ${WORKSPACE}:/repository'
                    + ' -e JAVA_VERSION='+ repoJavaVersion
                    + ' -e MODERNE_PUBLISH_URL=https://artifactory.moderne.ninja/artifactory/moderne-ingest'
                    + ' -e MODERNE_PUBLISH_USER=${MODERNE_PUBLISH_USER}'
                    + ' -e MODERNE_PUBLISH_PWD=${MODERNE_PUBLISH_PWD}'
                    + ' -e MODERNE_ACTIVE_STYLE=' + repoStyle
                    + ' -e MODERNE_BUILD_ACTION=' + repoBuildAction
                    + ' -e MODERNE_MVN_SETTINGS_XML=' + mavenIngestSettingsXmlRepoFile
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
