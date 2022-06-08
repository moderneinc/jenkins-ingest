## How to add repositories

`./add-repos.main.kts csv-file`

The csv-file argument is expected to be a valid csv file, with optional header:

`repoName, branch, label, style, buildTool`

- repoName: Required. Github repository with form 'organization/name', i.e. google/guava. 
- branch: Optional. Github branch name to ingest.
- label: Optional. Jenkins worker node label. Current supported values: {java8, java11}. Defaults to java11.
- style: Optional. OpenRewrite style name to apply during ingest.
- buildTool: Optional. Auto-detected if omitted. Current supported value: {gradle, gradlew, maven}.

## init.gradle changes
The `init.gradle` file in Jenkins is not under source control. Be careful making changes and be sure to update the init.gradle after making changes.