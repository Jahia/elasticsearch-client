package org.jahia.modules.esclient.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESServiceException extends Exception {
    private static final String ERROR_MESSAGE = "Could not connect to ElasticSearch due to : %s";

    public ESServiceException(String message) {
        super(String.format(ERROR_MESSAGE, message));
    }
}
