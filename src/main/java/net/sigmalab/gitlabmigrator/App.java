package net.sigmalab.gitlabmigrator;

import lombok.extern.slf4j.Slf4j;
import net.sigmalab.gitlabmigrator.cli.MirrorRepos;
import org.gitlab4j.api.GitLabApiException;
import picocli.CommandLine;

/**
 * GitLab Migrator
 */
@Slf4j
public class App {

    public static void main(String[] args) throws GitLabApiException {

        int exitCode = new CommandLine(
                new MirrorRepos())
                .execute(args);

        System.exit(exitCode);

    }

}
