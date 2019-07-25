/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2016.                            (c) 2016.
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

import ca.nrc.cadc.dlm.DownloadDescriptor;
import ca.nrc.cadc.net.HttpDownload;
import ca.nrc.cadc.net.InputStreamWrapper;
import org.restlet.data.MediaType;

import javax.security.auth.Subject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * Represent a ZIP file output stream for a series of files and folders to download.
 * CURRENTLY NOT USED, BUT HERE FOR PERSERVATION
 * jenkinsd 2017.04.25
 */
public class ZIPFileRepresentation extends AbstractAuthOutputRepresentation {

    private final Iterator<DownloadDescriptor> downloadDescriptorIterator;
    private final Subject currentUser;


    /**
     * Full constructor.
     *
     * @param currentUser                   The currently authenticated user.
     * @param downloadDescriptorIterator    Iterator for Download items.
     */
    public ZIPFileRepresentation(final Subject currentUser,
                                 final Iterator<DownloadDescriptor> downloadDescriptorIterator) {
        super(MediaType.APPLICATION_ZIP);

        this.currentUser = currentUser;
        this.downloadDescriptorIterator = downloadDescriptorIterator;
    }


    /**
     * Write out the files to the given stream.
     *
     * @param outputStream      The OutputStream to write to.
     * @throws IOException      Any writing errors.
     */
    @Override
    public void write(final OutputStream outputStream) throws IOException {
        final ZipOutputStream zos = new ZipOutputStream(outputStream);

        while (downloadDescriptorIterator.hasNext()) {
            final DownloadDescriptor downloadDescriptor = downloadDescriptorIterator.next();
            if (downloadDescriptor.url != null) {
                final InputStreamWrapper inputStreamWrapper =
                        new InputStreamWrapper() {
                            @Override
                            public void read(final InputStream inputStream) throws IOException {
                                int length;

                                // create byte buffer
                                byte[] buffer = new byte[1024];

                                // Begin writing a new ZIP entry, positions
                                // the stream to the start of the entry
                                // data.
                                zos.putNextEntry(new ZipEntry(downloadDescriptor.destination));

                                while ((length = inputStream.read(buffer)) > 0) {
                                    zos.write(buffer, 0, length);
                                }

                                zos.closeEntry();

                                inputStream.close();
                            }
                        };

                final HttpDownload httpDownload = new HttpDownload(downloadDescriptor.url, inputStreamWrapper);

                httpDownload.setFollowRedirects(true);

                downloadAs(currentUser, httpDownload);
            }
        }

        // close the ZipOutputStream
        zos.close();
    }
}
