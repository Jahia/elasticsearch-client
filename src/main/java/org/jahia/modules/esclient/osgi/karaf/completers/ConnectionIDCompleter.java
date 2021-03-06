package org.jahia.modules.esclient.osgi.karaf.completers;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.jahia.modules.esclient.services.ESService;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

@Service
public class ConnectionIDCompleter implements Completer {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionIDCompleter.class);

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        final ESService esService = BundleUtils.getOsgiService(ESService.class, null);
        final Collection<String> connections = esService.listConnections();

        if (CollectionUtils.isEmpty(connections)) return -1;


        final StringsCompleter delegate = new StringsCompleter();
        final String argument = commandLine.getCursorArgument();
        if (StringUtils.isBlank(argument)) {
            candidates.addAll(connections);
        } else {
            for (String connection : connections) {
                if (StringUtils.startsWith(connection, argument))
                    candidates.add(connection);
            }

        }
        return delegate.complete(session, commandLine, candidates);
    }
}
