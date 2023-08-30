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
                scmHost,repoName,repoBranch,repoStyle,repoBuildAction,repoSkip,skipReason
                ,foo,trim-leading-origin,style,build,,
                ,foo,set-java-when-empty,style,build,,
                ,foo,keep-java-when-not-empty,style,build,,
                ,foo,skip-when-archived,style,build,,
                """;
        String newCsv = """
                github.com,foo,trim-leading-origin,style,build,,
                ,foo,set-java-when-empty,style,build,,
                ,foo,keep-java-when-not-empty,style,build,,
                ,foo,skip-when-archived,style,build,TRUE,archived
                """;
        Path repos = tempdir.resolve("repos.csv");
        Path new_ = tempdir.resolve("new.csv");
        Files.writeString(repos, original);
        Files.writeString(new_, newCsv);

        Merger.mergeDatatables(repos, new_);

        String actual = Files.readString(repos);
        assertEquals("""
                scmHost,repoName,repoBranch,repoStyle,repoBuildAction,repoSkip,skipReason
                ,foo,keep-java-when-not-empty,style,build,,
                ,foo,set-java-when-empty,style,build,,
                ,foo,skip-when-archived,style,build,TRUE,archived
                ,foo,trim-leading-origin,style,build,,
                """, actual);
    }

    @Test
    void mergeBgjug(@TempDir Path tempdir) throws Exception {
        String original = """
                scmHost,repoName,repoBranch,repoStyle,repoBuildAction,repoSkip,skipReason
                ,bgiegel/Starpoints-app,master,,,,
                ,bgjug/jprime,master,,,,
                ,bgogetap/StickyHeaders,master,,,,
                """;
        String newCsv = """
                ,bgjug/common-cdi-extension,master,,,,
                ,bgjug/jcache-workshop,master,,,,
                ,bgjug/jprime-android,master,,,,
                ,bgjug/jprime,master,,,,
                ,bgjug/jprime_registration,master,,,,
                ,bgjug/kafka-cdi-extension,master,,,,
                ,bgjug/microprofile-hol-1x,master,,,,
                ,bgjug/microprofile-hol,master,,,,
                ,bgjug/mvc10-workshop,master,,,,
                ,bgjug/repo-maven-plugin,master,,,,
                ,bgjug/spring-boot-forge-addon,master,,,,
                """;
        Path repos = tempdir.resolve("repos.csv");
        Path new_ = tempdir.resolve("new.csv");
        Files.writeString(repos, original);
        Files.writeString(new_, newCsv);
        
        Merger.mergeDatatables(repos, new_);
        String actual = Files.readString(repos);
        assertEquals("""
                scmHost,repoName,repoBranch,repoStyle,repoBuildAction,repoSkip,skipReason
                ,bgiegel/Starpoints-app,master,,,,
                ,bgjug/common-cdi-extension,master,,,,
                ,bgjug/jcache-workshop,master,,,,
                ,bgjug/jprime,master,,,,
                ,bgjug/jprime-android,master,,,,
                ,bgjug/jprime_registration,master,,,,
                ,bgjug/kafka-cdi-extension,master,,,,
                ,bgjug/microprofile-hol,master,,,,
                ,bgjug/microprofile-hol-1x,master,,,,
                ,bgjug/mvc10-workshop,master,,,,
                ,bgjug/repo-maven-plugin,master,,,,
                ,bgjug/spring-boot-forge-addon,master,,,,
                ,bgogetap/StickyHeaders,master,,,,
                """, actual);
    }
}