/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2021.                            (c) 2021.
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
package ca.nrc.cadc.beacon.web.config;

import ca.nrc.cadc.config.ApplicationConfiguration;
import ca.nrc.cadc.util.StringUtil;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import org.apache.log4j.Logger;

public class VOSpaceServiceConfigMgr {
    private static final Logger log = Logger.getLogger(VOSpaceServiceConfigMgr.class);
    private TreeMap<String, VOSpaceServiceConfig> serviceConfigMap;

    private String defaultServiceName;
    public String currentServiceName;
    private List<String> serviceList = new ArrayList<>();

    // Properties files keys
    public static final String VOSPACE_SERVICE_NAME_KEY = "org.opencadc.vosui.service.name";
    public static final String VOSPACE_DEFAULT_SERVICE_NAME_KEY = "org.opencadc.vosui.service.default";

    // Used to construct service-specific property keys (ie KEY_BASE + {svc name} + {rest of key}
    public static final String KEY_BASE = "org.opencadc.vosui.";
    public static final String NODE_URI_KEY = ".node.resourceid";
    public static final String USER_HOME_KEY = ".user.home";
    public static final String SERVICE_PROPER_NAME_KEY = ".service.proper.name";
    public static final String SERVICE_TYPE_KEY = ".service.type";
    public static final String SERVICE_RESOURCEID_KEY = ".service.resourceid";

    public VOSpaceServiceConfigMgr(ApplicationConfiguration appConfig) {
        this.serviceConfigMap = new TreeMap<>();
        this.loadConfig(appConfig);
    }

    public void loadConfig(ApplicationConfiguration appConfig) {
        if (serviceConfigMap.size() != 0) {
            this.serviceConfigMap = new TreeMap<>();
            this.serviceList = new ArrayList<>();
        }

        // Grab and validate a default service name
        this.defaultServiceName = appConfig.lookup(VOSPACE_DEFAULT_SERVICE_NAME_KEY);

        if (!StringUtil.hasLength(this.defaultServiceName)) {
            throw new IllegalArgumentException("Required value " + VOSPACE_DEFAULT_SERVICE_NAME_KEY + " not found in application config.");
        }

        // Get all VOSpace services named in the config file
        this.serviceList = Arrays.asList(appConfig.lookupAll(VOSPACE_SERVICE_NAME_KEY));

        for (String storageServiceName: this.serviceList) {
            log.debug("adding vospace service to map: " + VOSPACE_SERVICE_NAME_KEY + ": " + serviceList.toString());

            String vospaceResourceIDStr = appConfig.lookup(KEY_BASE + storageServiceName + SERVICE_RESOURCEID_KEY);
            URI vospaceResourceID;
            if (!StringUtil.hasLength(vospaceResourceIDStr)) {
                throw new IllegalArgumentException("Required value " + KEY_BASE + storageServiceName + SERVICE_RESOURCEID_KEY + " not found in application config.");
            } else {
                vospaceResourceID = URI.create(vospaceResourceIDStr);
            }

            String nodeResourceIDStr = appConfig.lookup(KEY_BASE + storageServiceName + NODE_URI_KEY);
            URI nodeResourceID;
            if (!StringUtil.hasLength(nodeResourceIDStr)) {
                throw new IllegalArgumentException("Required value " + KEY_BASE + storageServiceName + NODE_URI_KEY + " not found in application config.");
            } else {
                nodeResourceID = URI.create(nodeResourceIDStr);
            }

            // At this point, the values have been validated
            VOSpaceServiceConfig newConfig = new VOSpaceServiceConfig(storageServiceName,
                vospaceResourceID, nodeResourceID);

            String userHomeDir = KEY_BASE + storageServiceName + USER_HOME_KEY;
            log.debug("user home directory: " + userHomeDir);
            String userHomeValue = appConfig.lookup(userHomeDir);
            if (StringUtil.hasLength(userHomeDir)) {
                newConfig.homeDir = userHomeValue;
            }

            log.debug("node resource id base: " + nodeResourceID);

            // Set to map
            this.serviceConfigMap.put(storageServiceName, newConfig);

        } // end for (String sotrageServiceName: this.serviceList) { ...

    }

    public VOSpaceServiceConfig getServiceConfig(String serviceName) {
        return this.serviceConfigMap.get(serviceName);
    }

    public String getDefaultServiceName() {
        return defaultServiceName;
    }

    public List<String> getServiceList() {
        final List<String> listToSort = new ArrayList<>(this.serviceList);
        Collections.sort(listToSort);
        Collections.reverse(listToSort);
        return listToSort;
    }

}
