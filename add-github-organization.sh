#!/bin/bash
set -ex

gh repo list $1 --language java --no-archived --source --visibility public --limit 1000 --json nameWithOwner,defaultBranchRef --template "{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,,{{\"\n\"}}{{end}}" > new.csv
gh repo list $1 --language python --no-archived --source --visibility public --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,,{{"\n"}}{{end}}' >> new.csv

# Archive repos
gh repo list $1 --language java --archived --source --visibility public --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,TRUE,archived{{"\n"}}{{end}}' >> new.csv
gh repo list $1 --language groovy --archived --source --visibility public --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,TRUE,archived{{"\n"}}{{end}}' >> new.csv
gh repo list $1 --language kotlin --archived --source --visibility public --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,TRUE,archived{{"\n"}}{{end}}' >> new.csv
gh repo list $1 --language python --archived --source --visibility public --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,TRUE,archived{{"\n"}}{{end}}' >> new.csv

# Merge new repos into existing repos
./gradlew build && java -cp build/libs/jenkins-ingest-1.0-SNAPSHOT.jar io.moderne.jenkins.ingest.Merger repos.csv  new.csv
rm new.csv

# Quick analysis of largest organizations
cut --delimiter='/' --fields=1 repos.csv | sort | uniq -c | sort -h | tail -n 20
