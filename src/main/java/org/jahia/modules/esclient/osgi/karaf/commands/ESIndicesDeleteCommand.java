package org.jahia.modules.esclient.osgi.karaf.commands;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.jahia.modules.esclient.osgi.karaf.completers.IndicesCompleter;
import org.jahia.modules.esclient.services.ESService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Command(scope = "es", name = "indices-delete", description = "Deletes some ES indices")
@Service
public class ESIndicesDeleteCommand extends AbstractESCommand {

    private static final Logger logger = LoggerFactory.getLogger(ESIndicesDeleteCommand.class);
    private static final String DELETE_ALL = "delete-all";
    private static final String PREFIX = "prefix";
    private static final String EXACT_MATCH = "exact-match";

    @Argument(description = "Indices to delete", required = true, multiValued = true)
    @Completion(IndicesCompleter.class)
    List<String> indices;

    @Override
    public Object execute() throws Exception {
        if (noConnectionDefined()) return null;

        final Map<String, List<String>> deletionPatterns = indices.stream().collect(Collectors.groupingBy(s -> {
            if (StringUtils.equals(s, "*")) return DELETE_ALL;
            if (StringUtils.endsWith(s, "*")) return PREFIX;
            return EXACT_MATCH;
        }));
        if (deletionPatterns.containsKey(PREFIX))
            deletionPatterns.put(PREFIX, deletionPatterns.get(PREFIX).stream().
                    map(s -> StringUtils.substring(s, 0, s.length()-1)).collect(Collectors.toList()));

        final ESService esService = getESService();
        for (String index : esService.listIndices().keySet()) {
            if (deletionPatterns.containsKey(DELETE_ALL)) {
                removeIndex(index, esService);
                continue;
            }
            if (deletionPatterns.containsKey(PREFIX)) {
                for (String p : deletionPatterns.get(PREFIX)) {
                    if (StringUtils.startsWith(index, p)) {
                        removeIndex(index, esService);
                        continue;
                    }
                }
            }
            if (deletionPatterns.containsKey(EXACT_MATCH) && deletionPatterns.get(EXACT_MATCH).contains(index)) {
                removeIndex(index, esService);
                continue;
            }
        }

        return null;
    }

    private void removeIndex(String index, ESService esService) {
        if (esService.removeIndex(index)) System.out.println("Deleted " + index);
        else System.out.println("Failed to delete " + index);
    }
}
