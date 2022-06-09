## How to add repositories

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

## `init.gradle` changes
The `init.gradle` file in Jenkins is now under source control. Be careful making changes and be sure to update the `init.gradle` after making changes.
