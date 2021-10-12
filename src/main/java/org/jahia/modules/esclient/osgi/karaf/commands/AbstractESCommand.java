package org.jahia.modules.esclient.osgi.karaf.commands;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.Session;
import org.jahia.api.Constants;
import org.jahia.modules.esclient.services.ESService;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

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

    protected String writeFileInJCR(String filename, String foldername, Collection<String> lines) {
        return writeFileInJCR(filename, null, null, foldername, lines);
    }

    protected String writeFileInJCR(String filename, String extension, String mimetype, String foldername, Collection<String> lines) {
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, null, new JCRCallback<String>() {
                @Override
                public String doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    final JCRNodeWrapper filesFolder = session.getNode("/sites/systemsite/files");
                    final JCRNodeWrapper outputDir;
                    if (StringUtils.isNotBlank(foldername)) {
                        outputDir = filesFolder.hasNode(foldername) ?
                                filesFolder.getNode(foldername) :
                                filesFolder.addNode(foldername, Constants.JAHIANT_FOLDER);
                        if (!outputDir.isNodeType(Constants.JAHIANT_FOLDER)) {
                            logger.error(String.format("Impossible to write the folder %s of type %s", outputDir.getPath(), outputDir.getPrimaryNodeTypeName()));
                            return null;
                        }
                    } else {
                        outputDir = filesFolder;
                    }
                    final String nodename = String.format("%s-%s.%s",
                            filename,
                            FastDateFormat.getInstance("yyyy_MM_dd-HH_mm_ss_SSS").format(System.currentTimeMillis()),
                            StringUtils.isBlank(extension) ? "txt" : extension
                    );
                    final InputStream stream = new ByteArrayInputStream(StringUtils.join(lines, System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
                    final JCRNodeWrapper fileNode = outputDir.uploadFile(nodename, stream, StringUtils.isBlank(mimetype) ? "plain/text" : mimetype);
                    session.save();
                    logger.info("Written the file " + fileNode.getPath());
                    return fileNode.getPath();
                }
            });
        } catch (RepositoryException e) {
            logger.error("", e);
            return null;
        }
    }
}
