package net.sigmalab.gitlabmigrator.model;

import org.gitlab4j.api.models.Group;

import java.util.Map;

public record MigratorDb(
        Map<Long, Group> gitlabGroups,
        Map<Long, GroupSetting> groupSettings,
        Map<Long, ProjectSetting> projectSettings) {
}
