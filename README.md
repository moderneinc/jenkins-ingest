# Jenkins ingest

https://app.moderne.io allows users to run recipes against thousands of Open Source Software projects.
This repository contains a comma separated value (CSV) file of repositories to ingest into Moderne on a daily basis.

## How to add/update the Java OSS repositories of new/existing GH org/user

Open [this workflow](https://github.com/moderneinc/jenkins-ingest/actions/workflows/addGitHubOrganization.yaml) 
and introduce the GitHub username or organization that you would like to add in our Moderne public tenant.  

If you are using a fork of this repository, you need to create a classic Personal Access Token (PAT) (fine-grained tokens 
are invalid) with `repo` scope. The secret needs to be called `PAT`. 

![Workflow configuration](/assets/images/workflow.png "workflow configuration")

This will create a pull request with all the Java repositories entries. Feel free to change 
the entries you want to remove or customize. 

![Pull request](/assets/images/auto-pull-request.png "Pull request")

After the pull request is merged, a new process to update our Jenkins instance will be triggered.

See the next section to understand the columns and accepted values.

## repos.csv file format
The CSV file can use an optional header row.
```csv
scmHost,repoName,repoBranch,repoStyle,repoBuildAction,repoSkip,skipReason
```

The columns are defined as follows:

| Column          | Required | Notes                                                                                             |
|-----------------|----------|---------------------------------------------------------------------------------------------------|
| scmHost         | Optional | Repository host such as `github.com`, `gitlab.com` or enterprise hosts. Defaults to `github.com`. |
| repoName        | Required | Repository path with form `organization/name`, i.e. `google/guava`.                               |
| repoBranch      | Optional | Git branch name to ingest. Defaults to `main`.                                                    |
| repoStyle       | Optional | Name of the OpenRewrite style to apply during ingest. Defaults to empty.                          |
| repoBuildAction | Optional | Additional build tool tasks/targets to execute before ingestion goal. Defaults to empty.          |
| repoSkip        | Optional | Use 'TRUE' to omit ingest job creation for the CSV row. Defaults to empty.                        |
| skipReason      | Optional | Reason a job is set to skip.                                                                      |

## Creating Jenkins jobs
The Jenkins ingestion jobs are created from `repos.csv` by running the moderne-cli command `moderne-cli connect jenkins`
either on a local machine, or through GitHub Actions on the `moderne-cli` repository.

## Upload `maven/ingest-settings.xml`
Ingestion can optionally use a Maven settings file to configure authentication for private repositories.
An example of which is given in `maven/ingest-settings.xml`.
This file should be configured in Jenkins master through `Manage Jenkins > Global Tool Configuration > Maven Configuration`.

## Adding repositories from your local machine
To add a repository to the ingestion process, add a row to `repos.csv` with the repository name and branch.

```shell
$ ./add-github-organization.sh openrewrite
```

Then trigger a manual run of https://github.com/moderneinc/moderne-cli/actions/workflows/jenkins.yml to create the Jenkins jobs, potentially with a prefix filter.
After the jobs have run once, you might want to update or skip failed jobs through [the parser](./parser/README.md).
