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
                ,foo,trim-leading-origin,maven,gradle,java17,style,build,,
                ,foo,set-java-when-empty,maven,gradle,java17,style,build,,
                ,foo,keep-java-when-not-empty,maven,gradle,java8,style,build,,
                ,foo,skip-when-archived,maven,gradle,java17,style,build,TRUE,archived
                """, actual);
    }

}