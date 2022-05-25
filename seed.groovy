import groovy.json.JsonSlurper

def jsonSlurper = new JsonSlurper()
def workspaceDir = new File(__FILE__).getParentFile()
def repos = jsonSlurper.parse(new File(workspaceDir, 'repos.json'))

repos.each { Map repoConfig ->

    def repoBuildTool = null

    try {
        repoBuildTool = buildTool(repoConfig.ownerAndName)
    } catch (Exception e) {
        println("Error getting build tool for ${repoConfig.ownerAndName}")
        println(e)
        return;
    }
    def jobName = repoConfig.ownerAndName.replaceAll('/', '_')

    if (repoConfig.branch == null) {
        repoConfig.branch = getDefaultBranch(repoConfig.ownerAndName)
    }
    if (repoConfig.label == null) {
        repoConfig.label = 'java11'
    }

    Thread.sleep(10000)

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
            if (repoBuildTool == BuildTool.GRADLE) {
                configFiles {
                    file('moderne-gradle-init') {
                        targetLocation('moderne-init.gradle')
                    }
                }
            }
        }

        steps {
            if (repoBuildTool == BuildTool.GRADLE) {
                gradle {
                    useWrapper()
                    // TODO specify style
                    switches('--no-daemon -I moderne-init.gradle')
                    tasks('publishModernePublicationToMavenRepository')
                }
            }
        }

        if (repoBuildTool == BuildTool.MAVEN) {
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
    def urlConnection = new URL("https://api.github.com/repos/${repoOwnerAndName}/contents/${filename}").openConnection()
    urlConnection.setRequestProperty('Accept', 'application/vnd.github.v3+json')
    urlConnection.setRequestProperty('Authorization', System.getenv('GITHUB_PAT'))
    return urlConnection.getResponseCode() == 200
}

def getDefaultBranch(String repoOwnerAndName) {
    def urlConnection = new URL("https://api.github.com/repos/${repoOwnerAndName}").openConnection()
    urlConnection.setRequestProperty('Accept', 'application/vnd.github.v3+json')
    urlConnection.setRequestProperty('Authorization', System.getenv('GITHUB_PAT'))
    def jsonSlurper = new JsonSlurper()
    def repo = jsonSlurper.parse(urlConnection.getInputStream())
    return repo.get('default_branch')
}
