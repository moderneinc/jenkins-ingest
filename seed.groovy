def workspaceDir = new File(__FILE__).getParentFile()

folder('ingest') {
    displayName('Ingest Jobs')
}

configFiles {
    groovyScript {
        id("moderne-gradle-init")
        name("init.gradle")
        comment("A Gradle init script used to inject universal plugins into a gradle build.")
        content readFileFromWorkspace('init.gradle')
    }
    customConfig {
        id("add-gradle-enterprise-extension")
        name("Gradle Enterprise Maven Extension")
        comment("Creates the `.mvn` directory if it is missing, creates the `extensions.xml` file if it is missing, and adds the gradle enterprise xml.")
        content readFileFromWorkspace('add-gradle-enterprise-extension.sh')
    }
    xmlConfig {
        id("gradle-enterprise.xml")
        name("Gradle Enterprise Maven Configuration")
        comment("A gradle-enterprise.xml file that defines how to connect to ge.openrewrite.org")
        content readFileFromWorkspace('gradle-enterprise.xml')
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
                string('GRADLE_ENTERPRISE_ACCESS_KEY', 'gradle-enterprise-access-key')
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
                        switches("--no-daemon -Dskip.tests=true -DactiveStyle=${repoStyle} -I moderne-init.gradle")
                    } else {
                        switches('--no-daemon -Dskip.tests=true -I moderne-init.gradle')
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
                        goals '-B -Drat.skip=true -Dfindbugs.skip=true -DskipTests -DskipITs -Dcheckstyle.skip=true -Drewrite.activeStyles=${repoStyle} -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn install io.moderne:moderne-maven-plugin:0.11.1:ast'
                    } else {
                        goals '-B -Drat.skip=true -Dfindbugs.skip=true -DskipTests -DskipITs -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn install io.moderne:moderne-maven-plugin:0.11.1:ast'
                    }
                }

                node / 'buildWrappers' << 'org.jfrog.hudson.maven3.ArtifactoryMaven3Configurator' {
                    deployArtifacts true
                    artifactDeploymentPatterns {
                        includePatterns '*-ast.jar'
                    }
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
