import hudson.FilePath
import hudson.remoting.VirtualChannel

import java.nio.charset.StandardCharsets

FilePath workspaceFilePath = build.getWorkspace()
def patchTempFile = workspaceFilePath.createTempFile('moderne', '.patch')
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

    int gitApplyRc = patchTempFile.act({ File f, VirtualChannel c ->
        runProc("git apply ${f.getName()}")
    } as FilePath.FileCallable)
    if (gitApplyRc != 0) {
        println 'Applying git patch failed!'
        return -1
    }

} finally {
    patchTempFile.delete()
}

int runProc(String command) {
    println "executing ${command}"
    build.getWorkspace().act({File f, VirtualChannel channel ->
        def proc = command.execute(null, f)
        def output = new StringWriter()
        def error = new StringWriter()
        proc.waitForProcessOutput(output, error)
        println("exit value: ${proc.exitValue()}")
        println("output: ${output}")
        println("err: ${error}")
        return proc.exitValue()
    } as FilePath.FileCallable)
}

static int copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
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