def workspaceDir = new File(__FILE__).getParentFile()

new File(workspaceDir, 'repos.csv').splitEachLine(',') { repoConfig ->
    if (tokens.get(0).startsWith('repoName')) {
        return
    }
    // repoName, branch, label, style, buildTool
    def repoName = tokens.get(0)
    def branch = tokens.get(1)
    def label = tokens.get(2)
    def style = tokens.get(3)
    def buildTool = tokens.get(4)
    def jobName = repoName.replaceAll('/', '_')

    println("creating job $jobName")
    // TODO figure out how to store rewrite version, look it up on next run, and if rewrite hasn't changed and commit hasn't changed, don't run.
    job("$jobName") {

        label("$label")

        scm {
            git {
                remote {
                    url("https://github.com/${repoName}")
                    branch(branch)
                }
                extensions {
                    localBranch(branch)
                }
            }
        }

        triggers {
            cron('H 8 * * *')
        }

        wrappers {
            credentialsBinding {
                usernamePassword('ARTIFACTORY_USER', 'ARTIFACTORY_PASSWORD', 'artifactory')
            }
            if (buildTool == 'gradle' || buildTool == 'gradlew') {
                configFiles {
                    file('moderne-gradle-init') {
                        targetLocation('moderne-init.gradle')
                    }
                }
            }
        }

        steps {
            if (buildTool == 'gradle' || buildTool == 'gradlew') {
                gradle {
                    if (buildTool == 'gradlew') {
                        useWrapper()
                    }
                    // TODO specify style
                    switches('--no-daemon -I moderne-init.gradle')
                    tasks('publishModernePublicationToMavenRepository')
                }
            }
        }

        if (buildTool == 'maven') {
            configure { node ->

                node / 'builders' << 'org.jfrog.hudson.maven3.Maven3Builder' {
                    mavenName 'maven 3'
                    if (style != null) {
                        goals "-B -Drewrite.activeStyles=${style} io.moderne:moderne-maven-plugin:0.10.0:ast install"
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
    }
    return
}
