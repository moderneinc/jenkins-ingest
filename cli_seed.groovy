def workspaceDir = new File(__FILE__).getParentFile()

def mavenIngestSettingsXmlFileId = "maven-ingest-settings-credentials"
def mavenIngestSettingsXmlRepoFile = ".mvn/ingest-settings.xml"

def publishURL = 'https://artifactory.moderne.ninja/artifactory/moderne-ingest'
def publishCreds = 'artifactory'
def scmCredentialsId = 'jkschneider-pat'
def scheduling='H 4 * * *'

folder('cli-ingest') {
    displayName('CLI Ingest Jobs')
}

configFiles {
    mavenSettingsConfig {
        id(mavenIngestSettingsXmlFileId)
        name("Maven Settings: ingest-maven-settings.xml")
        comment("Maven settings that sets mirror on repos that are known to use http, and injects artifactory credentials")
        content readFileFromWorkspace('maven/ingest-settings.xml')
        isReplaceAll(true)
        serverCredentialMappings {
            serverCredentialMapping {
                serverId('moderne-public')
                credentialsId('artifactory')
            }
            serverCredentialMapping {
                serverId('moderne-remote-cache')
                credentialsId('artifactory')
            }
        }
    }
}

new File(workspaceDir, 'repos.csv').splitEachLine(',') { tokens ->
    if (tokens[0].startsWith('repoName')) {
        return
    }
    def scmHost = 'github.com'
    def repoName = tokens[0]
    def repoBranch = tokens[1]
    def repoJavaVersion = tokens[2]
    def repoStyle = tokens[3] ?: ''
    def repoBuildAction = tokens[5] ?: ''
    def repoSkip = tokens[6]

    if ('true' == repoSkip) {
        return
    }

    def repoJobName = repoName.replaceAll('/', '_')

    boolean requiresJava = repoJavaVersion != null && !repoJavaVersion.equals("")

    println("creating job $repoJobName")

    job("cli-ingest/$repoJobName") {
        
        def extraArgs = ''
        if (repoStyle != null && !repoStyle.equals("")) {
            extraArgs = '--activeStyle ' + repoStyle
        }
        if (repoBuildAction != null && !repoBuildAction.equals("")) {
            extraArgs = extraArgs + ' --buildAction ' + repoBuildAction
        }
        if (requiresJava) {
            extraArgs = extraArgs + ' --mvnSettingsXml ' + mavenIngestSettingsXmlRepoFile
        }

        steps {
            scm {
                git {
                    remote {
                        url("https://${scmHost}/${repoName}")
                        branch(repoBranch)
                        credentials(scmCredentialsId)
                    }
                    extensions {
                        localBranch(repoBranch)
                    }
                }
            }
        
            wrappers {
                credentialsBinding {
                    usernamePassword('MODERNE_PUBLISH_USER', 'MODERNE_PUBLISH_PWD', publishCreds)
                }
                configFiles {
                    file(mavenIngestSettingsXmlFileId) {
                        targetLocation(mavenIngestSettingsXmlRepoFile)
                    }
                }
            }
            if (requiresJava) {
                shell("jenv local ${repoJavaVersion}")
            }
            
            shell('mod publish --path . --url ' + publishURL + ' ' + extraArgs)
        }

        logRotator {
            daysToKeep(7)
        }

        triggers {
            cron(scheduling)
        }

        publishers {
            cleanWs()
        }
    }
    return
}
