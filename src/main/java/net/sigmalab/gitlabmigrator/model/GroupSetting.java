package net.sigmalab.gitlabmigrator.model;

import java.time.LocalDateTime;

public record GroupSetting(boolean sync, LocalDateTime lastSyncedAt, SyncStatus syncStatus) {
}
