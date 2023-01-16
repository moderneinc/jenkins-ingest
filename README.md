## Purpose

On a nightly basis, [Moderne](https://public.moderne.io) receives updated lossless semantic tree (LST) representations of a large body of OSS software from this process. Each repository that is subject to nightly ingest is listed in `repos.csv`, along with calculated information about its build tooling and language level requirements.

This same process can be used with a few tweaks in the enterprise to mass produce LSTs for ingestion into a private Moderne tenant, as described below.

## How to add repositories

Run the following Kotlin script to take an input `csv` file. The result will be additional lines
added to `repos.csv` in the root of this project, which serves as the source of repositories that the
seed job will manage jobs for.

`./add-repos.main.kts csv-file`

The csv-file argument is expected to be a valid `csv` file, with optional header:

`repoName, branch, label, style, buildTool`

| Column | Required | Notes |
|----|----|----|
|repoName | Required | Github repository with form `organization/name`, i.e. `google/guava`. |
|branch | Optional | Github branch name to ingest. |
|label | Optional | Jenkins worker node label. Current supported values: {`java8`, `java11`}. Defaults to `java8`. |
|style | Optional | OpenRewrite style name to apply during ingest. |
|buildTool | Optional | Auto-detected if omitted. Current supported value: {`gradle`, `gradlew`, `maven`}. |

## NOTE: `init.gradle` changes
The `init.gradle` file in this repository is imported into Jenkins. Any changes made to the file directly in Jenkins will be overwritten on each run of the seed job.
