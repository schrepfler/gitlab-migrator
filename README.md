# GitLab Migrator

GitLab Migrator is a Java application which can be used primarily to map and migrate GitLab groups into a GitHub organisation.
You can also use it to keep GitLab repositories synced up locally and eventuall it'll be able to keep your GitLab and GitHub in sync.

### TODO badges

## Installation

TODO Needs implementing CI/CD for binaries and publish.

```bash
...
```

## Usage

Gitlab Migrator consists of the following commands

### Init

This command will fetch the GitLab group, subgroups and repository information locally, will scan the GitHub organisation and then save this information.

```shell
gitlab-migrator init  --group-id-or-path=<groupIdOrPath>
                      --gitlab-access-token=<gitlabAccessToken>
                      --github-organization=<githubOrganization>
                      --github-personal-access-token=<githubAppInstalationToken>
                      [--migrator-db-file=<migratorDbFile>]
```

### Export

This command will clone the gitlab repositories locally.

```shell
gitlab-migrator export  --target-directory=<targetDirectory>
                        --group-id-or-path=<groupIdOrPath>
                        --gitlab-access-token=<gitlabAccessToken>
                        --expanded=<expanded|false>
                        --enable-gitlab-logging=<enableGitLabLogging|false>
```

### WIP Map Organisation

### WIP Migrate

```shell
 
gitlab-migrator migrate --github-organization=<githubOrganization>
                        --github-personal-access-token=<githubAppInstalationToken>
                        --gitlab-personal-access-token=<gitlabPersonalAccessToken>
                        --group-id-or-path=<groupIdOrPath>
                        [--mapping-file=<mappingFile>]
                        [--target-directory=<targetDir>]
                        [--delete-organization-projects]
```

When executing the tool you'll go through two phases. 
- In the first go, the tool will create a file (default value `repo-mappings.json`)
which will be populated with a map of all repositories in the origin GitLab group at which point it will stop.
You can now modify that map and customise the title of the target repository as GitHub does not have support for groups 
and sub-groups so this is a good moment to establish a naming convention.
- In the second run, the tool should start cloning locally the repositories in the `target-directory` location and once
  finished it should start creating repositories in the target GitHub organisation and pushing the local repos 
  (including branches and tags) into them.
  - In case of errors during processing the `mirrorStatus` will move from `TODO` to `ERROR` or in case of hitting the
  GitHub API rate limit `RATE_LIMIT_HIT_DELAYED`. Once you understand the error or the rate limit has expired you can  
  move the `mirrorStatus` back in `TODO` and the application will retry the repositories which are in `TODO` state. 
  In that case it will skip the `PUSHED` or errored repositories and will also refresh the list in the case there are
  newly added repositories in the GitLab group.
- There's a nuclear option to nuke all GitHub organisation repositories if for some reason you want to refresh the
repositories as we don't support sync between remotes. Contributions are welcome. Use at own responsibility!

```json
{
  "repoMappings" : {
    "123456788" : {
      "id" : 123456788,
      "origin" : "top-group/sub-group/fancy-app",
      "title" : "TOPGROUP SUBGROUP fancy-app",
      "mirrorStatus" : "TODO"
    },
    "123456789" : {
      "id": 123456789,
      ...
    }
...
```

## Build

Creating the release files locally can be performed using
```shell
mvn clean install jreleaser:assemble
```


## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
[Apache-2.0](https://spdx.org/licenses/Apache-2.0.html)

## Roadmap
- Add and Validate support for hosted GitLab CE
- Track upstream repos and push updates.
- Handle adding users to organisation.
- Handle existing target repo.
- Not sure if description is migrated?
- Support multiple commands.
- Make it more robust.
