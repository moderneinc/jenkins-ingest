def workspaceDir = new File(__FILE__).getParentFile()

def moderneCLIVersion= 'v0.0.41'
def moderneCLIPlatform = 'linux'
def moderneCLIURL = "https://pkgs.dev.azure.com/moderneinc/moderne_public/_packaging/moderne/maven/v1/io/moderne/moderne-cli-${moderneCLIPlatform}/${moderneCLIVersion}/moderne-cli-${moderneCLIPlatform}-${moderneCLIVersion}"
def doCliDownload = false;

folder('validate') {
    displayName('Recipe Run Validation Jobs')
}

new File(workspaceDir, 'repos.csv').splitEachLine(',') { tokens ->
    if (tokens[0].startsWith('repoName')) {
        return
    }
    def scmHost = tokens[0] ?: 'github.com'
    def repoName = tokens[1]
    def repoBranch = tokens[2]
    def mavenTool = tokens[3] ? "maven '${tokens[3]}' " : ''
    def gradleTool = tokens[4] ? "gradle '${tokens[4]}' " : ''
    def jdkTool = tokens[5] ? "jdk '${tokens[5]}' " : ''
    def repoStyle = tokens[6] ?: ''
    def repoBuildAction = tokens[7] ?: ''
    def repoSkip = tokens[8]
    def cliDownload = doCliDownload ? """
sh "curl --request GET ${moderneCLIURL} > mod"
sh "chmod 755 mod"
""" : ''


    if ('true' == repoSkip) {
        return
    }

    def repoJobName = repoName.replaceAll('/', '_')

    println("creating job $repoJobName")

    pipelineJob("validate/$repoJobName") {
        parameters {stringParam('buildName')}
        parameters {stringParam('patchDownloadUrl')}
        definition {
            cps {
                sandbox(true)
                script("""
pipeline {
    agent {
        label 'validate'
    }
    tools {
        ${mavenTool}
        ${gradleTool}
        ${jdkTool}
    }
    environment {
        MOD_TOKEN = credentials('modToken')
        SCM_TOKEN = credentials('scmToken_${scmHost}')
    }
    options {
         buildDiscarder(logRotator(daysToKeepStr: '7'))
    }
    stages {
        stage('Build') {
            steps {
                script {
                    currentBuild.displayName = "\${params.buildName}"
                }
                git (url: 'https://${scmHost}/${repoName}.git', branch: '${repoBranch}', credentialsId: 'jkschneider-pat')
                ${cliDownload}
                script {
                    if (params.patchDownloadUrl) {
                        withEnv(["PATCH_URL=\${params.patchDownloadUrl}"]) {
                           println 'fetching patch'
                           sh 'curl -o patch.diff --request GET --url \$PATCH_URL --header "Authorization: Bearer \$MOD_TOKEN" --header "x-moderne-scmtoken: \$SCM_TOKEN"'
                        }
                        println 'applying patch'
                        sh 'git apply patch.diff'
                    } else {
                        println 'building without patch'
                    }
                }
                sh 'mod build --path . --activeStyle ${repoStyle ?: '""'} --buildAction ${repoBuildAction ?: '""'}'
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}
                  """)
            }
        }
    }
}
