package org.jahia.modules.esclient.osgi.karaf.commands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.Session;
import org.jahia.modules.esclient.services.ESService;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jahia.modules.esclient.osgi.karaf.Constants.CONNECTION_ID;

public abstract class AbstractESCommand implements Action {

    private static final Logger logger = LoggerFactory.getLogger(AbstractESCommand.class);

    @Reference
    private Session session;

    protected void saveConnectionID(String id) {
        session.put(CONNECTION_ID, id);
    }

    protected void saveNoConnection() {
        saveConnectionID(null);
    }

    protected String getConnectionID() {
        return (String) session.get(CONNECTION_ID);
    }

    protected ESService getESService() {
        final ESService service = BundleUtils.getOsgiService(ESService.class, null);
        if (service == null || !service.isClientActive()) saveNoConnection();
        return service;
    }

    protected boolean noConnectionDefined() {
        if (getConnectionID() == null) {
            System.out.println("No connection selected");
            return true;
        }
        return false;
    }

}
