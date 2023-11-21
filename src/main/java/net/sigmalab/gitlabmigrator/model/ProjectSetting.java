package net.sigmalab.gitlabmigrator.model;

import java.time.LocalDateTime;

public record ProjectSetting(boolean sync, LocalDateTime lastSyncedAt, SyncStatus syncStatus) {
}