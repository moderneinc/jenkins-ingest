# Parser

Updates `repos.csv` based on a `org.openrewrite.FindBuildToolFailures` Data table CSV export.

## Usage

1. Run https://public.moderne.io/recipes/org.openrewrite.FindBuildToolFailures on a set of repositories, _without output log_.
2. Download the Data table `Build tool failures` CSV.
3. Run the parser:
```shell
./gradlew build && java -jar build/libs/parser-1.0-SNAPSHOT.jar ../repos.csv  ~/Downloads/FRfzi.csv
```
4. Commit the changed `repos.csv`.
5. Trigger the [Moderne-CLI workflow](https://github.com/moderneinc/moderne-cli/actions/workflows/jenkins.yml) to update Jenkins jobs.
