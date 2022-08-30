## Purpose

On a nightly basis, [Moderne](https://public.moderne.io) receives updated abstract syntax tree (AST) representations of a large body of OSS software from this process. Each repository that is subject to nightly ingest is listed in `repos.csv`, along with calculated information about its build tooling and language level requirements.

This same process can be used with a few tweaks in the enterprise to mass produce ASTs for ingestion into a private Moderne tenant, as described below.

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

## Mass ingesting private repositories with this process

1. Click the green "Use this template" button on this page to create a copy of this repository. This is distinct from a fork in that it can be copied into a private organization and does not have an upstream link back to this repository.
2. Change this line in [add-repos.sh](https://github.com/moderneinc/jenkins-ingest/blob/main/add-repos.sh#L76) to point to your version control system.
3. Empty the `repos.csv` file and follow [these instructions](#How-to-add-repositories) to add your own.
4. In `init.gradle`, look for the publish task configuration (approximately [here](https://github.com/moderneinc/jenkins-ingest/blob/main/gradle/init.gradle#L115-L120)) that defines the Maven repository where artifacts will be published. Set this to any artifact repository with a Maven layout (Artifactory or Nexus).
5. Remove Gradle Enterprise:
     * From the seed generator for Maven projects [here](https://github.com/moderneinc/jenkins-ingest/blob/main/seed.groovy#L26-L31).
     * From the `.mvn` configuration [here](https://github.com/moderneinc/jenkins-ingest/blob/main/maven/add-mvn-configuration.sh#L5), [here](https://github.com/moderneinc/jenkins-ingest/blob/main/maven/add-mvn-configuration.sh#L12-L24).
     * From the `init.gradle` [here](https://github.com/moderneinc/jenkins-ingest/blob/main/gradle/init.gradle#L62-L92) and [here](https://github.com/moderneinc/jenkins-ingest/blob/main/gradle/init.gradle#L180-L190).

Optionally: Take note of the mirrors section in `ingest-settings.xml` and consider whether these mirrors will be DNS addressable in your corporate environment. We use these mirrors in the public Moderne tenant to reduce the load of dependency resolution on artifact repositories with rate limit policies (like Apache's). Inside of your environment, you may have a different set of mirrors to accomplish the same.

## NOTE: `init.gradle` changes
The `init.gradle` file in this repository is imported into Jenkins. Any changes made to the file directly in Jenkins will be overwritten on each run of the seed job.
