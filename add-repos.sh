#!/usr/bin/env bash

if [ -z "$1" ] || [ -z "$2" ]; then
    echo "Usage: $1 <path-to-input-file>.csv $2 <path-to-output-file>.csv"
    exit 1
fi

cleanupTmpGitDir() {
    if [ -d "moderne-git" ]; then
        rm -rf "moderne-git"
    fi
}

cleanupTmpGitDir
outFile=$2

while read -r line
do
    IFS=',' read -r -a array <<< "$line"

    if [[ "${array[0]}" =~ "repoName" ]]; then
        continue
    fi

    REPO="${array[0]}"
    BRANCH="${array[1]}"
    LABEL="${array[2]}"
    STYLE="${array[3]}"
    BUILD_TOOL="${array[4]}"

    if [ -z "$LABEL" ];then
        LABEL="java8"
    fi
    
    mkdir -p moderne-git
    cd moderne-git || exit

    if ! git clone --depth 1 --no-checkout "https://github.com/$REPO.git"; then
        echo "Failed to clone $REPO"
        exit 1
    fi

    if [ -z "$BUILD_TOOL" ]; then

        REPO_NAME=$(basename "$REPO")
        cd "$REPO_NAME" || exit
        git sparse-checkout set --no-cone /build.gradle.kts /build.gradle /pom.xml /gradlew
        git checkout

        if [ -f "build.gradle.kts" ] || [ -f "build.gradle" ]; then
            if [ -f "gradlew" ]; then 
                BUILD_TOOL=gradlew
            else
                BUILD_TOOL=gradle
            fi
        elif [ -f "pom.xml" ]; then
            BUILD_TOOL=maven
        fi
    fi

    if [ -z "$BUILD_TOOL" ]; then
        echo "Skipping $REPO, because none of the supported build tool files (build.gradle.kts, build.gradle, or pom.xml) is present at the root."
        continue
    fi

    if [ -z "$BRANCH" ]; then
        BRANCH="$(git rev-parse --abbrev-ref HEAD)"
    fi

    # Clean whitespace
    BRANCH=$(echo "$BRANCH" | sed 's/^[[:blank:]]*//;s/[[:blank:]]*$//')

    cd ../..

    if [ ! -f "$outFile" ]; then
        printf "repoName,branchName,label,style,buildTool\n" >&3
    fi
    printf "%s,%s,%s,%s,%s\n" "$REPO" "$BRANCH" "$LABEL" "$STYLE" "$BUILD_TOOL" >&3

done < "$1" 3>> "$outFile"

cat "$outFile" | awk 'NR<2{print $0;next}{print $0| "sort -u"}' > "$outFile.tmp" && mv "$outFile.tmp" "$outFile"
cleanupTmpGitDir

exit 0