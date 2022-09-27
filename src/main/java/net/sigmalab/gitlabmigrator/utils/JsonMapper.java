package net.sigmalab.gitlabmigrator.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMapper {

    public static final ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper;
    }

}
