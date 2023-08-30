package io.moderne.jenkins.ingest;

@lombok.With
record CsvRow(String scmHost, String repoName, String repoBranch, String repoStyle, String repoBuildAction,
              String repoSkip, String skipReason) {
    @Override
    public String toString() {
        return String.join(",", scmHost, repoName, repoBranch, repoStyle, repoBuildAction, repoSkip, skipReason);
    }
}
