package org.jahia.modules.esclient.osgi.karaf.commands;

import org.apache.commons.collections.CollectionUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.jahia.modules.esclient.osgi.karaf.completers.IndicesCompleter;
import org.jahia.modules.esclient.services.ESService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command(scope = "es", name = "index-search", description = "Search entries in an index")
@Service
public class ESIndexSearchCommand extends AbstractESCommand {

    private static final Logger logger = LoggerFactory.getLogger(ESIndexSearchCommand.class);

    @Option(name = "-i", description = "Index to use", required = true)
    @Completion(IndicesCompleter.class)
    String index;

    @Argument(description = "Filters to apply", multiValued = true)
    List<String> filters;

    @Override
    public Object execute() throws Exception {
        if (noConnectionDefined()) return null;

        if (CollectionUtils.isEmpty(filters)) {
            System.out.println("No filter specified");
            return null;
        }

        if (filters.size() % 2 != 0) {
            System.out.println("Invalid filters. The number of values must match the number of fields");
            return null;
        }

        final Map<String, String> queryFilters = new HashMap<>();
        int i = 0;
        while (i < filters.size()) queryFilters.put(filters.get(i++), filters.get(i++));

        final ESService esService = getESService();
        final List<String> hits = esService.getHits(index, queryFilters);

        final int size = hits.size();
        if (size == 0) {
            System.out.println("Nothing matched");
            return null;
        }

        final ShellTable table = new ShellTable();
        table.column(new Col("Hit"));
        for (int j = 0; j < 20 && j < size; j++) {
            table.addRow().addContent(hits.get(j));
        }

        table.noHeaders();
        table.print(System.out, true);
        if (size > 20) {
            System.out.println(String.format("%d entries found in the index, displaying only the first 20", size));
        }

        return null;
    }
}
