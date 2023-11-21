package net.sigmalab.gitlabmigrator.gitlab;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.gitlab4j.api.Constants;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@Slf4j
public class GitlabClient {
    private GitLabApi gitLabApi;
    public GitlabClient(String gitlabAccessToken, boolean enableGitLabLogging) {
        assert gitlabAccessToken != null;

        try (GitLabApi gitLabApi = new GitLabApi("https://gitlab.com", gitlabAccessToken)) {
            if (enableGitLabLogging) {
                gitLabApi.enableRequestResponseLogging();
            }
            this.gitLabApi = gitLabApi;
        }

    }

    public String cloneGitlabProject(String targetDir, Project project, String group, TransportConfigCallback transportConfigCallback, boolean bare) {
        project.setSquashOption(Constants.SquashOption.DEFAULT_ON);
        return cloneRepository(targetDir, project.getSshUrlToRepo(), group, project.getName(), transportConfigCallback, bare);
    }

    @SneakyThrows
    public List<Project> getProjectsForGroup(Group group) {
        System.out.println("Fetching projects for group: " + group.getName() + "(" + group.getId() + ")");
        var groupProjects = gitLabApi.getGroupApi().getProjects(group.getId());

        System.out.println("Found " + groupProjects.size() + " projects:");
        groupProjects.stream().map(Project::getSshUrlToRepo).forEach(url -> System.out.println("\t" + url));
        System.out.println();
        return groupProjects;
    }

    @SneakyThrows
    public List<Group> getSubgroups(String groupIdOrPath) {

        var directSubgroups = gitLabApi.getGroupApi().getSubGroups(groupIdOrPath);
        List<Group> acc = new ArrayList<>(directSubgroups);

        for (Group group : directSubgroups) {
            acc.addAll(getSubgroups(group.getId()+""));
        }

        return acc;
    }

    public String cloneRepository(String targetDir, String uri, String groupName, String repoName, TransportConfigCallback transportConfigCallback, boolean isBare) {

        String pathname = targetDir + "/" + groupName + "/" + repoName;

        File file = new File(pathname);

        if (file.exists()) {
            // pull
            try {
                log.info("FETCHING {}", pathname);
                var git = Git.open(file);
                git.fetch()
//                        .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
                        .setTransportConfigCallback(transportConfigCallback)
                        .call();
            } catch (GitAPIException | IOException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            // clone
            log.info("CLONING {}", uri);

            var cloneCmd = Git.cloneRepository()
                    .setBare(isBare)
                    .setURI(uri)
                    .setDirectory(new File(pathname))
                    .setCloneAllBranches(true)
                    .setTransportConfigCallback(transportConfigCallback)
//                    .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.err)))
                    .setTimeout(60);
            try (Git git = cloneCmd.call()) {

            } catch (GitAPIException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return pathname;
    }
}
