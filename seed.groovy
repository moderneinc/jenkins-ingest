import groovy.json.JsonSlurper

def jsonSlurper = new JsonSlurper()
def repos = jsonSlurper.parse(new File('repos.json'))

repos.each { Map repoConfig ->

    def buildTool = buildTool(repoConfig.ownerAndName)
    def jobName = repoConfig.ownerAndName.replaceAll('/', '_')
    job("$jobName") {

        label("$repoConfig.label")

        scm {
            git {
                remote {
                    url("http://github.com/${repoConfig.ownerAndName}")
                    branch(repoConfig.branch)
                }
                extensions {
                    localBranch("master")
                }
            }
        }

        triggers {
            cron('H H * * *')
        }

        wrappers {
            credentialsBinding {
                usernamePassword('ARTIFACTORY_USER', 'ARTIFACTORY_PASSWORD', 'artifactory')
            }
            if (buildTool == BuildTool.GRADLE) {
                configFiles {
                    file('moderne-gradle-init') {
                        targetLocation('moderne-init.gradle')
                    }
                }
            }
        }

        steps {
            if (buildTool == BuildTool.GRADLE) {
                gradle {
                    useWrapper()
                    switches('--no-daemon -I moderne-init.gradle')
                    tasks('publishModernePublicationToMavenRepository')
                }
            }
        }

        if (buildTool == BuildTool.MAVEN) {
            configure { node ->

                node / 'builders' << 'org.jfrog.hudson.maven3.Maven3Builder' {
                    mavenName 'maven 3'
                    goals 'io.moderne:moderne-maven-plugin:0.10.0:ast install'
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

enum BuildTool {
    MAVEN, GRADLE
}

def buildTool(String repoOwnerAndName) {
    if (isGradle(repoOwnerAndName)) {
        return BuildTool.GRADLE
    } else if (isMaven(repoOwnerAndName)) {
        return BuildTool.MAVEN
    } else {
        throw new IllegalStateException('Repositories must contain build.gradle, build.gradle.kts, or pom.xml at the root.')
    }
}

def isMaven(String repoOwnerAndName) {
    return checkGithubForFile(repoOwnerAndName, "pom.xml")
}

def isGradle(String repoOwnerAndName) {
    return checkGithubForFile(repoOwnerAndName, "build.gradle") ||
            checkGithubForFile(repoOwnerAndName, "build.gradle.kts")
}

def checkGithubForFile(String repoOwnerAndName, String filename) {
    def getFile = new URL("https://api.github.com/repos/${repoOwnerAndName}/contents/${filename}").openConnection()
    getFile.setRequestProperty('Accept', 'application/vnd.github.v3+json')
    return getFile.getResponseCode() == 200
}