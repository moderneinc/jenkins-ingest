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
            // A step that runs before the maven build to setup the gradle enterprise extension.
            steps {
                // Adds a shell script into the Jobs workspace in /tmp.
                // We should add the 'add-gradle-enterprise-extension.sh' and reference that in the shell method.
                shell(
'''
MVN_DIR=".mvn"
EXT_FILE="extensions.xml"
MVN_EXT_XML="<extension><groupId>com.gradle</groupId><artifactId>gradle-enterprise-maven-extension</artifactId><version>1.14.2</version></extension>"

# Create the .mvn directory if it does not exist.
if [ ! -d "$MVN_DIR" ]; then
    mkdir "$MVN_DIR"
fi

# Create the extensions.xml file if it does not exist.
if [ ! -f "$MVN_DIR/$EXT_FILE" ]; then
    touch "$MVN_DIR/$EXT_FILE"
    # Add the `<extensions>` tag.
    echo "<extensions>" >> "$MVN_DIR/$EXT_FILE"
    echo "</extensions>" >> "$MVN_DIR/$EXT_FILE"
    ADD=$(echo $MVN_EXT_XML | sed 's/\\//\\\\\\//g')
    sed -i "/<\\/extensions>/ s/.*/${ADD}\\n&/" "$MVN_DIR/$EXT_FILE"
else
    # Assumes the <extensions> already exists and the <extension> tag does exist.
    ADD=$(echo $MVN_EXT_XML | sed 's/\\//\\\\\\//g')
    sed -i "/<\\/extensions>/ s/.*/${ADD}\\n&/" "$MVN_DIR/$EXT_FILE"
fi
''')
            }
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
