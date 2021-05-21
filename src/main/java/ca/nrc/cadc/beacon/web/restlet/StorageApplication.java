/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2020.                            (c) 2020.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *
 ************************************************************************
 */

package ca.nrc.cadc.beacon.web.restlet;

import ca.nrc.cadc.ac.client.GMSClient;
import ca.nrc.cadc.accesscontrol.AccessControlClient;

import ca.nrc.cadc.auth.PrincipalExtractor;
import ca.nrc.cadc.beacon.web.config.VOSpaceServiceConfig;
import ca.nrc.cadc.beacon.web.config.VOSpaceServiceConfigMgr;
import ca.nrc.cadc.beacon.web.resources.*;
import ca.nrc.cadc.beacon.web.view.FreeMarkerConfiguration;
import ca.nrc.cadc.config.ApplicationConfiguration;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.vos.client.VOSpaceClient;
import ca.nrc.cadc.web.RestletPrincipalExtractor;
import ca.nrc.cadc.web.SubjectGenerator;
import org.apache.log4j.Logger;
import org.restlet.*;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.Directory;
import org.restlet.resource.ResourceException;
import org.restlet.routing.Router;
import org.restlet.routing.TemplateRoute;
import org.restlet.routing.Variable;

import javax.security.auth.Subject;
import javax.servlet.ServletContext;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;


public class StorageApplication extends Application {
    private static final Logger log =
        Logger.getLogger(StorageApplication.class);

    // Public properties are made available in the Context.
    public static final String REGISTRY_CLIENT_KEY = "org.opencadc.registry.client";
    public static final String ACCESS_CONTROL_CLIENT_KEY = "org.opencadc.ac.client";
    public static final String FREEMARKER_CONFIG_KEY = "org.opencadc.vospace.freemarker-config";
    public static final String SERVLET_CONTEXT_ATTRIBUTE_KEY = "org.restlet.ext.servlet.ServletContext";
    public static final String DEFAULT_CONTEXT_PATH = "/storage/";
    private static final String DEFAULT_GMS_SERVICE_ID = "ivo://cadc.nrc.ca/gms";
    public static final String GMS_SERVICE_PROPERTY_KEY = "org.opencadc.gms.service_id";

    // For those wishing to make use of the meta files service for a more readable download link on files.
    public static final String FILES_META_SERVICE_SERVICE_ID_KEY = "org.opencadc.vospace.files_meta_service_id";
    public static final String FILES_META_SERVICE_STANDARD_ID_KEY = "org.opencadc.vospace.files_meta_standard_id";
    public static final String DEFAULT_FILES_META_SERVICE_SERVICE_ID = "ivo://cadc.nrc.ca/files";
    public static final String DEFAULT_FILES_META_SERVICE_STANDARD_ID =
        "vos://cadc.nrc.ca~vospace/CADC/std/archive#file-1.0";

    private static final String DEFAULT_CONFIG_FILE_PATH = System.getProperty("user.home") + "/config/org.opencadc.vosui.properties";

    private final ApplicationConfiguration applicationConfiguration;
    private final VOSpaceServiceConfigMgr vospaceServiceConfigMgr;
    public String storageServiceName;

    /**
     * Constructor.
     *
     * @param context The context to use based on parent component context. This
     *                context should be created using the
     *                {@link Context#createChildContext()} method to ensure a proper
     *                isolation with the other applications.
     */
    public StorageApplication(Context context) {
        super(context);
        setStatusService(new VOSpaceStatusService());
        this.applicationConfiguration = new ApplicationConfiguration(DEFAULT_CONFIG_FILE_PATH);
        this.vospaceServiceConfigMgr = new VOSpaceServiceConfigMgr(this.applicationConfiguration);
    }


    /**
     * Creates a inbound root Restlet that will receive all incoming calls. In
     * general, instances of Router, Filter or Finder classes will be used as
     * initial application Restlet. The default implementation returns null by
     * default. This method is intended to be overridden by subclasses.
     *
     * @return The inbound root Restlet.
     */
    @Override
    public Restlet createInboundRoot() {
        final Context context = getContext();
        log.debug("context: " + context);

        // These values don't change per page load
        context.getAttributes().put(REGISTRY_CLIENT_KEY, createRegistryClient());
        context.getAttributes().put(ACCESS_CONTROL_CLIENT_KEY, createAccessControlClient());
        context.getAttributes().put(GMS_SERVICE_PROPERTY_KEY, createGMSClient());
        context.getAttributes().put(FREEMARKER_CONFIG_KEY, createFreemarkerConfig());
        context.getAttributes().put(FILES_META_SERVICE_SERVICE_ID_KEY,
                                    URI.create(applicationConfiguration.lookup(FILES_META_SERVICE_SERVICE_ID_KEY,
                                                                       DEFAULT_FILES_META_SERVICE_SERVICE_ID)));
        context.getAttributes().put(FILES_META_SERVICE_STANDARD_ID_KEY,
                                    URI.create(applicationConfiguration.lookup(FILES_META_SERVICE_STANDARD_ID_KEY,
                                        DEFAULT_FILES_META_SERVICE_STANDARD_ID)));

        final ServletContext servletContext = getServletContext();
        final String contextPath = (servletContext == null) ? DEFAULT_CONTEXT_PATH : "/";
        final Router router = new Router(context);

        router.attach(contextPath + "ac/authenticate", AccessControlServerResource.class);

        router.attach(contextPath + "groups", GroupNameServerResource.class);

        final Map<String, Variable> routeVariables = new HashMap<>();
        routeVariables.put("svc", new Variable(Variable.TYPE_ALPHA_DIGIT));
        routeVariables.put("path", new Variable(Variable.TYPE_URI_PATH));

        // Build a set of links for each configured storage service

        // backward-compatible routes (pre use of {svc}
        // Allow for an empty path to be the root.
        router.attach(contextPath + "list", MainPageServerResource.class);
        router.attach(contextPath + "list/", MainPageServerResource.class);

        router.attach(contextPath + "page", PageServerResource.class);
        final TemplateRoute pageRoute = router.attach(contextPath + "page/{path}",
            PageServerResource.class);

        // Generic endpoint for files, folders, or links.
        final TemplateRoute itemRoute = router.attach(contextPath + "item/{path}",
            StorageItemServerResource.class);
        final TemplateRoute folderRoute = router.attach(contextPath + "folder/{path}",
            FolderItemServerResource.class);
        final TemplateRoute fileRoute = router.attach(contextPath + "file/{path}",
            FileItemServerResource.class);
        final TemplateRoute linkRoute = router.attach(contextPath + "link/{path}",
            LinkItemServerResource.class);
        final TemplateRoute listRoute = router.attach(contextPath + "list/{path}",
            MainPageServerResource.class);
        final TemplateRoute nodeRoute = router.attach(contextPath + "access/{path}",
            NodeServerResource.class);
        final TemplateRoute rawRoute = router.attach(contextPath + "raw/{path}",
            MainPageServerResource.class);

        itemRoute.getTemplate().getVariables().putAll(routeVariables);
        folderRoute.getTemplate().getVariables().putAll(routeVariables);
        linkRoute.getTemplate().getVariables().putAll(routeVariables);
        pageRoute.getTemplate().getVariables().putAll(routeVariables);
        fileRoute.getTemplate().getVariables().putAll(routeVariables);
        listRoute.getTemplate().getVariables().putAll(routeVariables);
        rawRoute.getTemplate().getVariables().putAll(routeVariables);
        nodeRoute.getTemplate().getVariables().putAll(routeVariables);

        // Support for routes with {svc} in URL
        router.attach(contextPath + "{svc}/page", PageServerResource.class);
        final TemplateRoute svcPageRoute = router.attach(contextPath + "{svc}/page/{path}",
            PageServerResource.class);

        // Allow for an empty path to be the root.
        final TemplateRoute svcListRouteNoPath = router.attach(contextPath +  "{svc}/list", MainPageServerResource.class);
        final TemplateRoute svcListRoute2 = router.attach(contextPath +  "{svc}/list/", MainPageServerResource.class);

        // Generic endpoint for files, folders, or links.
        final TemplateRoute svcItemRoute = router.attach(contextPath + "{svc}/item/{path}",
            StorageItemServerResource.class);
        final TemplateRoute svcFolderRoute = router.attach(contextPath + "{svc}/folder/{path}",
            FolderItemServerResource.class);
        final TemplateRoute svcFileRoute = router.attach(contextPath + "{svc}/file/{path}",
            FileItemServerResource.class);
        final TemplateRoute svcLinkRoute = router.attach(contextPath + "{svc}/link/{path}",
            LinkItemServerResource.class);
        final TemplateRoute svcListRoute = router.attach(contextPath + "{svc}/list/{path}",
            MainPageServerResource.class);
        final TemplateRoute svcNodeRoute = router.attach(contextPath + "{svc}/access/{path}",
            NodeServerResource.class);
        final TemplateRoute svcRawRoute = router.attach(contextPath + "{svc}/raw/{path}",
            MainPageServerResource.class);

        // Set route variables to all the templates
        svcItemRoute.getTemplate().getVariables().putAll(routeVariables);
        svcFolderRoute.getTemplate().getVariables().putAll(routeVariables);
        svcLinkRoute.getTemplate().getVariables().putAll(routeVariables);
        svcPageRoute.getTemplate().getVariables().putAll(routeVariables);
        svcFileRoute.getTemplate().getVariables().putAll(routeVariables);
        svcListRoute.getTemplate().getVariables().putAll(routeVariables);
        svcListRoute.getTemplate().getVariables().putAll(routeVariables);
        svcListRouteNoPath.getTemplate().getVariables().putAll(routeVariables);
        svcListRoute2.getTemplate().getVariables().putAll(routeVariables);
        svcRawRoute.getTemplate().getVariables().putAll(routeVariables);
        svcNodeRoute.getTemplate().getVariables().putAll(routeVariables);

        router.setContext(getContext());
        return router;
    }

    private RegistryClient createRegistryClient() {
        return new RegistryClient();
    }

    private AccessControlClient createAccessControlClient() {
        return new AccessControlClient(URI.create(applicationConfiguration.lookup(StorageApplication.GMS_SERVICE_PROPERTY_KEY,
                                                                          StorageApplication.DEFAULT_GMS_SERVICE_ID)));
    }

    private GMSClient createGMSClient() {
        return new GMSClient(URI.create(applicationConfiguration.lookup(StorageApplication.GMS_SERVICE_PROPERTY_KEY,
                                                                StorageApplication.DEFAULT_GMS_SERVICE_ID)));
    }

    private ServletContext getServletContext() {
        return (ServletContext) getContext().getAttributes().get(StorageApplication.SERVLET_CONTEXT_ATTRIBUTE_KEY);
    }

    /**
     * Override this to set a custom FreeMarkerConfiguration.
     *
     * @return FreeMarkerConfiguration instance.
     */
    public FreeMarkerConfiguration createFreemarkerConfig() {
        final FreeMarkerConfiguration freeMarkerConfiguration = new FreeMarkerConfiguration();
        freeMarkerConfiguration.addDefault(getServletContext());

        return freeMarkerConfiguration;
    }


    public static void main(final String[] args) throws Exception {
        final Component component = new Component();
        final Application application = new StorageApplication(component.getContext().createChildContext()) {
            /**
             * Creates a inbound root Restlet that will receive all incoming calls. In
             * general, instances of Router, Filter or Finder classes will be used as
             * initial application Restlet. The default implementation returns null by
             * default. This method is intended to be overridden by subclasses.
             *
             * @return The inbound root Restlet.
             */
            @Override
            public Restlet createInboundRoot() {
                final Context context = getContext();
                final Router router = (Router) super.createInboundRoot();
                final String[] staticDirs = {"js", "css", "scripts", "fonts", "themes"};

                router.attachDefault(MainPageServerResource.class);

                for (final String dir : staticDirs) {
                    final Reference dirReference = new Reference(URI.create("clap://class/META-INF/resources/" + dir));
                    router.attach(DEFAULT_CONTEXT_PATH + dir + "/", new Directory(context, dirReference));
                }

                return router;
            }

            void handleInParent(final Request request, final Response response) {
                super.handle(request, response);
            }

            @Override
            public void handle(final Request request, final Response response) {
                final SubjectGenerator subjectGenerator = new SubjectGenerator();
                final PrincipalExtractor principalExtractor = new RestletPrincipalExtractor(request);

                try {
                    final Subject subject = subjectGenerator.generate(principalExtractor);

                    Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                        @Override
                        public Object run() {
                            handleInParent(request, response);
                            return null;
                        }
                    });
                } catch (Exception e) {
                    throw new ResourceException(e);
                }
            }
        };

        final Server server = new Server(Protocol.HTTP, 8080, application);

        component.getDefaultHost().attachDefault(application);

        component.getServers().add(server);
        component.getClients().add(Protocol.FILE);
        component.getClients().add(Protocol.CLAP);

        component.start();
    }

//    public String getCurrentNodeURIKey() {
//        return VOSpaceServiceConfigMgr.KEY_BASE + storageServiceName + VOSpaceServiceConfigMgr.NODE_URI_KEY;
//    }
//
//    public String getCurrentUserHome() {
//        return VOSpaceServiceConfigMgr.KEY_BASE + storageServiceName + VOSpaceServiceConfigMgr.USER_HOME_KEY;
//    }

    public VOSpaceServiceConfigMgr getVospaceServiceConfigMgr() {
        return vospaceServiceConfigMgr;
    }

}
