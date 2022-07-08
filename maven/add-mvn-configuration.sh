#!/bin/bash

MVN_DIR=".mvn"
EXT_FILE="extensions.xml"
MVN_EXT_XML="<extension><groupId>com.gradle</groupId><artifactId>gradle-enterprise-maven-extension</artifactId><version>1.14.2</version></extension>"
JVM_CONFIG="jvm.config"
MVN_MEM="Xmx2048m"
# Create the .mvn directory if it does not exist.
if [ ! -d "$MVN_DIR" ]; then
    mkdir "$MVN_DIR"
fi
# Create the extensions.xml file if it does not exist.
if [ ! -f "$MVN_DIR/$EXT_FILE" ]; then
    touch "$MVN_DIR/$EXT_FILE"
    # Add the `<extensions>` tag.
    echo "<extensions>" >> "$MVN_DIR/$EXT_FILE"
    echo "</extensions>" >> "$MVN_DIR/$EXT_FILE"
    ADD=$(echo $MVN_EXT_XML | sed "s/\\//\\\\\\//g")
    sed -i "/<\\/extensions>/ s/.*/${ADD}\\n&/" "$MVN_DIR/$EXT_FILE"
else
    # Assumes the <extensions> already exists and the <extension> tag does exist.
    ADD=$(echo $MVN_EXT_XML | sed "s/\\//\\\\\\//g")
    sed -i "/<\\/extensions>/ s/.*/${ADD}\\n&/" "$MVN_DIR/$EXT_FILE"
fi

# Create the jvm.config file if it does not exist.$
if [ ! -f "$MVN_DIR/$JVM_CONFIG" ]; then
  touch "$MVN_DIR/$JVM_CONFIG"
  echo "-$MVN_MEM" >> "$MVN_DIR/$JVM_CONFIG"
else
  RESULT="$(grep -E Xmx[0-9]+.* $MVN_DIR/$JVM_CONFIG)"
  if [ -z "$RESULT" ]; then
    echo " -$MVN_MEM" >> "$MVN_DIR/$JVM_CONFIG"
  else
    sed -i "s/Xmx[0-9]*[MmGg]/$MVN_MEM/" "$MVN_DIR/$JVM_CONFIG"
  fi
fi