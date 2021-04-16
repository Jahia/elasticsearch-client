package org.jahia.modules.esclient.osgi.karaf.commands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.jahia.modules.esclient.osgi.karaf.Constants;
import org.jahia.modules.esclient.osgi.karaf.completers.ConnectionIDCompleter;
import org.jahia.modules.esclient.services.ESService;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "es", name = "connection", description = "Selects the connection to use")
@Service
public class SetConnectionCommand implements Action {

    private static final Logger logger = LoggerFactory.getLogger(SetConnectionCommand.class);

    @Reference
    Session session;

    @Argument(description = "Connection ID", required = true)
    @Completion(ConnectionIDCompleter.class)
    private String connection;

    @Override
    public Object execute() throws Exception {
        session.put(Constants.CONNECTION_ID, connection);
        final ESService esService = BundleUtils.getOsgiService(ESService.class, null);
        esService.loadClient(connection);
        System.out.println("Client loaded for the connection " + connection);
        return null;
    }
}
