#!/bin/bash
set -ex

java='java'
if [ -n "$2" ]; then
  java=$2
fi

gh repo list $1 --language java --no-archived --source --limit 1000 --json nameWithOwner,defaultBranchRef --template "{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,${java},,,,{{\"\n\"}}{{end}}" > new.csv
gh repo list $1 --language python --no-archived --source --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,,{{"\n"}}{{end}}' >> new.csv
gh repo list $1 --language java --archived --source --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,TRUE,archived{{"\n"}}{{end}}' >> new.csv
gh repo list $1 --language python --archived --source --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,,,,TRUE,archived{{"\n"}}{{end}}' >> new.csv

cd parser/
./gradlew build && java -cp build/libs/parser-1.0-SNAPSHOT.jar io.moderne.jenkins.ingest.Merger ../repos.csv  ../new.csv
rm ../new.csv
