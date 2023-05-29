#!/bin/bash
set -ex

gh repo list $1 --language java --no-archived --source --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,java17,,,,{{"\n"}}{{end}}' | sort > new.csv
gh repo list $1 --language python --no-archived --source --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,,{{"\n"}}{{end}}' | sort >> new.csv
gh repo list $1 --language java --archived --source --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,TRUE,archived{{"\n"}}{{end}}' | sort >> new.csv
gh repo list $1 --language python --archived --source --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,TRUE,archived{{"\n"}}{{end}}' | sort >> new.csv

cd parser/
./gradlew build && java -cp build/libs/parser-1.0-SNAPSHOT.jar io.moderne.jenkins.ingest.Parser ../repos.csv  ../new.csv
rm ../new.csv
