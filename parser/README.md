# Parser

Updates `repos.csv` based on a `org.openrewrite.FindBuildToolFailures` Data table CSV export.

## Usage

1. Run https://public.moderne.io/recipes/org.openrewrite.FindBuildToolFailures on a set of repositories.
2. Download the Data table `Build tool failures` CSV.
3. Run the parser

```shell
./gradlew build && java -jar build/libs/parser-1.0-SNAPSHOT.jar ../repos.csv  ~/Downloads/FRfzi.csv
```