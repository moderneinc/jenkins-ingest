#!/bin/bash
set -ex

# This script is exclusively developed for parser development purposes. It serves the specific use case of ingesting GitHub repositories internally for SAAS.

# Create out folder if not exist
mkdir -p out

# Search and add github repos by language, change the below language

# generate `gh.json` which includes repo fullName and branch name,
# Manually customize the language and adjust the count below according to your specific requirements.
# option to add filter: --updated="> 2023-11-21"
gh search repos --language javascript --visibility public --limit 1000 --json fullName,defaultBranch > out/gh.json

# generate `new.csv`, which will be merged to `repos.csv`
cat out/gh.json | jq -r '.[] | ",\(.fullName),\(.defaultBranch),,,,,,,"' > out/new.csv

# generate `repos.json`, which will be used to update `ownership.json`
cat out/gh.json | jq -r '.[] | "      {
        \"origin\": \"github.com\",
        \"path\": \"" + .fullName + "\",
        \"branch\": \"" + .defaultBranch + "\"
      },"' | sed '$s/,$//' > out/repos-content.json

# Merge `new.csv` to `repos.csv`
cd parser/
./gradlew build && java -cp build/libs/parser-1.0-SNAPSHOT.jar io.moderne.jenkins.ingest.Merger ../repos.csv  ../out/new.csv

# Quick analysis of largest organizations
cut --delimiter='/' --fields=1 repos.csv | sort | uniq -c | sort -h | tail -n 20
