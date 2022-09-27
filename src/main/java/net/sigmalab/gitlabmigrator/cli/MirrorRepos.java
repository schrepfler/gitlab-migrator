package net.sigmalab.gitlabmigrator.cli;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sigmalab.gitlabmigrator.gitlab.GitlabClient;
import net.sigmalab.gitlabmigrator.ssh.Ssh;
import net.sigmalab.gitlabmigrator.utils.JsonMapper;
import net.sigmalab.gitlabmigrator.utils.MappingDescriptor;
import net.sigmalab.gitlabmigrator.utils.MirrorStatus;
import net.sigmalab.gitlabmigrator.utils.RepoMapping;
import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.git.transport.GitSshdSessionFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteSetUrlCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.sigmalab.gitlabmigrator.utils.MirrorStatus.*;

@Command(name = "mirror-repos", description = "Mirrors GitLab group repositories into a Github organisation.")
@Slf4j
public class MirrorRepos implements Callable<Integer> {

    GitHub github;

    @Option(names = {"--target-directory"},
            description = "The target directory to which to clone the repositories.",
            required = true,
            defaultValue = "temp-repos")
    private File targetDir;

    @Option(names = "--gitlab-personal-access-token",
            description = "GitLab personal access token with access to the groups",
            required = true)
    private String gitlabPersonalAccessToken;

    @Option(names = "--github-personal-access-token",
            description = "GitHub personal access token with access to the groups",
            required = true)
    private String githubAppInstalationToken;

    @Option(names = "--group-id-or-path",
            description = "The GitLab group id or path to group",
            required = true,
            converter = GroupIdOrPathConverter.class)
    private Object groupIdOrPath;

    @Option(names = "--mapping-file",
            description = "File mapping repository names between origin and target repositories.\n" +
                    "If not existing, migrator will create one on first run and abort processing for you to validate",
            required = true,
            defaultValue = "repo-mappings.json")
    private File mappingFile;

    @Option(names = {"--github-organization", "--github-organisation"},
            description = "GitHub organisation to mirror the repos into.\n" +
                    "It's the text part of the organisation in it's url",
            required = true)
    private String githubOrganization;

    @Option(names = {"--delete-organization-projects", "--delete-organisation-projects"},
            description = "ATTENTION! Deletes ALL projects in your Github organization!\n" +
                    "Specify if you want to delete ALL projects in organisation prior to uploading them in order to avoid conflicts.\n" +
                    "Note. Your Personal Access Token needs to have `delete_repo` permission set.")
    private boolean deleteTargetProjects;

    @Override
    public Integer call() throws Exception {

        System.out.println("gitlabPersonalAccessToken = " + gitlabPersonalAccessToken);
        System.out.println("githubAppInstalationToken = " + githubAppInstalationToken);
        System.out.println("targetDir = " + targetDir);
        System.out.println("groupIdOrPath = " + groupIdOrPath);
        System.out.println("githubOrganization = " + githubOrganization);

        github = new GitHubBuilder()
                .withOAuthToken(githubAppInstalationToken)
                .build();

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
            Group parentGroup = gitLabApi.getGroupApi().getGroup(groupIdOrPath);
            groupWorkset.add(parentGroup);

            var projectsWorkset = groupWorkset.stream()
                    .map(group -> GitlabClient.getProjectsForGroup(gitLabApi, group))
                    .flatMap(List::stream)
                    .toList();

            for (Project project : projectsWorkset) {

                if (!mappingFile.exists()) {

                    Map<Long, RepoMapping> repoMappings = projectsWorkset.stream()
                            .map(proj -> new RepoMapping(proj.getId(), proj.getPathWithNamespace(), proj.getPathWithNamespace(), TODO))
                            .collect(Collectors.toMap(RepoMapping::id, Function.identity()));

                    var mappingDescriptor = new MappingDescriptor(repoMappings);
                    saveMappingDescriptor(mappingDescriptor);
                    log.error("Mapping file not found. Creating default mapping file.");
                    return 1;

                } else {
                    var mappingDescriptor = loadMappingDescriptor();

                    if (!mappingDescriptorCoversAllProjects(projectsWorkset, mappingDescriptor)) {
                        log.error("Mapping descriptor does not cover all projects");
                        return 3;
                    }

                    if (deleteTargetProjects) {
                        github.getOrganization(githubOrganization)
                                .getRepositories().values().forEach(ghRepository -> {
                                    try {
                                        System.out.println("DELETING " + ghRepository.getGitTransportUrl());
                                        ghRepository.delete();
                                    } catch (IOException e) {
                                        log.error(e.getMessage());
                                        throw new RuntimeException(e);
                                    }
                                });
                    }

                    for (Project proj : projectsWorkset) {

                        RepoMapping repoMapping = mappingDescriptor.repoMappings().get(proj.getId());

                        if (TODO == repoMapping.mirrorStatus() || RATE_LIMIT_HIT_DELAYED == repoMapping.mirrorStatus()) {

                            try {
                                var path = GitlabClient.cloneGitlabProject(targetDir.getAbsolutePath(), proj, proj.getNamespace().getName(), transportConfigCallback);

                                var newRepo = github.getOrganization(githubOrganization)
                                        .createRepository(repoMapping.title())
                                        .private_(true)
                                        .create();

                                System.out.println("CREATED " + newRepo.getUrl());
                                openRepositoryAddRemoteAndPush(newRepo.getSshUrl(), path, transportConfigCallback);
                                var updatedRepoMapping = new RepoMapping(repoMapping.id(), repoMapping.origin(), repoMapping.title(), PUSHED);
                                mappingDescriptor.repoMappings().put(updatedRepoMapping.id(), updatedRepoMapping);
                                Thread.sleep(1000);

                            } catch (Throwable e) {
                                String message = e.getMessage();
                                MirrorStatus reason = null;
                                log.error(message, e);
                                if (message.contains("Please retry your request again later")) {
                                    reason = RATE_LIMIT_HIT_DELAYED;
                                } else {
                                    reason = ERROR;
                                }
                                var updatedRepoMapping = new RepoMapping(repoMapping.id(), repoMapping.origin(), repoMapping.title(), reason);
                                mappingDescriptor.repoMappings().put(updatedRepoMapping.id(), updatedRepoMapping);
                            } finally {
                                saveMappingDescriptor(mappingDescriptor);
                            }

                        } else {
                            log.info("Skipping {} as status is {}", repoMapping.title(), repoMapping.mirrorStatus());
                        }

                    }

                    return 0;

                }

            }

        }

        return 0;
    }

    private void saveMappingDescriptor(MappingDescriptor mappingDescriptor) throws IOException {
        Files.writeString(mappingFile.toPath(), JsonMapper
                .objectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(mappingDescriptor));
    }

    private MappingDescriptor loadMappingDescriptor() throws IOException {
        return JsonMapper.objectMapper().readValue(mappingFile, MappingDescriptor.class);
    }

    @SneakyThrows
    private void openRepositoryAddRemoteAndPush(String sshUrl, String localPath, TransportConfigCallback transportConfigCallback) {

        var git = Git.open(new File(localPath));

        URIish uri = new URIish(sshUrl);

        git.remoteSetUrl()
                .setRemoteName("mirror")
                .setRemoteUri(uri)
                .setUriType(RemoteSetUrlCommand.UriType.PUSH).call();

        log.info("PUSHING {}", sshUrl);
        git.push()
                .setRemote("mirror")
                .add("refs/*:refs/*")
                .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
                .setTransportConfigCallback(transportConfigCallback)
                .call();

    }

    private boolean mappingDescriptorCoversAllProjects(List<Project> projectsWorkset, MappingDescriptor mappingDescriptor) {
        var mappedRepos = mappingDescriptor
                .repoMappings()
                .values()
                .stream()
                .map(RepoMapping::id)
                .toList();

        for (Project project : projectsWorkset) {
            if (!mappedRepos.contains(project.getId())) {
                log.error("Project not mapped {}", project.getHttpUrlToRepo());
                return false;
            }
        }
        return true;
    }

}
