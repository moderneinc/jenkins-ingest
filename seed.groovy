def workspaceDir = new File(__FILE__).getParentFile()

new File(workspaceDir, 'repos.csv').splitEachLine(',') { tokens ->
    if (tokens.get(0).startsWith('repoName')) {
        return
    }
    def repoName = tokens[0]
    def repoBranch = tokens[1]
    def repoLabel = tokens[2]
    def repoStyle = tokens[3]
    def repoBuildTool = tokens[4]
    def repoJobName = repoName.replaceAll('/', '_')

    println("creating job $repoJobName")
    // TODO figure out how to store rewrite version, look it up on next run, and if rewrite hasn't changed and commit hasn't changed, don't run.
    job("$repoJobName") {

        label("$repoLabel")

        scm {
            git {
                remote {
                    url("https://github.com/${repoName}")
                    branch(repoBranch)
                }
                extensions {
                    localBranch(repoBranch)
                }
            }
        }

        triggers {
            cron('H 4 05 * *')
        }

        wrappers {
            credentialsBinding {
                usernamePassword('ARTIFACTORY_USER', 'ARTIFACTORY_PASSWORD', 'artifactory')
            }
            timeout {
                absolute(60)
                abortBuild
            }
            if (repoBuildTool == 'gradle' || repoBuildTool == 'gradlew') {
                configFiles {
                    file('moderne-gradle-init') {
                        targetLocation('moderne-init.gradle')
                    }
                }
            }
        }

        steps {
            if (repoBuildTool == 'gradle' || repoBuildTool == 'gradlew') {
                gradle {
                    useWrapper(repoBuildTool == 'gradlew')
                    if (repoBuildTool == 'gradle') {
                        gradleName('gradle 7.4.2')
                    }
                    // TODO specify style
                    switches('--no-daemon -I moderne-init.gradle')
                    tasks('publishModernePublicationToMavenRepository')
                }
            }
        }

        if (repoBuildTool == 'maven') {
            configure { node ->

                node / 'builders' << 'org.jfrog.hudson.maven3.Maven3Builder' {
                    mavenName 'maven 3'
                    if (repoStyle != null) {
                        goals "-B -Drewrite.activeStyles=${repoStyle} io.moderne:moderne-maven-plugin:0.10.0:ast install"
                    } else {
                        goals '-B io.moderne:moderne-maven-plugin:0.10.0:ast install'
                    }
                }

                node / 'buildWrappers' << 'org.jfrog.hudson.maven3.ArtifactoryMaven3Configurator' {
                    deployArtifacts true
                    deployerDetails {
                        artifactoryName 'moderne-artifactory'
                        deployReleaseRepository {
                            keyFromText 'moderne-public-ast'
                        }
                        deploySnapshotRepository {
                            keyFromText 'moderne-public-ast'
                        }
                    }
                }
            }
        }

        publishers {
            cleanWs
        }
    }
    return
}
