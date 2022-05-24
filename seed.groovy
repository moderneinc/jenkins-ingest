import groovy.json.JsonSlurper

def jsonSlurper = new JsonSlurper()
def workspaceDir = new File(__FILE__).getParentFile()
def repos = jsonSlurper.parse(new File(workspaceDir, 'repos.json'))

repos.each { Map repoConfig ->

    def buildTool = buildTool(repoConfig.ownerAndName)
    def jobName = repoConfig.ownerAndName.replaceAll('/', '_')

    if (repoConfig.branch == null) {
        repoConfig.branch = getDefaultBranch(repoConfig.ownerAndName)
    }
    if (repoConfig.label == null) {
        repoConfig.label = 'java11'
    }

    // TODO figure out how to store rewrite version, look it up on next run, and if rewrite hasn't changed and commit hasn't changed, don't run.
    job("$jobName") {

        label("$repoConfig.label")

        scm {
            git {
                remote {
                    url("http://github.com/${repoConfig.ownerAndName}")
                    branch(repoConfig.branch)
                }
                extensions {
                    localBranch(repoConfig.branch)
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
                    // TODO specify style
                    switches('--no-daemon -I moderne-init.gradle')
                    tasks('publishModernePublicationToMavenRepository')
                }
            }
        }

        if (buildTool == BuildTool.MAVEN) {
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

def getDefaultBranch(String repoOwnerAndName) {
    def jsonSlurper = new JsonSlurper()
    def repo = jsonSlurper.parse(new URL("http://api.github.com/repos/${repoOwnerAndName}"))
    return repo.get('default_branch')
}