package io.moderne.jenkins.ingest;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {

    private static final String MINIMUM_MAVEN_VERSION = "3.3.1";
    private static final String MINIMUM_GRADLE_VERSION = "4.10";

    public static void main(String[] args) throws IOException {
        updateReposBasedOnDatatable(Path.of(args[0]), Path.of(args[1]));
    }

    public static void updateReposBasedOnDatatable(Path reposFile, Path datatableFile) throws IOException {
        Map<Key, DataTableRow> datatableRowsByKey = parseDataTable(datatableFile);
        updateReposCsv(reposFile, datatableRowsByKey);
    }

    private static Map<Key, DataTableRow> parseDataTable(Path datatableFile) throws IOException {
        // header: repositoryOrigin,repositoryPath,repositoryBranch,type,version,command,exitCode,requiredJavaVersion,logOutput
        List<String> datatableLines = Files.readAllLines(datatableFile);
        return datatableLines.stream()
                .skip(1)
                .map(line -> line.replace("\"", ""))
                .map(line -> line.split(",", -1))
                .map(row -> new DataTableRow(row[0].equals("github.com") ? "" : row[0],
                        row[1], row[2], row[3], row[4], row[5], row[6], row[7], row[8]))
                .collect(Collectors.toMap(
                        row -> new Key(row.repositoryOrigin(), row.repositoryPath(), row.repositoryBranch()),
                        row -> row,
                        (a, b) -> b,
                        LinkedHashMap::new));
    }

    private static void updateReposCsv(Path reposFile, Map<Key, DataTableRow> datatableRowsByKey) throws IOException {
        // header: scmHost,repoName,repoBranch,mavenTool,gradleTool,jdkTool,repoStyle,repoBuildAction,repoSkip,skipReason
        List<String> reposLines = Files.readAllLines(reposFile);
        try (FileWriter writer = new FileWriter(reposFile.toFile())) {
            // Loop over repoLines and update them based matching data table rows
            for (String line : reposLines) {
                String[] split = line.split(",", -1);
                CsvRow csvRow = new CsvRow(split[0], split[1], split[2], split[3], split[4], split[5], split[6], split[7], split[8], split[9]);
                DataTableRow datatableRow = datatableRowsByKey.get(new Key(csvRow.scmHost(), csvRow.repoName(), csvRow.repoBranch()));
                if (datatableRow != null) {
                    csvRow = updateCsvRow(csvRow, datatableRow);
                }
                writer.write(csvRow.toString() + "\n");
            }
        }
    }

    private static CsvRow updateCsvRow(CsvRow csvRow, DataTableRow datatableRow) {
        // Pick minimum required Java version
        Integer requiredJavaVersion = javaVersion(datatableRow.requiredJavaVersion());
        if (requiredJavaVersion != null) {
            csvRow = csvRow.withJdkTool("java" + requiredJavaVersion);
            if (requiredJavaVersion < 8) {
                csvRow = csvRow
                        .withRepoSkip("TRUE")
                        .withSkipReason("Java version " + requiredJavaVersion + " is not supported");
            }
        }

        // Skip outdated Maven and Gradle wrappers
        String wrapper = datatableRow.type();
        String wrapperVersion = datatableRow.version();
        if (wrapper != null && !wrapper.isBlank() && wrapperVersion != null && !wrapperVersion.isBlank()) {
            if (wrapper.equalsIgnoreCase("Mvnw") && 0 < MINIMUM_MAVEN_VERSION.compareTo(wrapperVersion)) {
                csvRow = csvRow
                        .withRepoSkip("TRUE")
                        .withSkipReason("Maven wrapper " + wrapperVersion + " is not supported");
            } else if (wrapper.equalsIgnoreCase("Gradlew") && 0 < "5".compareTo(wrapperVersion) && !wrapperVersion.startsWith(MINIMUM_GRADLE_VERSION)) {
                csvRow = csvRow
                        .withRepoSkip("TRUE")
                        .withSkipReason("Gradle wrapper " + wrapperVersion + " is not supported");
            }
        }
        return csvRow;
    }

    private static Integer javaVersion(String requiredJavaVersion) {
        if (requiredJavaVersion == null || requiredJavaVersion.isBlank()) {
            return null;
        }
        int javaVersion = Integer.parseInt(requiredJavaVersion);
        if (javaVersion <= 8) {
            return javaVersion;
        } else if (javaVersion <= 11) {
            return 11;
        } else if (javaVersion <= 17) {
            return 17;
        } else {
            return 20;
        }
    }
}

record Key(String origin, String path, String branch) implements Comparable<Key> {
    @Override
    public int compareTo(Key o) {
        return Comparator.comparing(Key::origin)
                .thenComparing(Key::path)
                .thenComparing(Key::branch)
                .compare(this, o);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key key = (Key) o;
        return Objects.equals(origin, key.origin) &&
               Objects.equals(path.toLowerCase(), key.path.toLowerCase()) &&
               Objects.equals(branch, key.branch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, path.toLowerCase(), branch);
    }
}

record DataTableRow(String repositoryOrigin, String repositoryPath, String repositoryBranch, String type,
                    String version, String command, String exitCode, String requiredJavaVersion, String logOutput) {
}

@lombok.With
record CsvRow(String scmHost, String repoName, String repoBranch, String mavenTool, String gradleTool, String jdkTool,
              String repoStyle, String repoBuildAction, String repoSkip, String skipReason) {
    @Override
    public String toString() {
        return String.join(",", scmHost, repoName, repoBranch, mavenTool, gradleTool, jdkTool, repoStyle, repoBuildAction, repoSkip, skipReason);
    }
}


