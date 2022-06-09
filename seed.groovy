def workspaceDir = new File(__FILE__).getParentFile()

folder('ingest') {
    displayName('Ingest Jobs')
}

configFiles {
    groovyScript {
        id("gradle-inject")
        name("init.gradle")
        comment("A Gradle init script used to inject universal plugins into a gradle build.")
        content readFileFromWorkspace('init.gradle')
    }
}
new File(workspaceDir, 'repos.csv').splitEachLine(',') { tokens ->
    if (tokens[0].startsWith('repoName')) {
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
    job("ingest/$repoJobName") {

        if (!['java8', 'java11'].contains(repoLabel)) {
            disabled()
        }

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
            cron('H 8 * * *')
        }

        wrappers {
            credentialsBinding {
                usernamePassword('ARTIFACTORY_USER', 'ARTIFACTORY_PASSWORD', 'artifactory')
            }
            timeout {
                absolute(60)
                abortBuild()
            }
            if (['gradle', 'gradlew'].contains(repoBuildTool)) {
                configFiles {
                    file('moderne-gradle-init') {
                        targetLocation('moderne-init.gradle')
                    }
                }
            }
        }

        steps {
            if (['gradle', 'gradlew'].contains(repoBuildTool)) {
                gradle {
                    if (repoBuildTool == 'gradle') {
                        useWrapper(false)
                        gradleName('gradle 7.4.2')
                    } else {
                        useWrapper(true)
                        makeExecutable(true)
                    }
                    if (repoStyle != null) {
                        switches("--no-daemon -DactiveStyle=${repoStyle} -I moderne-init.gradle")
                    } else {
                        switches('--no-daemon -I moderne-init.gradle')
                    }
                    tasks('publishModernePublicationToMavenRepository')
                }
            }
        }

        if (['maven', 'mvnw'].contains(repoBuildTool)) {
            configure { node ->

                node / 'builders' << 'org.jfrog.hudson.maven3.Maven3Builder' {
                    mavenName 'maven 3'
                    useWrapper(repoBuildTool == 'mvnw')
                    if (repoStyle != null) {
                        goals "-B -Drewrite.activeStyles=${repoStyle} io.moderne:moderne-maven-plugin:0.11.0:ast install"
                    } else {
                        goals '-B io.moderne:moderne-maven-plugin:0.11.0:ast install'
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
            cleanWs()
        }
    }
    return
}
