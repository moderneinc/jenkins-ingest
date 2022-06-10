#!/usr/bin/env bash

# getopts for input, output, tmpdir
while getopts ":i:o:t:" opt; do
  case $opt in
    i)
      input_csv=$OPTARG
      ;;
    o)
      output_csv=${OPTARG}
      ;;
    t)
      tmp_dir=${OPTARG}
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

if [ -z "$input_csv" ]; then
  echo "No input CSV specified."
  exit 1
fi

output_csv=${output_csv:-"repos.csv"}
tmp_dir=${tmp_dir:-"."}
clone_dir=${tmp_dir}/moderne-git
base_dir=$(pwd)

cleanupTmpGitDir() {
    if [ -d "$clone_dir" ]; then
        printf "Removing temporary git directory...\n"
        rm -rf "$clone_dir"
    fi
}

cleanupTmpGitDir

while read -r input_csv
do
    IFS=',' read -r -a array <<< "$input_csv"

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
    
    cleanupTmpGitDir
    mkdir -p "$clone_dir"
    cd "$clone_dir" || exit

    if ! git clone --depth 1 --no-checkout "https://github.com/$REPO.git"; then
        echo "Failed to clone $REPO"
        continue
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

    cd "$base_dir" || exit

    if [ ! -f "$output_csv" ]; then
        printf "repoName,branchName,label,style,buildTool\n" >&3
    fi
    printf "%s,%s,%s,%s,%s\n" "$REPO" "$BRANCH" "$LABEL" "$STYLE" "$BUILD_TOOL" >&3

done < "$input_csv" 3>> "$output_csv"

cd "$base_dir" || exit
cleanupTmpGitDir
touch "$output_csv"
cat "$output_csv" | awk 'NR<2{print $0;next}{print $0| "sort -u"}' > "$output_csv.tmp" && mv "$output_csv.tmp" "$output_csv"
exit 0