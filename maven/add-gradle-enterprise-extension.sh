#!/bin/bash

MVN_DIR=".mvn"
EXT_FILE="extensions.xml"
MVN_EXT_XML="<extension><groupId>com.gradle</groupId><artifactId>gradle-enterprise-maven-extension</artifactId><version>1.14.2</version></extension>"

if [ ! -d "$MVN_DIR" ]; then
  echo "Creating $MVN_DIR directory."
  mkdir "$MVN_DIR"
fi

if [ ! -f "$MVN_DIR/$EXT_FILE" ]; then
  echo "Creating $MVN_DIR/$EXT_FILE"
  touch "$MVN_DIR/$EXT_FILE"
  echo "<extensions>" >> "$MVN_DIR/$EXT_FILE"
  echo "</extensions>" >> "$MVN_DIR/$EXT_FILE"
  ADD=$(echo $MVN_EXT_XML | sed 's/\//\\\//g')
  sed -i "/<\/extensions>/ s/.*/${ADD}\n&/" "$MVN_DIR/$EXT_FILE"
else
  echo "File exists, creating copy and adding extension."
  # Assumes the extension does not exist in the file already.
  ADD=$(echo $MVN_EXT_XML | sed 's/\//\\\//g')
  sed -i "/<\/extensions>/ s/.*/${ADD}\n&/" "$MVN_DIR/$EXT_FILE"
fi