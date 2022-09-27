package net.sigmalab.gitlabmigrator.gitlab;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GitlabClient {
    public static String cloneGitlabProject(String targetDir, Project project, String group, TransportConfigCallback transportConfigCallback) {
        return cloneRepository(targetDir, project.getSshUrlToRepo(), group, project.getName(), transportConfigCallback);
    }

    @SneakyThrows
    public static List<Project> getProjectsForGroup(GitLabApi gitLabApi, Group group) {
        System.out.println("Fetching projects for group: " + group.getName() + "(" + group.getId() + ")");
        var groupProjects = gitLabApi.getGroupApi().getProjects(group.getId());

        System.out.println("Found " + groupProjects.size() + " projects:");
        groupProjects.stream().map(Project::getSshUrlToRepo).forEach(url -> {
            System.out.println("\t" + url);
        });
        System.out.println();

        return groupProjects;
    }

    @SneakyThrows
    public static List<Group> getSubgroups(GitLabApi gitLabApi, Object groupIdOrPath) {

        var directSubgroups = gitLabApi.getGroupApi().getSubGroups(groupIdOrPath);
        List<Group> acc = new ArrayList<>(directSubgroups);

        for (Group group : directSubgroups) {
            acc.addAll(getSubgroups(gitLabApi, group.getId()));
        }

        return acc;
    }

    public static String cloneRepository(String targetDir, String uri, String groupName, String repoName, TransportConfigCallback transportConfigCallback) {

        String pathname = targetDir + "/" + groupName + "/" + repoName;

        File file = new File(pathname);

        if (file.exists()) {
            // pull
            try {
                log.info("FETCHING {}", pathname);
                var git = Git.open(file);
                git.fetch()
                        .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
                        .setTransportConfigCallback(transportConfigCallback)
                        .call();
            } catch (GitAPIException | IOException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            // clone
            log.info("CLONING {}", uri);
            try (Git git = Git.cloneRepository()
                    .setBare(true)
                    .setURI(uri)
                    .setDirectory(new File(pathname))
                    .setCloneAllBranches(true)
                    .setTransportConfigCallback(transportConfigCallback)
                    .setTimeout(60)
                    .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
                    .call()) {

            } catch (GitAPIException e) {
                log.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return pathname;
    }
}
