package net.sigmalab.gitlabmigrator.utils;

/**
 *
 * @param id
 * @param origin
 * @param title
 * @param mirrorStatus
 * @param skip
 */
public record RepoMapping(long id, String origin, String title, MirrorStatus mirrorStatus, boolean skip) {
}
