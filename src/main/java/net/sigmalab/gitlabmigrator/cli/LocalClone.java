package net.sigmalab.gitlabmigrator.cli;

import net.sigmalab.gitlabmigrator.gitlab.GitlabClient;
import net.sigmalab.gitlabmigrator.ssh.Ssh;
import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.git.transport.GitSshdSessionFactory;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Project;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "local-clone", description = "Clones gitlab repositories locally.")
public class LocalClone implements Callable<Integer> {

    @Option(names = {"--target-directory"},
            description = "The target directory to which to clone the repositories.",
            required = true)
    private File targetDir;

    @Option(names = "--gitlab-personal-access-token",
            description = "GitLab personal access token with access to the groups",
            required = true)
    private String gitlabPersonalAccessToken;

    @Option(names = "--group-id-or-path",
            description = "The group id or path to group",
            required = true,
            converter = GroupIdOrPathConverter.class)
    private Object groupIdOrPath;

    @Override
    public Integer call() throws Exception {

        var client = ClientBuilder.builder().build();

        GitSshdSessionFactory sshdSessionFactory = new GitSshdSessionFactory(client);
        TransportConfigCallback transportConfigCallback = new Ssh.SshTransportConfigCallback();

        try {
            client.start();
            SshSessionFactory.setInstance(sshdSessionFactory);
        } finally {
            client.stop();
        }

        try (GitLabApi gitLabApi = new GitLabApi("https://gitlab.com", gitlabPersonalAccessToken)) {
            gitLabApi.enableRequestResponseLogging();

            var groupWorkset = GitlabClient.getSubgroups(gitLabApi, groupIdOrPath);
            groupWorkset.add(gitLabApi.getGroupApi().getGroup(groupIdOrPath));

            var projectsWorkset = groupWorkset.stream()
                    .map(group -> GitlabClient.getProjectsForGroup(gitLabApi, group))
                    .flatMap(List::stream)
                    .toList();

            for (Project project : projectsWorkset) {
                System.out.println("Cloning: " + project.getNamespace().getName() + "/" + project.getName());
                GitlabClient.cloneGitlabProject(targetDir.getAbsolutePath(), project, project.getNamespace().getName(), transportConfigCallback);
            }

        }

        return 0;
    }
}
