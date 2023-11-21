package net.sigmalab.gitlabmigrator.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sigmalab.gitlabmigrator.gitlab.GitlabClient;
import net.sigmalab.gitlabmigrator.model.GroupSetting;
import net.sigmalab.gitlabmigrator.model.MigratorDb;
import net.sigmalab.gitlabmigrator.utils.JsonMapper;
import org.gitlab4j.api.models.Group;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@CommandLine.Command(name = "init", description = "Initialises migrator DB with GitLab and GitHub data.")
@Slf4j
public class InitCommand implements Callable<Integer> {

    public static final ObjectMapper OBJ_MAPPER = JsonMapper.objectMapper();
    public static final int ERROR_MIGRATOR_DB_EXISTING = -1;
    @CommandLine.Option(names = "--group-id-or-path",
            description = "The group id or path to group",
            required = true)
    private String groupIdOrPath;

    @CommandLine.Option(names = "--gitlab-access-token",
            description = "GitLab access token with access to the groups",
            required = true)
    private String gitlabAccessToken;

    @CommandLine.Option(names = {"--github-organization", "--github-organisation"},
            description = "GitHub organisation to mirror the repos into.\n" +
                    "It's the text part of the organisation in it's url",
            required = true)
    private String githubOrganization;

    @CommandLine.Option(names = "--github-access-token",
            description = "GitHub access token with access to the groups",
            required = true)
    private String githubAccessToken;

    @CommandLine.Option(names = "--db-file",
            description = "The migrator database file.",
            defaultValue = "migrator-db.json")
    private File migratorDbFile;

    @CommandLine.Option(names = {"--silent", "-s"},
            description = "If an existing db file is found it will be overwritten.")
    private boolean isSilent;

    @Override
    public Integer call() throws Exception {

        if (!isSilent && migratorDbFile.exists()) {
            System.out.println("Migrator DB file existing and not in silent mode. Exiting.");
            return ERROR_MIGRATOR_DB_EXISTING;
        }
        // TODO validate inputs

        // load gitlab
        var migratorDb = scanGitlab();

        saveDb(migratorDb);


        return 0;
    }

    private void saveDb(MigratorDb migratorDb) throws IOException {
        OBJ_MAPPER.writer().writeValue(migratorDbFile, migratorDb);
    }

    private MigratorDb scanGitlab() {
        var gitlabClient = new GitlabClient(gitlabAccessToken, log.isDebugEnabled());

        var groupWorkset = gitlabClient.getSubgroups(groupIdOrPath);

        var gitlabGroups = groupWorkset.stream()
                .collect(Collectors.toMap(Group::getId, identity()));

        gitlabGroups.forEach((id, group) -> group.setProjects(gitlabClient.getProjectsForGroup(group)));

        var gitlabGroupsMap = groupWorkset.stream().collect(Collectors.toMap(Group::getId, identity()));

        var groupSettings = gitlabGroupsMap.values().stream().collect(Collectors.toMap(Group::getId, g -> new GroupSetting(true, null, null)));

        return new MigratorDb(gitlabGroupsMap, groupSettings, null);
    }

}
