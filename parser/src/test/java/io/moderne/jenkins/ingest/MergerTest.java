package io.moderne.jenkins.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MergerTest {

    @Test
    void mergeDatatables(@TempDir Path tempdir) throws Exception {
        String original = """
                scmHost,repoName,repoBranch,mavenTool,gradleTool,jdkTool,repoStyle,repoBuildAction,repoSkip,skipReason
                ,foo,trim-leading-origin,maven,gradle,java17,style,build,,
                ,foo,set-java-when-empty,maven,gradle,,style,build,,
                ,foo,keep-java-when-not-empty,maven,gradle,java8,style,build,,
                ,foo,skip-when-archived,maven,gradle,java17,style,build,,
                """;
        String newCsv = """
                github.com,foo,trim-leading-origin,maven,gradle,java17,style,build,,
                ,foo,set-java-when-empty,maven,gradle,java17,style,build,,
                ,foo,keep-java-when-not-empty,maven,gradle,java8,style,build,,
                ,foo,skip-when-archived,maven,gradle,java17,style,build,TRUE,archived
                """;
        Path repos = tempdir.resolve("repos.csv");
        Path new_ = tempdir.resolve("new.csv");
        Files.writeString(repos, original);
        Files.writeString(new_, newCsv);

        Merger.mergeDatatables(repos, new_);

        String actual = Files.readString(repos);
        assertEquals("""
                scmHost,repoName,repoBranch,mavenTool,gradleTool,jdkTool,repoStyle,repoBuildAction,repoSkip,skipReason
                ,foo,keep-java-when-not-empty,maven,gradle,java8,style,build,,
                ,foo,set-java-when-empty,maven,gradle,java17,style,build,,
                ,foo,skip-when-archived,maven,gradle,java17,style,build,TRUE,archived
                ,foo,trim-leading-origin,maven,gradle,java17,style,build,,
                """, actual);
    }

    @Test
    void mergeBgjug(@TempDir Path tempdir) throws Exception {
        String original = """
                scmHost,repoName,repoBranch,mavenTool,gradleTool,jdkTool,repoStyle,repoBuildAction,repoSkip,skipReason
                ,bgiegel/Starpoints-app,master,maven,,java8,,,,
                ,bgjug/jprime,master,maven,,java8,,,,
                ,bgogetap/StickyHeaders,master,,,java8,,,,
                """;
        String newCsv = """
                ,bgjug/common-cdi-extension,master,,,java17,,,,
                ,bgjug/jcache-workshop,master,,,java17,,,,
                ,bgjug/jprime-android,master,,,java17,,,,
                ,bgjug/jprime,master,,,java17,,,,
                ,bgjug/jprime_registration,master,,,java17,,,,
                ,bgjug/kafka-cdi-extension,master,,,java17,,,,
                ,bgjug/microprofile-hol-1x,master,,,java17,,,,
                ,bgjug/microprofile-hol,master,,,java17,,,,
                ,bgjug/mvc10-workshop,master,,,java17,,,,
                ,bgjug/repo-maven-plugin,master,,,java17,,,,
                ,bgjug/spring-boot-forge-addon,master,,,java17,,,,
                """;
        Path repos = tempdir.resolve("repos.csv");
        Path new_ = tempdir.resolve("new.csv");
        Files.writeString(repos, original);
        Files.writeString(new_, newCsv);
        
        Merger.mergeDatatables(repos, new_);
        String actual = Files.readString(repos);
        assertEquals("""
                scmHost,repoName,repoBranch,mavenTool,gradleTool,jdkTool,repoStyle,repoBuildAction,repoSkip,skipReason
                ,bgiegel/Starpoints-app,master,maven,,java8,,,,
                ,bgjug/common-cdi-extension,master,,,java17,,,,
                ,bgjug/jcache-workshop,master,,,java17,,,,
                ,bgjug/jprime,master,maven,,java8,,,,
                ,bgjug/jprime-android,master,,,java17,,,,
                ,bgjug/jprime_registration,master,,,java17,,,,
                ,bgjug/kafka-cdi-extension,master,,,java17,,,,
                ,bgjug/microprofile-hol,master,,,java17,,,,
                ,bgjug/microprofile-hol-1x,master,,,java17,,,,
                ,bgjug/mvc10-workshop,master,,,java17,,,,
                ,bgjug/repo-maven-plugin,master,,,java17,,,,
                ,bgjug/spring-boot-forge-addon,master,,,java17,,,,
                ,bgogetap/StickyHeaders,master,,,java8,,,,
                """, actual);
    }
}