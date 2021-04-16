package org.jahia.modules.esclient.osgi.karaf.commands;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.jahia.modules.esclient.osgi.karaf.completers.ConnectionIDCompleter;
import org.jahia.modules.esclient.services.ESService;
import org.jahia.modules.esclient.services.ESServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "es", name = "connection", description = "Selects the connection to use")
@Service
public class SetConnectionCommand extends AbstractESCommand {

    private static final Logger logger = LoggerFactory.getLogger(SetConnectionCommand.class);

    @Argument(description = "Connection ID. If not specified, the available connections are listed", required = false)
    @Completion(ConnectionIDCompleter.class)
    private String connection;

    @Override
    public Object execute() throws Exception {
        if (StringUtils.isBlank(connection)) {
            printAvailableConnections();
        } else {
            setConnection(connection);
        }

        return null;
    }

    private void setConnection(String id) {
        final ESService esService = getESService();
        try {
            esService.loadClient(connection);
            saveConnectionID(connection);
            System.out.printf("Client loaded for the connection %s%n", connection);
        } catch (ESServiceException e) {
            saveNoConnection();
            logger.error("", e);
            System.out.printf("Impossible to load the client for the connection %s : %s%n", connection, e.getMessage());
        }
    }

    private void printAvailableConnections() {
        final ESService esService = getESService();

        final ShellTable table = new ShellTable();
        table.column(new Col("Connection"));
        table.column(new Col("Selected"));

        for (String c : esService.listConnections()) {
            table.addRow().addContent(c, StringUtils.equals(c, getConnectionID()) ? true : null);
        }

        table.print(System.out, true);
    }
}
