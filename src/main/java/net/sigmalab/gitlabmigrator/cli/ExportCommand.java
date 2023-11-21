package net.sigmalab.gitlabmigrator.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sigmalab.gitlabmigrator.gitlab.GitlabClient;
import net.sigmalab.gitlabmigrator.model.MigratorDb;
import net.sigmalab.gitlabmigrator.ssh.Ssh;
import net.sigmalab.gitlabmigrator.utils.JsonMapper;
import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.git.transport.GitSshdSessionFactory;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Project;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Command(name = "export", description = "Clones gitlab repositories locally.")
@Slf4j
public class ExportCommand implements Callable<Integer> {

    public static final ObjectMapper OBJ_MAPPER = JsonMapper.objectMapper();
    @Option(names = "--target-directory",
            description = "The target directory to which to clone the repositories.",
            required = true)
    private File targetDir;

    @Option(names = "--gitlab-access-token",
            description = "GitLab personal access token with access to the groups",
            required = true)
    private String gitlabAccessToken;

    @Option(names = "--group-id-or-path",
            description = "The group id or path to group",
            required = true
    )
    private String groupIdOrPath;

    @Option(names = "--expanded",
            description = "Clone repositories in expanded state (not bare).",
            defaultValue = "false")
    private Boolean expanded;

    @Option(names = "--enable-gitlab-logging",
            description = "Enable GitLab request/response logging",
            defaultValue = "false")
    private Boolean enableGitLabLogging;

    public ExportCommand() {

    }

    @Override
    public Integer call() throws Exception {

        System.out.println("targetDir = " + targetDir);
        System.out.println("expanded = " + expanded);
        System.out.println("enableGitLabLogging = " + enableGitLabLogging);

        MigratorDb migratorDb = loadDB();

        var client = ClientBuilder.builder().build();

        GitSshdSessionFactory sshdSessionFactory = new GitSshdSessionFactory(client);
        TransportConfigCallback transportConfigCallback = new Ssh.SshTransportConfigCallback();

        try {
            client.start();
            SshSessionFactory.setInstance(sshdSessionFactory);
        } finally {
            client.stop();
        }

        var gitlabClient = new GitlabClient(gitlabAccessToken, log.isDebugEnabled());

        var projects = migratorDb.gitlabGroups().values().stream().flatMap(group -> group.getProjects().stream()).toList();

        for (Project project : projects) {
            System.out.print("Cloning: " + project.getNamespace().getName() + "/" + project.getName());
            gitlabClient.cloneGitlabProject(targetDir.getAbsolutePath(), project, project.getNamespace().getName(), transportConfigCallback, !expanded);
            System.out.println("... Done.");
        }

        return 0;
    }

    private MigratorDb loadDB() throws IOException {
        return OBJ_MAPPER.readerFor(MigratorDb.class).readValue(new File("migrator-db.json"));
    }

}
