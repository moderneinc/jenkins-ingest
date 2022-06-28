#!/usr/bin/env bash

while read line
do
  if [[ "$line" != \#* ]]
  then
    export "moderne_$line"
  fi
done < target/scm.properties