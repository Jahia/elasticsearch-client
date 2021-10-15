package org.jahia.modules.esclient.osgi.karaf.commands;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;
import org.jahia.modules.esclient.osgi.karaf.completers.IndicesCompleter;
import org.jahia.modules.esclient.services.ESService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Command(scope = "es", name = "index-view", description = "Prints out the content of an index")
@Service
public class ESIndexViewCommand extends AbstractESCommand {

    private static final Logger logger = LoggerFactory.getLogger(ESIndexViewCommand.class);

    @Argument(description = "Index to use", required = true)
    @Completion(IndicesCompleter.class)
    String index;

    @Option(name = "-u", description = "If true, upload the content of the index as a file in the JCR")
    boolean uploadInJCR;

    @Override
    public Object execute() throws Exception {
        if (noConnectionDefined()) return null;

        final ESService esService = getESService();
        final Map<String, String> indexContent = esService.getIndexContent(index);

        final ShellTable table = new ShellTable();
        table.column(new Col("ID"));
        table.column(new Col("Source"));

        int count = 0;
        for (Map.Entry<String, String> hit : indexContent.entrySet()) {
            if (++count < 20) {
                final Row row = table.addRow();
                row.addContent(hit.getKey());
                row.addContent(hit.getValue());
            }
        }
        table.print(System.out, true);
        final int size = indexContent.size();
        if (size > 20) {
            System.out.println(String.format("%d entries in the index, printing out only the first 20", size));
        }

        if (uploadInJCR) {
            final String file = writeFileInJCR(index, "es-indices", indexContent.values());
            if (StringUtils.isNotBlank(file)) {
                System.out.println("Uploaded the index content in " + file);
            } else {
                System.out.println("Failed to upload the index content in the JCR");
            }
        }

        return null;
    }
}
