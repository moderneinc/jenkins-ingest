package io.moderne.jenkins.ingest;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Merger {

    public static void main(String[] args) throws IOException {
        mergeDatatables(Path.of(args[0]), Path.of(args[1]));
    }

    public static void mergeDatatables(Path original, Path newCsv) throws IOException {
        Map<Key, CsvRow> newRows = parseNewCsv(newCsv);
        updateReposCsv(original, newRows);
    }

    private static Map<Key, CsvRow> parseNewCsv(Path reposFile) throws IOException {
        // header: scmHost,repoName,repoBranch,mavenTool,gradleTool,jdkTool,repoStyle,repoBuildAction,repoSkip,skipReason
        List<String> reposLines = Files.readAllLines(reposFile);
        return reposLines.stream()
                .map(line -> line.split(",", -1))
                .map(split -> new CsvRow(split[0].equals("github.com") ? "" : split[0],
                        split[1], split[2], split[3], split[4], split[5], split[6], split[7], split[8], split[9]))
                .collect(Collectors.toMap(row -> new Key(row.scmHost(), row.repoName(), row.repoBranch()), Function.identity()));
    }

    private static void updateReposCsv(Path reposFile, Map<Key, CsvRow> newRowsByKey) throws IOException {
        // header: scmHost,repoName,repoBranch,mavenTool,gradleTool,jdkTool,repoStyle,repoBuildAction,repoSkip,skipReason
        List<String> reposLines = Files.readAllLines(reposFile);
        try (FileWriter writer = new FileWriter(reposFile.toFile())) {
            // Loop over repoLines and update them based matching data table rows
            for (String line : reposLines) {
                String[] split = line.split(",", -1);
                CsvRow csvRow = new CsvRow(split[0], split[1], split[2], split[3], split[4], split[5], split[6], split[7], split[8], split[9]);
                CsvRow newRow = newRowsByKey.get(new Key(csvRow.scmHost(), csvRow.repoName(), csvRow.repoBranch()));
                if (newRow != null) {
                    csvRow = updateCsvRow(csvRow, newRow);
                }
                writer.write(csvRow.toString() + "\n");
            }
        }
    }

    private static CsvRow updateCsvRow(CsvRow csvRow, CsvRow newRow) {
        CsvRow mergedRow = csvRow;
        if (!newRow.mavenTool().isBlank()) {
            mergedRow = mergedRow.withMavenTool(newRow.mavenTool());
        }
        if (!newRow.gradleTool().isBlank()) {
            mergedRow = mergedRow.withGradleTool(newRow.gradleTool());
        }
        if (!newRow.jdkTool().isBlank()) {
            mergedRow = mergedRow.withJdkTool(newRow.jdkTool());
        }
        if (!newRow.repoStyle().isBlank()) {
            mergedRow = mergedRow.withRepoStyle(newRow.repoStyle());
        }
        if (!newRow.repoBuildAction().isBlank()) {
            mergedRow = mergedRow.withRepoBuildAction(newRow.repoBuildAction());
        }
        if (!newRow.repoSkip().isBlank()) {
            mergedRow = mergedRow.withRepoSkip(newRow.repoSkip());
        }
        if (!newRow.skipReason().isBlank()) {
            mergedRow = mergedRow.withSkipReason(newRow.skipReason());
        }
        return mergedRow;
    }

}


