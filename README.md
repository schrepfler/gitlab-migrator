# GitLab Migrator

GitLab Migrator is a java application which can be used to migrate GitLab groups into an GitHub organisation and keep locally checkout out groups in sync with upstream.
### TODO badges

## Installation

TODO Needs implementing CI/CD for binaries and publish.

```bash
...
```

## Usage

The command can be executed with the following application arguments

```shell
 
gitlab-migrator     --github-organization=<githubOrganization>
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
