package net.sigmalab.gitlabmigrator.cli;

import picocli.CommandLine;

public class GroupIdOrPathConverter implements CommandLine.ITypeConverter<Object> {

    @Override
    public Object convert(String groupIdOrPath) throws Exception {
        try {
            return Long.valueOf(groupIdOrPath);
        } catch (NumberFormatException ex) {
            return groupIdOrPath;
        }
    }
}
