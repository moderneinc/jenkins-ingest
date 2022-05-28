def workspaceDir = new File(__FILE__).getParentFile()
new File(workspaceDir, 'repos.csv').splitEachLine(',') { repoConfig ->
    def jobName = repoConfig.repoName.replaceAll('/', '_')

    println("creating job $jobName")
    // TODO figure out how to store rewrite version, look it up on next run, and if rewrite hasn't changed and commit hasn't changed, don't run.
    job("$jobName") {

        label("$repoConfig.label")

        scm {
            git {
                remote {
                    url("https://github.com/${repoConfig.repoName}")
                    branch(repoConfig.branch)
                }
                extensions {
                    localBranch(repoConfig.branch)
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
            if (repoConfig.buildTool == 'gradle' || repoConfig.buildTool == 'gradlew') {
                configFiles {
                    file('moderne-gradle-init') {
                        targetLocation('moderne-init.gradle')
                    }
                }
            }
        }

        steps {
            if (repoConfig.buildTool == 'gradle' || repoConfig.buildTool == 'gradlew') {
                gradle {
                    if (repoConfig.buildTool == 'gradlew') {
                        useWrapper()
                    }
                    // TODO specify style
                    switches('--no-daemon -I moderne-init.gradle')
                    tasks('publishModernePublicationToMavenRepository')
                }
            }
        }

        if (repoConfig.buildTool == 'maven') {
            configure { node ->

                node / 'builders' << 'org.jfrog.hudson.maven3.Maven3Builder' {
                    mavenName 'maven 3'
                    if (repoConfig.style != null) {
                        goals "-B -Drewrite.activeStyles=${repoConfig.style} io.moderne:moderne-maven-plugin:0.10.0:ast install"
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
}
