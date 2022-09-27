package net.sigmalab.gitlabmigrator.cli;

import org.gitlab4j.api.models.Project;

public record ProjectPath(Project project, String path) {
}
