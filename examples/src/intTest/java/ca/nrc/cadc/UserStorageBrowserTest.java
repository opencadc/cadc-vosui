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


    @Test
    public void browseUserStorage() throws Exception
    {
        System.out.println("Visiting: " + getWebURL() + STORAGE_ENDPOINT);

        final String workingDirectoryName = UserStorageBrowserTest.class
                                                    .getSimpleName() + "_"
                                            + generateAlphaNumeric(16);
        UserStorageBrowserPage userStoragePage =
                goTo(STORAGE_ENDPOINT, null,
                     UserStorageBrowserPage.class);

        final String testFolderName = "CADCtest";

        verifyTrue(userStoragePage.isDefaultSort());

        // enter search(filter) value
        // check that rows of table are shorted correctly
        // verify entry is correct

        userStoragePage = userStoragePage.enterSearch(testFolderName);
        int rowCount = userStoragePage.getTableRowCount();
        verifyTrue(rowCount < 3);
        verifyTrue(userStoragePage
                           .verifyFolderName(rowCount - 1, testFolderName));
        verifyTrue(userStoragePage.verifyFolderSize(rowCount - 1));


        // Verify page permissions prior to logging in
        // click through to CADCtest folder
        userStoragePage = userStoragePage.clickFolder(testFolderName);

        // Verify sub folder page state
        verifyTrue(userStoragePage.isSubFolder(testFolderName));

        // Check permissions on page
        verifyTrue(userStoragePage.isReadAccess());


        // Login test - credentials should be in the gradle build file.
        String username = "CADCtest";

        userStoragePage = loginTest(userStoragePage);

        rowCount = userStoragePage.getTableRowCount();

        System.out.println("Rowcount: " + rowCount);
        verifyTrue(rowCount > 2);

        // Check access to page: should be write accessible
        verifyFalse(userStoragePage.isReadAccess());

        // Test navigation buttons
        // Test state is currently in a subfolder: Start at Root
        System.out.println("navigating to root...");
        userStoragePage = userStoragePage.navToRoot();

        // Verify in Root Folder
        verifyTrue(userStoragePage.isRootFolder());
        verifyFalse(userStoragePage.quotaIsDisplayed());

        // Nav to home directory
        userStoragePage = userStoragePage.navToHome();
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
            userStoragePage = userStoragePage.enterSearch(autoTestFolder);
        }

        String parentWriteGroup = userStoragePage.getValueForRowCol(1, 5);
        String parentReadGroup = userStoragePage.getValueForRowCol(1, 6);
        userStoragePage = userStoragePage.clickFolder(autoTestFolder);


        // Test 'nav up one level' - last nav button to test explicitly
        userStoragePage = userStoragePage.navUpLevel();
        verifyTrue(userStoragePage.getHeaderText().equals("/" + username));

        // Return to auto test folder
        userStoragePage = userStoragePage.clickFolder(autoTestFolder);


        // Create a context group, and run tests in there
        userStoragePage = userStoragePage.createNewFolder(workingDirectoryName);
        userStoragePage = userStoragePage.enterSearch(workingDirectoryName);
        userStoragePage = userStoragePage.clickFolder(workingDirectoryName);

        // Create second test folder
        // This will be deleted at the end of this test suite
        String tempTestFolder = "vosui_automated_test_tobedeleted_";
//                                + generateAlphaNumeric(8);
        userStoragePage = userStoragePage.createNewFolder(tempTestFolder);

        // Test selecting checkbox
        System.out.println("testing selecting checkbox");
        userStoragePage = userStoragePage.clickCheckboxForRow(startRow);
        verifyTrue(userStoragePage.isFileSelectedMode(startRow));

        userStoragePage = userStoragePage.clickCheckboxForRow(startRow);
        verifyFalse(userStoragePage.isFileSelectedMode(startRow));


        final boolean isPublic = parentReadGroup.equals("Public");

        // Test that permissions are same as the parent to start
        verifyTrue(userStoragePage
                           .isPermissionDataForRow(1, parentWriteGroup, parentReadGroup, isPublic));

        // Edit permissions on the form
        String currentReadGroup =
                userStoragePage.getValueForRowCol(1, 6);

        // Clearly only works for English test suite. :/
        // Toggle the Public attribute to get the underlying read group (if any)
        if (currentReadGroup.equals("Public"))
        {
            userStoragePage = userStoragePage.togglePublicAttributeForRow();
//            userStoragePage.getValueForRowCol(1, 6);
        }

        String readGroupName = "cadcsw";
        String writeGroupName = "cadc-dev";
        String invalidGroupName = "invalid-group";

        // Don't change anything, verify that the correct message is displayed
        userStoragePage = userStoragePage.clickEditIconForFirstRow();
        userStoragePage = userStoragePage.clickButton(UserStorageBrowserPage.SAVE);
        userStoragePage = userStoragePage.clickButton(UserStorageBrowserPage.CANCEL);

        PermissionsFormData formData = userStoragePage.getValuesFromEditIcon();
        Boolean isModifyNode = true;
        // Set read group to blank (owner access only)
        // Depending on whether the permissions on automated_test parent folder have been changed,
        // the readGroup may not be set initially.
        // Read group may be displayed as 'public', where the read group itself may not be that.
        // The element grabbd here is not visible, but is a reflection of the input to the
        // permissions editing form - attached to the edit icon (the glyphicon-pencil)
        if (formData.getReadGroup().equals(""))
        {
            isModifyNode = false;
        }
        userStoragePage = userStoragePage
                .setGroup(UserStorageBrowserPage.READ_GROUP_INPUT, "", isModifyNode);
        verifyTrue(userStoragePage
                           .isPermissionDataForRow(1, parentWriteGroup, "", false));
        userStoragePage.waitForPromptFinish();

        isModifyNode = true;
        // Set read group to selected group
        userStoragePage = userStoragePage
                .setGroup(UserStorageBrowserPage.READ_GROUP_INPUT, readGroupName, true);
        verifyTrue(userStoragePage
                           .isPermissionDataForRow(1, parentWriteGroup, readGroupName, false));

        // Set write group to blank
        if (formData.getWriteGroup().equals(""))
        {
            isModifyNode = false;
        }
        userStoragePage = userStoragePage
                .setGroup(UserStorageBrowserPage.WRITE_GROUP_INPUT, "", isModifyNode);
        verifyTrue(userStoragePage
                           .isPermissionDataForRow(1, "", readGroupName, false));
        userStoragePage = userStoragePage
                .setGroup(UserStorageBrowserPage.WRITE_GROUP_INPUT, writeGroupName, true);
        verifyTrue(userStoragePage
                           .isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        // Test response to invalid autocomplete selection
        userStoragePage.clickEditIconForFirstRow();
        // last parameter says 'don't confirm anything'
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.READ_GROUP_INPUT, invalidGroupName, false);
        verifyTrue(userStoragePage.isGroupError(UserStorageBrowserPage.READ_GROUP_DIV));

        readGroupName = "CHIMPS";
        // Enter correct one in order to close the prompt
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.READ_GROUP_INPUT, readGroupName, true);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        // Test response to invalid autocomplete selection
        userStoragePage.clickEditIconForFirstRow();
        // second parameter says 'don't confirm anything'
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.WRITE_GROUP_INPUT, invalidGroupName, false);
        verifyTrue(userStoragePage.isGroupError(UserStorageBrowserPage.WRITE_GROUP_DIV));

        // Enter correct one in order to close the prompt
        writeGroupName = "CHIMPS";
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.WRITE_GROUP_INPUT, writeGroupName, true);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        // Toggle public permissions to set them
        // Group name displayed in table should read "Public"
        userStoragePage = userStoragePage.togglePublicAttributeForRow();

        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, "Public", true));
        System.out.println("Set read group to public");

        // Toggle public permission to unset
        userStoragePage = userStoragePage.togglePublicAttributeForRow();
        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        userStoragePage = userStoragePage.enterSearch(tempTestFolder);
        userStoragePage = userStoragePage.clickFolder(tempTestFolder);

        String recursiveTestFolder = "recursive_tobedeleted"; // + generateAlphaNumeric(8);
        userStoragePage = userStoragePage.createNewFolder(recursiveTestFolder);

        userStoragePage = userStoragePage.navUpLevel();

        userStoragePage.applyRecursivePermissions(UserStorageBrowserPage.WRITE_GROUP_INPUT, "cadcsw");
        verifyTrue(userStoragePage.isPermissionDataForRow(1, "cadcsw", readGroupName, false));

        userStoragePage = userStoragePage.clickFolder(tempTestFolder);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, "cadcsw", readGroupName, false));


        // Test Move folder
        String moveTestFolder = "moveTest_toBeDeleted_" + generateAlphaNumeric(4);
        userStoragePage = userStoragePage.createNewFolder(moveTestFolder);

        userStoragePage = userStoragePage.enterSearch(moveTestFolder);
        userStoragePage = userStoragePage.clickCheckboxForRow(1);

        // Kick off first ajax call to populate tree
        userStoragePage = userStoragePage.startMove();

        // Navigate through to the target node
        userStoragePage = userStoragePage.selectFolderFromTree("CADCtest");
        userStoragePage = userStoragePage.selectFolderFromTree(autoTestFolder);
        userStoragePage = userStoragePage.selectFolderFromTree(workingDirectoryName);
        userStoragePage = userStoragePage.selectFolderFromTree(tempTestFolder);
        userStoragePage = userStoragePage.selectFolderFromTree(recursiveTestFolder);

        userStoragePage = userStoragePage.doMove();

        // verify folder was moved to expected location
        userStoragePage = userStoragePage.clickFolder(recursiveTestFolder);
        userStoragePage = userStoragePage.enterSearch(moveTestFolder);
        rowCount = userStoragePage.getTableRowCount();
        verifyTrue(rowCount < 3);
        verifyTrue(userStoragePage.verifyFolderName(rowCount - 1, moveTestFolder));


        // Test Link folder (can't do link to file yet if no browser test for upload file...
        String linkTestFolder = "linkTest_toBeDeleted_" + generateAlphaNumeric(4);
        userStoragePage = userStoragePage.createNewFolder(linkTestFolder);
        userStoragePage = userStoragePage.navUpLevel();

        // Kick off first ajax call to populate tree
        userStoragePage = userStoragePage.startVOSpaceLink();

        // Navigate through to the target node
        userStoragePage = userStoragePage.selectFolderFromTree("CADCtest");
        userStoragePage = userStoragePage.selectFolderFromTree(autoTestFolder);
        userStoragePage = userStoragePage.selectFolderFromTree(workingDirectoryName);
        userStoragePage = userStoragePage.selectFolderFromTree(tempTestFolder);
        userStoragePage = userStoragePage.selectFolderFromTree(recursiveTestFolder);
        userStoragePage = userStoragePage.selectFolderFromTree(linkTestFolder);

        userStoragePage = userStoragePage.doVOSpaceLink();

        // Verify item exists on page
        userStoragePage = userStoragePage.enterSearch(linkTestFolder);
        rowCount = userStoragePage.getTableRowCount();
        verifyTrue(rowCount < 3);
        verifyTrue(userStoragePage
                .verifyFolderName(rowCount - 1, linkTestFolder));

        // Test Delete while cleaning up
        userStoragePage = userStoragePage.navUpLevel();

		// Nav up one level & delete working folder as well
		userStoragePage = userStoragePage.navUpLevel();
        userStoragePage = userStoragePage.enterSearch(workingDirectoryName);
        userStoragePage = userStoragePage.clickCheckboxForRow(1);
        userStoragePage = userStoragePage.deleteFolder();

        // verify the folder is no longer there
        userStoragePage = userStoragePage.enterSearch(tempTestFolder);
        verifyTrue(userStoragePage.isTableEmpty());

		// Scenario 5: logout
		System.out.println("Test logout");
        userStoragePage = userStoragePage.doLogout();
		verifyFalse(userStoragePage.isLoggedIn());

    	System.out.println("UserStorageBrowserTest completed");
    }


    private UserStorageBrowserPage loginTest(UserStorageBrowserPage userPage) throws Exception
    {
        // Scenario 2: Login test - credentials should be in the gradle build file.
        String username = "CADCtest";
        userPage = userPage.doLogin(username, "sywymUL4");
        verifyTrue(userPage.isLoggedIn());
        System.out.println("logged in");

        return userPage;
    }


//    @Test
//    public void testErrorPageNotExist() throws Exception
//    {
//        final String bogusName = "bogus_" + generateAlphaNumeric(16);
//
//        System.out.println("Visiting: " + getWebURL() + STORAGE_ENDPOINT + bogusName);
//
//        // This endpoint shouldn't exist
//        UserStorageBrowserPage userStoragePage =
//                goTo(STORAGE_ENDPOINT + "/bogus", null,
//                        UserStorageBrowserPage.class);
//
//        verifyTrue(userStoragePage.verifyErrorMessage("404"));
//
//        System.out.println("UserStorageBrowserTest.testErrorPageNotExist() completed");
//    }
//
//
//    @Test
//    public void testErrorPageNotAuthorised() throws Exception {
//        // This endpoint should exist, but user won't have access
//        UserStorageBrowserPage userStoragePage =
//                goTo(STORAGE_ENDPOINT + "/CADCtest/automated_test/required_folder_please_leave_here", null,
//                        UserStorageBrowserPage.class);
//
//        verifyTrue(userStoragePage.verifyErrorMessage("401"));
//
//        System.out.println("UserStorageBrowserTest.testErrorPageNotAuthorised() completed");
//    }

}
