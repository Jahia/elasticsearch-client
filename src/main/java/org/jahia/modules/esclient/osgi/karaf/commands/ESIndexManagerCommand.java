package org.jahia.modules.esclient.osgi.karaf.commands;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;
import org.jahia.modules.esclient.services.ESService;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Command(scope = "es", name = "indices-list", description = "Lists the ES indexes")
@Service
public class ESIndexManagerCommand implements Action {

    private static final Logger logger = LoggerFactory.getLogger(ESIndexManagerCommand.class);

    @Override
    public Object execute() throws Exception {
        final ESService esService = BundleUtils.getOsgiService(ESService.class, null);
        final Map<String, String> indexes = esService.listIndices();

        final ShellTable table = new ShellTable();
        table.column(new Col("Index"));
        table.column(new Col("Aliases"));

        for (Map.Entry<String, String> index : indexes.entrySet()) {
            final Row row = table.addRow();
            row.addContent(index.getKey());
            row.addContent(index.getValue());
        }
        table.print(System.out, true);

        return null;
    }
}
