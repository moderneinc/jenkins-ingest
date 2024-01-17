#!/bin/bash
set -ex

# This script is exclusively developed for parser development purposes. It serves the specific use case of ingesting GitHub repositories internally for SAAS.

# Create out folder if not exist
mkdir -p out

# Search and add github repos by language, change the below language

# generate `gh.json` which includes repo fullName and branch name,
# Manually customize the language and adjust the count below according to your specific requirements.
# option to add filter: --updated="> yyyy-mm-dd"
gh search repos --visibility public --limit 500 --topic $1 --sort stars --json fullName,defaultBranch > out/$1-raw.json

# generate `new.csv`, which will be merged to `{query}-orgs.csv`
jq -r '.[] | ",\(.fullName),\(.defaultBranch),,,,,,,"' out/$1-raw.json > out/new.csv
jq '[.[] | {origin: "github.com", path: .fullName, branch: .defaultBranch}]' out/$1-raw.json > out/$1-orgs.json

# Merge `new.csv` to `repos.csv`
./gradlew build && java -cp build/libs/jenkins-ingest-1.0-SNAPSHOT.jar io.moderne.jenkins.ingest.Merger repos.csv  out/new.csv

