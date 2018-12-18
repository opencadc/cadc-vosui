package ca.nrc.cadc.beacon.web.view;

import ca.nrc.cadc.ac.GroupURI;
import ca.nrc.cadc.vos.VOSURI;

import java.net.URI;
import java.util.Date;


/**
 * Representation of a Link in VOSpace.
 */
public class LinkItem extends StorageItem
{
    public LinkItem(VOSURI uri, long sizeInBytes, Date lastModified,
                    boolean publicFlag, boolean lockedFlag,
                    GroupURI[] writeGroupURIs, GroupURI[] readGroupURIs,
                    String owner, boolean readableFlag, boolean writableFlag, String targetURL)
    {
        super(uri, sizeInBytes, lastModified, publicFlag, lockedFlag,
                writeGroupURIs, readGroupURIs, owner, readableFlag, writableFlag, targetURL);
    }


    @Override
    public String getItemIconCSS() {
        return "glyphicon-link";
    }

    /**
     * Links have no size.
     * @return      String '--'.
     */
    @Override
    public String getSizeHumanReadable()
    {
        return NO_SIZE_DISPLAY;
    }
}
