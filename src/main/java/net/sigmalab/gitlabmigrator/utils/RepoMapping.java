package net.sigmalab.gitlabmigrator.utils;

public record RepoMapping(long id, String origin, String title, MirrorStatus mirrorStatus) {
}
