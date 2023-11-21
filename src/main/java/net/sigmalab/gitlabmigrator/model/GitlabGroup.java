package net.sigmalab.gitlabmigrator.model;

import java.util.List;

public record GitlabGroup(String name, List<GitlabProject> projects) {}

