#!/bin/bash
set -ex

echo -n > new.csv

cd parser/
./gradlew build && java -cp build/libs/parser-1.0-SNAPSHOT.jar io.moderne.jenkins.ingest.Merger ../repos.csv  ../new.csv
rm ../new.csv
