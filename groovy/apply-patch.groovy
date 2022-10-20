import hudson.FilePath
import hudson.model.Node
import hudson.util.StreamTaskListener

import java.nio.charset.StandardCharsets

Node workerNode = build.getWorkspace().toComputer().getNode()
FilePath nodeWorkspacePath = workerNode.getWorkspaceFor(build.getProject())
def patchTempFile = nodeWorkspacePath.createTempFile('moderne', '.patch')
try {
    println('Writing patch to temp file')

    def conn = (HttpURLConnection) new URL(build.environment.patchDownloadUrl).openConnection()

    int responseCode = conn.getResponseCode()
    if (responseCode == HttpURLConnection.HTTP_OK) {
        def contentDisposition = conn.getHeaderField("Content-Disposition")
        int index = contentDisposition.indexOf('filename=')
        if (index > 0) {
            filename = contentDisposition.substring(index + 10)
        }
        copyStream(conn.getInputStream(), patchTempFile.write())
    } else {
        if (conn.getErrorStream() == null) {
            println "Failed to download patch file, response code: ${responseCode}"
        } else {
            def errorBaos = new ByteArrayOutputStream()
            copyStream(conn.getErrorStream(), errorBaos)
            def errorMessage = new String(errorBaos.toByteArray(), StandardCharsets.UTF_8)
            println "Failed to download patch file, response code: ${responseCode}, response body: ${errorMessage}"
        }
        return -1
    }
    println('Applying patch using git')
    ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream()
    def workerLauncher = workerNode.createLauncher(new StreamTaskListener(outputBuffer, StandardCharsets.UTF_8))
    def proc = workerLauncher.launch().cmdAsSingleString("/usr/bin/git apply ${patchTempFile.getName()}").pwd(build.getWorkspace()).start()
    proc.getStdout()
    if (proc.join() != 0) {
        println 'Applying git patch failed!'
        return -1
    }


} finally {
    patchTempFile.delete()
}


static def copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
    try {
        def byteCount = 0
        byte[] buffer = new byte[4096]
        int bytesRead
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead)
            byteCount += bytesRead
        }
        outputStream.flush()
        return byteCount
    } finally {
        outputStream.close()
        inputStream.close()
    }

}