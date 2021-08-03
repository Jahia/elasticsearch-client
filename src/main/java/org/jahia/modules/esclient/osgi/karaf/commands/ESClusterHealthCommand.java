package org.jahia.modules.esclient.osgi.karaf.commands;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Command(scope = "es", name = "cluster-health", description = "Prints out the cluster health info")
@Service
public class ESClusterHealthCommand extends AbstractESCommand {

    private static final Logger logger = LoggerFactory.getLogger(ESClusterHealthCommand.class);

    @Override
    public Object execute() throws Exception {
        if (noConnectionDefined()) return null;

        final ShellTable table = new ShellTable();
        table.column(new Col("Info"));
        table.column(new Col("Values"));

        for (Map.Entry<String, Object> entry : getESService().getHealthInfo().entrySet())
            table.addRow().addContent(entry.getKey(), entry.getValue());

        table.print(System.out, true);

        return null;
    }
}
