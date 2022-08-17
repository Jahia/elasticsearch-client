package org.jahia.modules.esclient.osgi.karaf.commands;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.jahia.modules.esclient.osgi.karaf.completers.IndicesCompleter;
import org.jahia.modules.esclient.services.ESService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Command(scope = "es", name = "indices-delete", description = "Deletes some ES indices")
@Service
public class ESIndicesDeleteCommand extends AbstractESCommand {

    private static final Logger logger = LoggerFactory.getLogger(ESIndicesDeleteCommand.class);
    private static final String DELETE_ALL = "delete-all";
    private static final String REGEX = "regex";
    private static final String EXACT_MATCH = "exact-match";

    @Option(name = "-s", aliases = "--simulate", description = "If true, do not proceed with the actual deletion, only log the name of the indices which would be deleted")
    boolean simulate;

    @Argument(description = "Indices to delete", required = true, multiValued = true)
    @Completion(IndicesCompleter.class)
    List<String> indices;

    @Override
    public Object execute() throws Exception {
        if (noConnectionDefined()) return null;

        final Map<String, List<String>> deletionPatterns = indices.stream().collect(Collectors.groupingBy(s -> {
            if (StringUtils.equals(s, "*")) return DELETE_ALL;
            if (StringUtils.contains(s, '*')) return REGEX;
            return EXACT_MATCH;
        }));
        List<Pattern> patterns = null;
        if (deletionPatterns.containsKey(REGEX))
            patterns = deletionPatterns.get(REGEX).stream()
                    .map(s -> Pattern.compile(StringUtils.replace(Pattern.quote(s), "*", ".+")))
                    .collect(Collectors.toList());


        final ESService esService = getESService();
        boolean currentIndexDeleted;
        for (String index : esService.listIndices().keySet()) {
            currentIndexDeleted = false;
            if (deletionPatterns.containsKey(DELETE_ALL)) {
                removeIndex(index, esService);
                currentIndexDeleted = true;
                continue;
            }
            if (patterns != null) {
                for (Pattern p : patterns) {
                    if (p.matcher(index).matches()) {
                        removeIndex(index, esService);
                        currentIndexDeleted = true;
                        break;
                    }
                }
                if (currentIndexDeleted) continue;
            }
            if (deletionPatterns.containsKey(EXACT_MATCH) && deletionPatterns.get(EXACT_MATCH).contains(index)) {
                removeIndex(index, esService);
                currentIndexDeleted = true;
            }
        }

        return null;
    }

    private void removeIndex(String index, ESService esService) {
        if (simulate) {
            System.out.println(String.format("Would delete %s", index));
            return;
        }

        if (esService.removeIndex(index)) {
            System.out.println("Deleted " + index);
        } else {
            System.out.println("Failed to delete " + index);
        }
    }
}
