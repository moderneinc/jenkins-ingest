import groovy.json.JsonSlurper

def jsonSlurper = new JsonSlurper()
def workspaceDir = new File(__FILE__).getParentFile()
def repos = jsonSlurper.parse(new File(workspaceDir, 'repos.json'))

repos.each { Map repoConfig ->

    def repoBuildTool = null

    try {
        repoBuildTool = buildTool(repoConfig.ownerAndName)
        println("${repoConfig.ownerAndName} buildTool: ${repoBuildTool}")
    } catch (Exception e) {
        println("Error getting build tool for ${repoConfig.ownerAndName}")
        println(e)
        return;
    }
    def jobName = repoConfig.ownerAndName.replaceAll('/', '_')

    if (repoConfig.branch == null) {
        repoConfig.branch = getDefaultBranch(repoConfig.ownerAndName)
        println("${repoConfig.ownerAndName} defaultBranch: ${repoConfig.branch}")
    }
    if (repoConfig.label == null) {
        repoConfig.label = 'java11'
    }

    println("creating job $jobName")
    // TODO figure out how to store rewrite version, look it up on next run, and if rewrite hasn't changed and commit hasn't changed, don't run.
    freeStyleJob("$jobName") {

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
            cron('H 8 * * *')
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

def getGithubPat() {
    def githubPat = getBinding().getVariables()['GITHUB_PAT']
    return githubPat
}

def checkGithubForFile(String repoOwnerAndName, String filename) {
    def url = new URL("https://api.github.com/repos/${repoOwnerAndName}/contents/${filename}")
    def urlConnection = (HttpURLConnection) url.openConnection()
    urlConnection.setRequestProperty('Accept', 'application/vnd.github.v3+json')
    def githubPat = getGithubPat()
    urlConnection.setRequestProperty('Authorization', "Bearer ${githubPat}")
    def statusCode = urlConnection.getResponseCode()
    Thread.sleep(1000)
    if (statusCode == 200) {
        return true
    } else if (statusCode == 404) {
        return false
    } else {
        def errorStream = urlConnection.getErrorStream()
        Scanner s = new Scanner(errorStream).useDelimiter("\\A")
        String body = s.hasNext() ? s.next() : ""
        println("Error calling github: url: ${url}, status code: ${statusCode}, body: ${body}")
        return false
    }
}

def getDefaultBranch(String repoOwnerAndName) {
    def url = new URL("https://api.github.com/repos/${repoOwnerAndName}")
    def urlConnection = (HttpURLConnection) url.openConnection()
    urlConnection.setRequestProperty('Accept', 'application/vnd.github.v3+json')
    def githubPat = getGithubPat()
    urlConnection.setRequestProperty('Authorization', "Bearer ${githubPat}")
    def jsonSlurper = new JsonSlurper()
    def statusCode = urlConnection.getResponseCode()
    if (statusCode == 200) {
        def repo = jsonSlurper.parse(urlConnection.getInputStream())
        Thread.sleep(1000)
        return repo.get('default_branch')
    } else {
        def errorStream = urlConnection.getErrorStream()
        Scanner s = new Scanner(errorStream).useDelimiter("\\A")
        String body = s.hasNext() ? s.next() : ""
        println("Error calling github: url: ${url}, status code: ${statusCode}, body: ${body}")
        Thread.sleep(1000)
        return false
    }
}
