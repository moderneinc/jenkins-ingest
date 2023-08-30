package io.moderne.jenkins.ingest;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Merger {

    public static void main(String[] args) throws IOException {
        mergeDatatables(Path.of(args[0]), Path.of(args[1]));
    }

    public static void mergeDatatables(Path original, Path newCsv) throws IOException {
        // header: scmHost,repoName,repoBranch,mavenTool,gradleTool,jdkTool,repoStyle,repoBuildAction,repoSkip,skipReason
        Map<Key, CsvRow> oldRows = parseCsv(original, 1);
        Map<Key, CsvRow> newRows = parseCsv(newCsv, 0);
        Collection<CsvRow> mergedRows = mergeRows(oldRows, newRows);
        // Remove master branch rows if main branch rows exist
        mergedRows.removeIf(row -> "master".equals(row.repoBranch())
                && oldRows.containsKey(new Key(row.scmHost(), row.repoName(), "main")));
        writeCsv(original, mergedRows);
    }

    private static Map<Key, CsvRow> parseCsv(Path reposFile, int skip) throws IOException {
        try (Stream<String> lines = Files.lines(reposFile)) {
            return lines
                    .skip(skip)
                    .map(line -> line.split(",", -1))
                    .peek(split -> {
                        if (split.length != 10) {
                            throw new RuntimeException("Invalid CSV line: " + String.join(",", split));
                        }
                    })
                    .map(split -> new CsvRow(split[0].equals("github.com") ? "" : split[0],
                            split[1], split[2], split[3], split[4], split[5], split[6], split[7], split[8], split[9]))
                    .collect(Collectors.toMap(
                            row -> new Key(row.scmHost(), row.repoName(), row.repoBranch()),
                            Function.identity(),
                            Merger::updateCsvRow));
        }
    }

    private static Collection<CsvRow> mergeRows(Map<Key, CsvRow> oldRowsByKey, Map<Key, CsvRow> newRowsByKey) {
        // Loop over existing rows and update them based on new data table rows
        Map<Key, CsvRow> mergedRows = new TreeMap<>();
        for (Map.Entry<Key, CsvRow> entry : oldRowsByKey.entrySet()) {
            CsvRow newRow = newRowsByKey.get(entry.getKey());
            if (newRow != null) {
                // Update existing row
                mergedRows.put(entry.getKey(), updateCsvRow(entry.getValue(), newRow));
                newRowsByKey.remove(entry.getKey());
            } else {
                // Retain existing row
                mergedRows.put(entry.getKey(), entry.getValue());
            }
        }
        // Add new rows
        mergedRows.putAll(newRowsByKey);
        return mergedRows.values();
    }

    private static void writeCsv(Path reposFile, Collection<CsvRow> rows) throws IOException {
        try (FileWriter writer = new FileWriter(reposFile.toFile())) {
            writer.write("scmHost,repoName,repoBranch,mavenTool,gradleTool,jdkTool,repoStyle,repoBuildAction,repoSkip,skipReason\n");
            for (CsvRow row : rows) {
                writer.write(row.toString() + "\n");
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
        if (!newRow.jdkTool().isBlank() && csvRow.jdkTool().isBlank() || "java".equals(newRow.jdkTool())) {
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


