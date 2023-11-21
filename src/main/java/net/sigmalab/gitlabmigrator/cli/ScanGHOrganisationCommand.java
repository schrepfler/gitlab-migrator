package net.sigmalab.gitlabmigrator.cli;

import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHTeam;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "scan-organisation", description = "Scans GitHub organisation and prepares a task list.")
@Slf4j
public class ScanGHOrganisationCommand implements Callable<Integer> {

    GitHub github;

    @CommandLine.Option(names = {"--target-directory"},
            description = "The target directory to use as task staging area for the GitHub organisation.",
            required = true,
            defaultValue = "temp-")
    private File targetDir;

    @CommandLine.Option(names = "--mapping-file",
            description = "File mapping repository names between origin and target repositories.\n" +
                    "If not existing, migrator will create one on first run and abort processing for you to validate",
            required = true,
            defaultValue = "organisation-mappings.json")
    private File mappingFile;

    @CommandLine.Option(names = "--github-personal-access-token",
            description = "GitHub personal access token with access to the groups",
            required = true)
    private String githubAppPersonalAccessToken;

    @CommandLine.Option(names = {"--github-organization", "--github-organisation"},
            description = "GitHub organisation to mirror the repos into.\n" +
                    "It's the text part of the organisation in it's url",
            required = true)
    private String githubOrganization;

    @Override
    public Integer call() throws Exception {

        System.out.println("targetDir = " + targetDir);
        System.out.println("mappingFile = " + mappingFile);
        System.out.println("githubAppPersonalAccessToken = " + githubAppPersonalAccessToken);
        System.out.println("githubOrganization = " + githubOrganization);

        github = new GitHubBuilder()
                .withOAuthToken(githubAppPersonalAccessToken)
                .build();

        var organisation = github.getOrganization(githubOrganization);

        var repositories = organisation.getRepositories();
        var teams = organisation.getTeams();


        System.out.println("organisation = " + organisation);
        System.out.println("repositories = " + repositories);
        System.out.println("teams = " + teams);

        for (GHTeam team : teams.values()) {
            System.out.println("team = " + team);
            team.listMembers().toList().forEach(m -> System.out.println("m = " + m));
        }

        return 0;
    }
}
