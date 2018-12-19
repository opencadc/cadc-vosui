/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2014.                         (c) 2014.
 * National Research Council            Conseil national de recherches
 * Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 * All rights reserved                  Tous droits reserves
 *
 * NRC disclaims any warranties         Le CNRC denie toute garantie
 * expressed, implied, or statu-        enoncee, implicite ou legale,
 * tory, of any kind with respect       de quelque nature que se soit,
 * to the software, including           concernant le logiciel, y com-
 * without limitation any war-          pris sans restriction toute
 * ranty of merchantability or          garantie de valeur marchande
 * fitness for a particular pur-        ou de pertinence pour un usage
 * pose.  NRC shall not be liable       particulier.  Le CNRC ne
 * in any event for any damages,        pourra en aucun cas etre tenu
 * whether direct or indirect,          responsable de tout dommage,
 * special or general, consequen-       direct ou indirect, particul-
 * tial or incidental, arising          ier ou general, accessoire ou
 * from the use of the software.        fortuit, resultant de l'utili-
 *                                      sation du logiciel.
 *
 *
 * @author jenkinsd
 * 15/05/14 - 1:19 PM
 *
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */
package ca.nrc.cadc;


import org.junit.Test;


public class UserStorageBrowserTest extends AbstractBrowserTest
{
    private static final String STORAGE_ENDPOINT = "/storage/list";


    public UserStorageBrowserTest() throws Exception
    {
        super();
    }


    @Test
    public void browseUserStorage() throws Exception
    {
        System.out.println("Visiting: " + getWebURL() + STORAGE_ENDPOINT);

        final String workingDirectoryName = UserStorageBrowserTest.class.getSimpleName() + "_" + generateAlphaNumeric();
        UserStorageBrowserPage userStoragePage = goTo(STORAGE_ENDPOINT, null, UserStorageBrowserPage.class);

        if (userStoragePage.isMainPage())
        {
            userStoragePage = userStoragePage.waitForStorageLoad();
        }

        final String testFolderName = getUsername();

        verifyTrue(userStoragePage.isDefaultSort());

        // Verify no edit icons are present
        verifyFalse(userStoragePage.isRowItemPermissionsEditable(1));

        userStoragePage.enterSearch(testFolderName);
        int rowCount = userStoragePage.getTableRowCount();
        verifyTrue(rowCount < 3);
        verifyTrue(userStoragePage.verifyFolderName(rowCount - 1, testFolderName));
        verifyTrue(userStoragePage.verifyFolderSize(rowCount - 1));

        // Verify page permissions prior to logging in
        // click through to CADCtest folder
        userStoragePage = userStoragePage.clickFolder(testFolderName);

        // Verify sub folder page state
        verifyTrue(userStoragePage.isSubFolder(testFolderName));

        // Check permissions on page
        verifyTrue(userStoragePage.isReadAccess());

        // Verify no edit icons are present
        verifyFalse(userStoragePage.isRowItemPermissionsEditable(1));

        // Login test - credentials should be in the gradle build file.
        String username = getUsername();

        userStoragePage = loginTest(userStoragePage);

        // verify edit icons are now present - as this is the user's own folder
        verifyTrue(userStoragePage.isRowItemPermissionsEditable(1));

        rowCount = userStoragePage.getTableRowCount();

        System.out.println("Rowcount: " + rowCount);
        verifyTrue(rowCount > 2);

        // Test navigation buttons
        // Test state is currently in a subfolder: Start at Root
        System.out.println("navigating to root...");
        userStoragePage = userStoragePage.navToRoot();

        // Verify in Root Folder
        verifyTrue(userStoragePage.isRootFolder());
        verifyFalse(userStoragePage.quotaIsDisplayed());

        // verify edit icons are still present - #s2106
        verifyTrue(userStoragePage.isRowItemPermissionsEditable(1));

        // Nav to home directory
        userStoragePage = userStoragePage.navToHome();
        userStoragePage.waitForHeaderText("/" + username);
        verifyTrue(userStoragePage.getHeaderText().equals("/" + username));
        verifyTrue(userStoragePage.quotaIsDisplayed());

        int startRow = 1;

        // Assert: home directory for CADCtest user is CADCtest
        // navigate to automated test folder
        String autoTestFolder = "automated_test";

        // Get Write and Read group permissions for this folder
        userStoragePage.enterSearch(autoTestFolder);

        // For whatever reason the automated test folder has been deleted.
        // Recreate it.
        if (userStoragePage.isTableEmpty())
        {
            userStoragePage = userStoragePage.createNewFolder(autoTestFolder);
            userStoragePage.enterSearch(autoTestFolder);
        }

        String parentWriteGroup = userStoragePage.getValueForRowCol(1, 5);
        String parentReadGroup = userStoragePage.getValueForRowCol(1, 6);
        userStoragePage = userStoragePage.clickFolder(autoTestFolder);

        // Test 'nav up one level' - last nav button to test explicitly
        userStoragePage = userStoragePage.navUpLevel();
        userStoragePage.waitForHeaderText("/" + username);
        verifyTrue(userStoragePage.getHeaderText().equals("/" + username));

        // Return to auto test folder
        userStoragePage = userStoragePage.clickFolder(autoTestFolder);

        // Create a context group, and run tests in there
        userStoragePage = userStoragePage.createNewFolder(workingDirectoryName);
        userStoragePage.enterSearch(workingDirectoryName);
        userStoragePage = userStoragePage.clickFolder(workingDirectoryName);

        // Create second test folder
        // This will be deleted at the end of this test suite
        String tempTestFolder = "vosui_automated_test_tobedeleted_";
        userStoragePage = userStoragePage.createNewFolder(tempTestFolder);

        // Test selecting checkbox
        System.out.println("testing selecting checkbox");
        userStoragePage.clickCheckboxForRow(startRow);
        verifyTrue(userStoragePage.isFileSelectedMode(startRow));

        userStoragePage.clickCheckboxForRow(startRow);
        verifyFalse(userStoragePage.isFileSelectedMode(startRow));

        final boolean isPublic = parentReadGroup.equals("Public");

        // Test that permissions are same as the parent to start
        verifyTrue(userStoragePage.isPermissionDataForRow(1, parentWriteGroup, parentReadGroup, isPublic));

        // Edit permissions on the form
        String currentReadGroup = userStoragePage.getValueForRowCol(1, 6);

        // Clearly only works for English test suite. :/
        // Toggle the Public attribute to get the underlying read group (if any)
        if (currentReadGroup.equals("Public"))
        {
            userStoragePage = userStoragePage.togglePublicAttributeForRow();
        }

        String readGroupName = "cadcsw";
        String writeGroupName = "cadc-dev";
        String invalidGroupName = "invalid-group";

        // Don't change anything, verify that the correct message is displayed
        userStoragePage.clickEditIconForFirstRow();
        userStoragePage = userStoragePage.clickButton(UserStorageBrowserPage.SAVE);
        userStoragePage = userStoragePage.clickButton(UserStorageBrowserPage.CANCEL);

//        PermissionsFormData formData = userStoragePage.getValuesFromEditIcon();
//        boolean isModifyNode = true;

        // Set read group to blank (owner access only)
        // Depending on whether the permissions on automated_test parent folder have been changed,
        // the readGroup may not be set initially.
        // Read group may be displayed as 'public', where the read group itself may not be that.
        // The element grabbed here is not visible, but is a reflection of the input to the
        // permissions editing form - attached to the edit icon (the glyphicon-pencil)
//        if (!formData.hasReadGroup())
//        {
//            isModifyNode = false;
//        }

        userStoragePage = userStoragePage.setReadGroup("", true);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, parentWriteGroup, "", false));
        userStoragePage.waitForPromptFinish();

//        isModifyNode = true;
        // Set read group to selected group
        userStoragePage = userStoragePage.setReadGroup(readGroupName, true);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, parentWriteGroup, readGroupName, false));

        // Set write group to blank
//        if (!formData.hasWriteGroup())
//        {
//            isModifyNode = false;
//        }

        userStoragePage = userStoragePage.setWriteGroup("", true);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, "", readGroupName, false));
        userStoragePage = userStoragePage.setWriteGroup(writeGroupName, true);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        // Test response to invalid autocomplete selection
        userStoragePage.clickEditIconForFirstRow();
        // last parameter says 'don't confirm anything'
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.READ_GROUP_INPUT, invalidGroupName,
                                                       false);
        verifyTrue(userStoragePage.isGroupError(UserStorageBrowserPage.READ_GROUP_DIV));

        readGroupName = "CHIMPS";
        // Enter correct one in order to close the prompt
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.READ_GROUP_INPUT,
                                                       readGroupName, true);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        // Test response to invalid autocomplete selection
        userStoragePage.clickEditIconForFirstRow();
        // second parameter says 'don't confirm anything'
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.WRITE_GROUP_INPUT,
                                                       invalidGroupName, false);
        verifyTrue(userStoragePage.isGroupError(UserStorageBrowserPage.WRITE_GROUP_DIV));

        // Enter correct one in order to close the prompt
        writeGroupName = "CHIMPS";
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.WRITE_GROUP_INPUT,
                                                       writeGroupName, true);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        // Toggle public permissions to set them
        // Group name displayed in table should read "Public"
        userStoragePage = userStoragePage.togglePublicAttributeForRow();

        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, "Public", true));
        System.out.println("Set read group to public");

        // Toggle public permission to unset
        userStoragePage = userStoragePage.togglePublicAttributeForRow();
        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        userStoragePage.enterSearch(tempTestFolder);
        userStoragePage = userStoragePage.clickFolder(tempTestFolder);

        String recursiveTestFolder = "recursive_tobedeleted";
        userStoragePage = userStoragePage.createNewFolder(recursiveTestFolder);

        userStoragePage = userStoragePage.navUpLevel();

        userStoragePage = userStoragePage.applyRecursivePermissions(UserStorageBrowserPage.WRITE_GROUP_INPUT,
                                                                    "cadcsw");
        verifyTrue(userStoragePage.isPermissionDataForRow(1, "cadcsw", readGroupName, false));

        userStoragePage = userStoragePage.clickFolder(tempTestFolder);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, "cadcsw", readGroupName, false));

        userStoragePage = userStoragePage.clickFolder(recursiveTestFolder);

        // Test Move folder
        String moveTestFolder = "moveTest_toBeDeleted_" + generateAlphaNumeric();
        userStoragePage = userStoragePage.createNewFolder(moveTestFolder);

        userStoragePage.enterSearch(moveTestFolder);
        userStoragePage.clickCheckboxForRow(1);

        // Kick off first ajax call to populate tree
        userStoragePage = userStoragePage.startMove();

        // Navigate through to the target node
        userStoragePage.selectFolderFromTree(getUsername());
        userStoragePage.selectFolderFromTree(autoTestFolder);
        userStoragePage.selectFolderFromTree(workingDirectoryName);
        userStoragePage.selectFolderFromTree(tempTestFolder);
        userStoragePage.selectFolderFromTree(recursiveTestFolder);

        userStoragePage = userStoragePage.doMove();

        // verify folder was moved to expected location
        userStoragePage.enterSearch(moveTestFolder);
        rowCount = userStoragePage.getTableRowCount();
        verifyTrue(rowCount < 3);
        verifyTrue(userStoragePage.verifyFolderName(rowCount - 1, moveTestFolder));

        // Test Link folder (can't do link to file yet if no browser test for upload file...
        String linkTestFolder = "linkTest_toBeDeleted_" + generateAlphaNumeric();
        userStoragePage = userStoragePage.createNewFolder(linkTestFolder);
        userStoragePage = userStoragePage.navUpLevel();

        // Kick off first ajax call to populate tree
        userStoragePage = userStoragePage.startVOSpaceLink();

        // Navigate through to the target node
        userStoragePage.selectFolderFromTree(getUsername());
        userStoragePage.selectFolderFromTree(autoTestFolder);
        userStoragePage.selectFolderFromTree(workingDirectoryName);
        userStoragePage.selectFolderFromTree(tempTestFolder);
        userStoragePage.selectFolderFromTree(recursiveTestFolder);
        userStoragePage.selectFolderFromTree(linkTestFolder);

        userStoragePage = userStoragePage.doVOSpaceLink();

        // Verify item exists on page
        userStoragePage.enterSearch(linkTestFolder);
        rowCount = userStoragePage.getTableRowCount();
        verifyTrue(rowCount < 3);
        verifyTrue(userStoragePage.verifyFolderName(rowCount - 1, linkTestFolder));

        // Test Delete while cleaning up
        userStoragePage = userStoragePage.navUpLevel();

        // Nav up one level & delete working folder as well
        userStoragePage = userStoragePage.navUpLevel();
        userStoragePage.enterSearch(workingDirectoryName);
        userStoragePage.clickCheckboxForRow(1);
        userStoragePage = userStoragePage.deleteFolder();

        // verify the folder is no longer there
        userStoragePage.enterSearch(tempTestFolder);
        verifyTrue(userStoragePage.isTableEmpty());

        // Scenario 5: logout
        System.out.println("Test logout");
        userStoragePage = userStoragePage.navToRoot();
        userStoragePage.doLogout();

        System.out.println("UserStorageBrowserTest completed");
    }

    private UserStorageBrowserPage loginTest(final UserStorageBrowserPage userPage) throws Exception
    {
        // Scenario 2: Login test - credentials should be in the gradle build file.
        final UserStorageBrowserPage authPage = userPage.doLogin(getUsername(), getPassword());
        verifyTrue(authPage.isLoggedIn());
        System.out.println("logged in");

        return authPage;
    }

}
