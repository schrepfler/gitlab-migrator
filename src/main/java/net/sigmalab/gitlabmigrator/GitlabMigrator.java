package net.sigmalab.gitlabmigrator;

import lombok.extern.slf4j.Slf4j;
import net.sigmalab.gitlabmigrator.cli.*;
import picocli.CommandLine;

/**
 * GitLab Migrator
 */
@Slf4j
@CommandLine.Command(
        name = "gitlab-migrator",
        subcommands = {
                InitCommand.class,
                ExportCommand.class,
                MirrorReposCommand.class,
        },
        description = "gitlab-migrator is a set of tools and small code experiments aimed at facilitating a migration of GitLab organisation to GitHub.")
public class GitlabMigrator {

    public static void main(String[] args) {

        int exitCode = new CommandLine(new GitlabMigrator())
                .setUnmatchedArgumentsAllowed(true)
                .execute(args);
        System.exit(exitCode);

    }

}
