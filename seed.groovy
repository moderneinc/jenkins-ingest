def workspaceDir = new File(__FILE__).getParentFile()

def gradleInitFileId = "gradle-init-gradle"
def gradleInitRepoFile = "moderne-init.gradle"

def mavenGradleEnterpriseXmlFileId = "maven-gradle-enterprise-xml"
def mavenGradleEnterpriseXmlRepoFile = ".mvn/gradle-enterprise.xml"

def mavenIngestSettingsXmlFileId = "maven-ingest-settings-credentials"
def mavenIngestSettingsXmlRepoFile = ".mvn/ingest-settings.xml"

def mavenAddExtensionShellFileId = "maven-add-extension.sh"
def mavenAddExtensionShellRepoLocation = ".mvn/add-extension.sh"

folder('ingest') {
    displayName('Ingest Jobs')
}

configFiles {
    groovyScript {
        id(gradleInitFileId)
        name("Gradle: init.gradle")
        comment("A Gradle init script used to inject universal plugins into a gradle build.")
        content readFileFromWorkspace('gradle/init.gradle')
    }
    xmlConfig {
        id(mavenGradleEnterpriseXmlFileId)
        name("Maven: gradle-enterprise.xml")
        comment("A gradle-enterprise.xml file that defines how to connect to ge.openrewrite.org")
        content readFileFromWorkspace('maven/gradle-enterprise.xml')
    }
    customConfig {
        id(mavenAddExtensionShellFileId)
        name("Maven: add-extension.sh")
        comment("A shell script that will add the gradle enterprise extension to a Maven Build")
        content readFileFromWorkspace('maven/add-extension.sh')
    }
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
    def repoName = tokens[0]
    def repoBranch = tokens[1]
    def repoJavaVersion = tokens[2]
    def repoStyle = tokens[3]
    def repoBuildTool = tokens[4]
    def repoJobName = repoName.replaceAll('/', '_')

    boolean isGradleBuild = ['gradle', 'gradlew'].contains(repoBuildTool);
    boolean isMavenBuild = repoBuildTool != null && (repoBuildTool.startsWith("maven") || repoBuildTool.equals("mvnw"));
    //The latest version of maven is used if the repoBuildTool is just "maven", otherwise the name repoBuildTool is treated as the jenkins name.
    def jenkinsMavenName = repoBuildTool != null && repoBuildTool == "maven" ? "maven3.x" : repoBuildTool


    println("creating job $repoJobName")
    // TODO figure out how to store rewrite version, look it up on next run, and if rewrite hasn't changed and commit hasn't changed, don't run.

    job("ingest/$repoJobName") {

        label('multi-jdk')

        jdk("java${repoJavaVersion}")

        environmentVariables {
            env('ANDROID_HOME', '/usr/lib/android-sdk')
            env('ANDROID_SDK_ROOT', '/usr/lib/android-sdk')
        }

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

        triggers {
            cron('H 4 * * *')
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
            if (isGradleBuild) {
                configFiles {
                    file(gradleInitFileId) {
                        targetLocation(gradleInitRepoFile)
                    }
                }
            }
            if (isMavenBuild) {
                configFiles {
                    file(mavenGradleEnterpriseXmlFileId) {
                        targetLocation(mavenGradleEnterpriseXmlRepoFile)
                    }
                    file(mavenIngestSettingsXmlFileId) {
                        targetLocation(mavenIngestSettingsXmlRepoFile)
                    }
                    file(mavenAddExtensionShellFileId) {
                        targetLocation(mavenAddExtensionShellRepoLocation)
                    }
                }
            }
        }

        steps {
            if (isGradleBuild) {
                gradle {
                    if (repoBuildTool == 'gradle') {
                        useWrapper(false)
                        gradleName('gradle 7.4.2')
                    } else {
                        useWrapper(true)
                        makeExecutable(true)
                    }
                    if (repoStyle != null) {
                        switches("--no-daemon -Dskip.tests=true -DactiveStyle=${repoStyle} -I ${gradleInitRepoFile}")
                    } else {
                        switches("--no-daemon -Dskip.tests=true -I ${gradleInitRepoFile}")
                    }
                    tasks('publishModernePublicationToMavenRepository')
                }
            }
        }

        if (isMavenBuild) {
            // A step that runs before the maven build to setup the gradle enterprise extension.
            steps {
                // Adds a shell script into the Jobs workspace in /tmp.
                // We should add the 'add-gradle-enterprise-extension.sh' and reference that in the shell method.
                shell("bash ${mavenAddExtensionShellRepoLocation}")
            }
            configure { node ->

                node / 'builders' << 'org.jfrog.hudson.maven3.Maven3Builder' {
                    mavenName jenkinsMavenName
                    useWrapper(repoBuildTool == 'mvnw')

                    goals "-B -DpomCacheDirectory=. -Drat.skip=true -Darchetype.test.skip=true -Dmaven.findbugs.enable=false -Dspotbugs.skip=true -Dpmd.skip=true -Dcpd.skip=true -Dfindbugs.skip=true -DskipTests -DskipITs -Dcheckstyle.skip=true -Denforcer.skip=true -s ${mavenIngestSettingsXmlRepoFile} ${(repoStyle != null) ? "-Drewrite.activeStyle=${repoStyle}" : ''} -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn install io.moderne:moderne-maven-plugin:0.11.3:ast"
                }

                node / 'buildWrappers' << 'org.jfrog.hudson.maven3.ArtifactoryMaven3Configurator' {
                    deployArtifacts true
                    artifactDeploymentPatterns {
                        includePatterns '*-ast.*'
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
