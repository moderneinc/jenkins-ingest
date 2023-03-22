package io.moderne.jenkins.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParserTest {

    @Test
    void test(@TempDir Path tempDir) throws IOException {
        Path datatableFile = tempDir.resolve("datatable.csv");
        Files.writeString(datatableFile, """
                repositoryOrigin,repositoryPath,repositoryBranch,type,version,command,exitCode,requiredJavaVersion,logOutput
                github.com,apache/cassandra-accord,trunk,Gradlew,8.0,./gradlew moderneJar -DmoderneVersion=latest.release -Dorg.gradle.jvmargs=-Xmx2G --no-daemon --warning-mode all --stacktrace --init-script /home/ubuntu/.config/moderne/init.gradle,1,,,
                github.com,apache/helix,master,Maven,,/home/ubuntu/jenkins-agent/tools/hudson.tasks.Maven_MavenInstallation/maven/bin/mvn package io.moderne:moderne-maven-plugin:LATEST:ast --batch-mode -Darchetype.test.skip=true -Dcheckstyle.skip=true -Dcpd.skip=true -Denforcer.skip=true -Dfindbugs.skip=true -Dgpg.skip -Dlicense.skipCheckLicense=true -Dlicense.skip=true -Dmaven.findbugs.enable=false -Dmaven.javadoc.skip=true -Dpmd.skip=true -Drat.numUnapprovedLicenses=1000 -Drat.skip=true -Dskip.bower -Dskip.grunt -Dskip.gulp -DskipITs -Dskip.jspm -Dskip.karma -Dskip.npm -DskipSource -DskipTests -Dskip.webpack -Dskip.yarn -Dspotbugs.skip=true -Dspotless.check.skip=true -Dsurefire.skip=true -Dtest.skip=true --update-snapshots,1,11,,
                github.com,apache/ofbiz-framework,trunk,Gradlew,7.6,./gradlew moderneJar -DmoderneVersion=latest.release -Dorg.gradle.jvmargs=-Xmx2G --no-daemon --warning-mode all --stacktrace --init-script /home/ubuntu/.config/moderne/init.gradle,1,17,,
                github.com,apache/poi,trunk,Gradlew,4.9,./gradlew moderneJar -DmoderneVersion=latest.release -Dorg.gradle.jvmargs=-Xmx2G --no-daemon --warning-mode all --stacktrace --init-script /home/ubuntu/.config/moderne/init.gradle,1,,,
                github.com,apache/servicemix5,trunk,Maven,,/home/ubuntu/jenkins-agent/tools/hudson.tasks.Maven_MavenInstallation/maven/bin/mvn package io.moderne:moderne-maven-plugin:LATEST:ast --batch-mode -Darchetype.test.skip=true -Dcheckstyle.skip=true -Dcpd.skip=true -Denforcer.skip=true -Dfindbugs.skip=true -Dgpg.skip -Dlicense.skipCheckLicense=true -Dlicense.skip=true -Dmaven.findbugs.enable=false -Dmaven.javadoc.skip=true -Dpmd.skip=true -Drat.numUnapprovedLicenses=1000 -Drat.skip=true -Dskip.bower -Dskip.grunt -Dskip.gulp -DskipITs -Dskip.jspm -Dskip.karma -Dskip.npm -DskipSource -DskipTests -Dskip.webpack -Dskip.yarn -Dspotbugs.skip=true -Dspotless.check.skip=true -Dsurefire.skip=true -Dtest.skip=true --update-snapshots,-1,,,
                github.com,apache/shenyu,master,Mvnw,3.2.0,./mvnw package io.moderne:moderne-maven-plugin:LATEST:ast --batch-mode -Darchetype.test.skip=true -Dcheckstyle.skip=true -Dcpd.skip=true -Denforcer.skip=true -Dfindbugs.skip=true -Dgpg.skip -Dlicense.skipCheckLicense=true -Dlicense.skip=true -Dmaven.findbugs.enable=false -Dmaven.javadoc.skip=true -Dpmd.skip=true -Drat.numUnapprovedLicenses=1000 -Drat.skip=true -Dskip.bower -Dskip.grunt -Dskip.gulp -DskipITs -Dskip.jspm -Dskip.karma -Dskip.npm -DskipSource -DskipTests -Dskip.webpack -Dskip.yarn -Dspotbugs.skip=true -Dspotless.check.skip=true -Dsurefire.skip=true -Dtest.skip=true --update-snapshots,1,,,
                """);
        Path reposFile = tempDir.resolve("repos.csv");
        Files.writeString(reposFile, """
                scmHost,repoName,repoBranch,mavenTool,gradleTool,jdkTool,repoStyle,repoBuildAction,repoSkip,skipReason
                ,apache/cassandra-accord,trunk,,,java11,,,,
                ,apache/helix,master,maven,,java8,,,,
                ,apache/ofbiz-framework,trunk,,,java11,,,,
                ,apache/poi,trunk,,,java8,,,,
                ,apache/servicemix5,trunk,maven,,java8,,,,
                ,apache/shenyu,master,maven,,java8,,,,
                ,apache/maven,master,maven,,java8,,,,
                """);
        Parser.updateReposBasedOnDatatable(reposFile, datatableFile);
        String updatedCsv = Files.readString(reposFile);
        assertEquals("""
                scmHost,repoName,repoBranch,mavenTool,gradleTool,jdkTool,repoStyle,repoBuildAction,repoSkip,skipReason
                ,apache/cassandra-accord,trunk,,,java11,,,,
                ,apache/helix,master,maven,,java11,,,,
                ,apache/ofbiz-framework,trunk,,,java17,,,,
                ,apache/poi,trunk,,,java8,,,TRUE,Gradle wrapper 4.9 is not supported
                ,apache/servicemix5,trunk,maven,,java8,,,,
                ,apache/shenyu,master,maven,,java8,,,TRUE,Maven wrapper 3.2.0 is not supported
                ,apache/maven,master,maven,,java8,,,,
                """, updatedCsv);
    }

}