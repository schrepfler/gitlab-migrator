package net.sigmalab.gitlabmigrator.utils;

import java.util.Map;

public record MappingDescriptor(Map<Long, RepoMapping> repoMappings) {
}
