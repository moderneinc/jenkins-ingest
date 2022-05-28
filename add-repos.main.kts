#!/usr/bin/env kotlin

import java.io.*
import java.nio.file.Files
import java.util.concurrent.TimeUnit

val repoWriter = FileOutputStream("repos.csv", true).bufferedWriter()

repoWriter.use {

    File(args[0]).forEachLine {

        if (it.startsWith("repoName")) {
            return@forEachLine
        }
        // repoName, branch, label, style, buildTool
        val row = it.split(",")
        val repoName: String = row[0]
        var branch = ""
        if (row.size > 1) {
            branch = row[1]
        }
        var label = ""
        if (row.size > 2) {
            label = row[2]
        }
        var style = ""
        if (row.size > 3) {
            style = row[3]
        }
        var buildTool = ""
        if (row.size > 4) {
            buildTool = row[4]
        }

        if (label.isBlank()) {
            label = "java11"
        }

        val gitPath = Files.createTempDirectory("moderne-git")
        try {
            if (!"git clone --depth 1 --no-checkout git@github.com:$repoName.git".runCommand(gitPath.toFile())) {
                println("$repoName does not exist in github")
                return@forEachLine
            }
            val cloneDirPath = gitPath.resolve(repoName.substring(repoName.indexOf('/') + 1))
            val cloneDir = cloneDirPath.toFile()
            if (buildTool.isBlank()) {
                "git sparse-checkout set --no-cone /build.gradle.kts /build.gradle /pom.xml /gradlew".runCommand(
                    cloneDir
                )
                "git checkout".runCommand(cloneDir)

                if (Files.exists(cloneDirPath.resolve("build.gradle.kts"))
                    || Files.exists(cloneDirPath.resolve("build.gradle"))
                ) {
                    buildTool = if (Files.exists(cloneDirPath.resolve("gradlew"))) {
                        "gradlew"
                    } else {
                        "gradle"
                    }
                } else if (Files.exists(cloneDirPath.resolve("pom.xml"))) {
                    buildTool = "maven"
                }
            }

            if (buildTool.isBlank()) {
                println("Skipping $repoName, because none of the supported build tool files (build.gradle.kts, build.gradle, or pom.xml) is present at the root.")
                return@forEachLine
            }

            if (branch.isBlank()) {
                branch = "git rev-parse --abbrev-ref HEAD".getCommandOutput(cloneDir).trim()
            }

            repoWriter.appendLine("$repoName,$branch,$label,$style,$buildTool")
        } finally {
            gitPath.toFile().deleteRecursively()
        }
    }
}

fun String.getCommandOutput(workingDir: File) : String {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(10, TimeUnit.SECONDS)
    return if (proc.exitValue() == 0) {
        proc.inputStream.bufferedReader().readText()
    } else {
        throw IllegalStateException("Error running command: " + proc.errorStream.bufferedReader().readText())
    }
}

fun String.runCommand(workingDir: File) : Boolean {
    return try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()


        if (proc.waitFor(10, TimeUnit.MINUTES)) {
            proc.exitValue() == 0
        } else {
            println("timed out after 10 minutes, waiting for $this")
            false
        }
    } catch(e: IOException) {
        e.printStackTrace()
        false
    }
}