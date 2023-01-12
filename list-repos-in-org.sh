#!/usr/bin/env bash

if ! command -v gh &> /dev/null
then
    echo "gh could not be found. Please install it!"
    exit
fi

# getopts for organization
while getopts ":o:" opt; do
  case $opt in
    o)
      organization=${OPTARG}
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

if [ -z "$organization" ]; then
  echo "No input organization specified."
  exit 1
fi

all_repos=""

languages=(
    java
    kotlin
    groovy
)
for l in "${languages[@]}"; do
  repos=`gh repo list --no-archived ${organization} --limit 9999999 --language $l --json nameWithOwner,defaultBranchRef --jq 'map(.nameWithOwner + "," + .defaultBranchRef.name + ",,,,,,")' | jq -c -r '.[]'`
  all_repos=$all_repos$'\n'$repos
done

all_repos=`echo "$all_repos" | sort -u | uniq -u`
echo "$all_repos"
