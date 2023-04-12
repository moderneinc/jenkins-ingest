#!/bin/bash
set -ex

gh repo list $1 --language java --no-archived --source --limit 1000 --json nameWithOwner,defaultBranchRef --template '{{range .}},{{.nameWithOwner}},{{.defaultBranchRef.name}},,,java17,,,,{{"\n"}}{{end}}' | sort > new.csv
# Save the header
head -n 1 repos.csv > header.csv
# Copy the data without the header
tail -n +2 repos.csv > old.csv
mv header.csv repos.csv
# Join the old and new data, deduplicate, and sort
# join merges the data together based upon the columns scmHost,repoName,repoBranch preserving duplicates from the new file
# awk replaces the empty jdkTool with java17, which is the default, and was stripped by join from the new file
join -t, -a2 -j1 -o2.2,2.3,2.4,1.5,1.6,1.7,1.8,1.9,1.10,1.11 <(<old.csv awk -F, '{print $1"-"$2"-"$3","$0}' | sort -k1,1 ) <(<new.csv awk -F, '{print $1"-"$2"-"$3","$0}' | sort -k1,1 ) | awk -F ',' -v OFS=',' '$6 == "" { $6 = "java17" }1' | LC_COLLATE=C sort | uniq >> new_deduplicated.csv
# Join the old and new deduplicate data, and sort
cat old.csv new_deduplicated.csv | LC_COLLATE=C sort | uniq >> repos.csv
rm old.csv new.csv new_deduplicated.csv
