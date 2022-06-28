#!/usr/bin/env bash

jar xvf target/*-ast.jar scm.properties

while read line
do
  if [[ "$line" != \#* ]]
  then
    export "moderne_$line"
  fi
done < target/scm.properties