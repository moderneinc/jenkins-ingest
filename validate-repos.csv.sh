#!/bin/bash
set -ex

echo -n > new.csv

./gradlew build && java -cp build/libs/jenkins-ingest-1.0-SNAPSHOT.jar io.moderne.jenkins.ingest.Merger repos.csv  new.csv
rm new.csv
