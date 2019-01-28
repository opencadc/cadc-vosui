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
 *                                  best ide for debugging gradle projects     <http://www.gnu.org/licenses/>.
 *
 *
 ************************************************************************
 */

package ca.nrc.cadc;

import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import ca.nrc.cadc.web.selenium.AbstractTestWebPage;

import ca.nrc.cadc.util.StringUtil;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Arrays;
import java.util.List;


public class UserStorageBrowserPage extends AbstractTestWebPage {
    // Strings for matching against prompt messages and buttons
    public static final String CANCEL = "Cancel";
    private static final String CLOSE = "Close";
    private static final String DELETE_CONFIRMATION_TEXT = "Are you sure you wish to delete the selected items?";
    public static final String LINK = "Link";
    public static final String LINK_OK = "Link successful";
    public static final String MODIFIED = "modified";
    private static final String MOVE_OK = "Move";
    public static final String NOT_MODIFIED = "not modified";
    private static final String OK = "Ok";
    private static final String ROOT_FOLDER_NAME = "ROOT";
    public static final String SAVE = "Save";
    private static final String SUBMITTED = "submitted";
    private static final String SUCCESSFUL = "successful";
    private static final String YES = "Yes";


    // Web Element locators
    private static final By NAVBAR_ELEMENTS_BY = By.xpath("//*[@id=\"navbar-functions\"]/ul");
    private static final By NEW_FOLDER_BY = By.id("newfolder");
    private static final By NEW_VOSPACE_LINK_BY = By.id("new_vospace_link");
    private static final By ACCESS_ACTIONS_DROPDOWN_BY = By.cssSelector("a.access-actions");
    private static final By LOGIN_DROPDOWN_BY = By.cssSelector("a.login-form");
    private static final By USER_ACTIONS_LINK_BY = By.cssSelector("a.user-actions");
    private static final By LOGOUT_LINK_BY = By.id("logout");
    private static final By USERNAME_INPUT_BY = By.id("username");
    private static final By PASSWORD_INPUT_BY = By.id("password");
    private static final By LOGIN_SUBMIT_BUTTON_BY = By.id("submitLogin");
    private static final By FOLDER_NAME_HEADER_BY = By.xpath("//h2[@property='name']");
    private static final By LEVEL_UP_BY = By.id("level-up");
    private static final By HOME_DIR_BY = By.id("homeDir");
    private static final By NEW_PULLDOWN_MENU_BY = By.id("newdropdown");
    private static final By LOGIN_FORM_BY = By.id("loginForm");
    private static final By MOVE_TO_BUTTON_BY = By.cssSelector("button.jqibutton:nth-child(1)");
    private static final By PROGRESS_BAR_BY = By.className("beacon-progress");

    // Error Page: is generated using a different template
    // at the same endpoint: /storage/list
    private static final By ERROR_DISPLAY = By.id("errorDisplayDiv");

    public static final String READ_GROUP_DIV = "readGroupDiv";
    public static final String WRITE_GROUP_DIV = "writeGroupDiv";
    public static final String READ_GROUP_INPUT = "readGroup";
    public static final String WRITE_GROUP_INPUT = "writeGroup";

    static final By PUBLIC_CHECKBOX_BY = By.id("publicPermission");
    static final By RECURSIVE_CHECKBOX_BY = By.id("recursive");

    // Put a row number inbetween these two strings and feed to a 'By'
    // statement to find the first permissions icon in a row
    private static final String EDIT_ICON_BY_TEMPLATE =
        "//*[@id='beacon']/tbody/tr[%s]/td[5]/span[contains(@class, 'glyphicon-pencil')]";

    // Elements always on the page
    @FindBy(id = "beacon_filter")
    private WebElement searchFilter;

    @FindBy(className = "beacon-progress")
    private WebElement progressBar;

    @FindBy(id = "beacon")
    private WebElement beaconTable;

    // element 'Showing x to y of z entries' line
    @FindBy(id = "beacon_info")
    private WebElement statusMessage;

    // header displaying name of current folder
    @FindBy(xpath = "//h2[@property='name']")
    private WebElement folderNameHeader;

    @FindBy(xpath = "//*[@id=\"navbar-functions\"]/ul")
    private WebElement navbarButtonList;

    @FindBy(xpath = "//*[@id=\"beacon\"]/tbody/tr[1]")
    private WebElement firstTableRow;

    @FindBy(id = "level-up")
    private WebElement levelUpButton;

    @FindBy(id = "root")
    private WebElement rootButton;

    // class has 'disabled' in it for base case.
    @FindBy(id = "newdropdown")
    private WebElement newdropdownButton;

    // element of the list under newdropdown
    @FindBy(id = "newfolder")
    private WebElement newFolder;

    @FindBy(id = "download")
    private WebElement downloadButton;

    @FindBy(id = "move")
    private WebElement moveButton;

    @FindBy(id = "delete")
    private WebElement deleteButton;

    @FindBy(id = "more_details")
    private WebElement moredetailsButton;


    public UserStorageBrowserPage(final WebDriver driver, final String headerText) throws Exception {
        super(driver);

        waitForElementPresent(NAVBAR_ELEMENTS_BY);
        waitForElementPresent(FOLDER_NAME_HEADER_BY);
        waitForElementVisible(FOLDER_NAME_HEADER_BY);

        if (StringUtil.hasText(headerText)) {
            waitForHeaderText(headerText);
        }

        waitForElementPresent(PROGRESS_BAR_BY);
        waitForElementVisible(PROGRESS_BAR_BY);

        PageFactory.initElements(driver, this);

        waitUntil(ExpectedConditions.attributeContains(progressBar, "class", "progress-bar-success"));
    }


    public UserStorageBrowserPage(final WebDriver driver) throws Exception {
        this(driver, null);
    }


    // Transition functions
    public UserStorageBrowserPage clickButton(String promptText) throws Exception {
        final By buttonBy = By.xpath("//button[contains(text(),\"" + promptText + "\")]");
        waitForElementClickable(buttonBy);
        click(buttonBy);

        return waitForStorageLoad();
    }

    public UserStorageBrowserPage clickButtonAndWait(final WebElement buttonElement) throws Exception {
        click(buttonElement);
        final UserStorageBrowserPage nextPage = new UserStorageBrowserPage(driver);
        nextPage.waitForStorageLoad();

        return nextPage;
    }

    public UserStorageBrowserPage clickButtonAndWait(final By buttonBy) throws Exception {
        return clickButtonAndWait(find(buttonBy));
    }

    public void clickButtonWithClass(String promptText, String className) throws Exception {
        final By buttonWithClassBy = By.xpath("//button[contains(@class, '" + className + "') and contains(text(),'"
                                                  + promptText + "')]");
        waitForElementPresent(buttonWithClassBy);
        waitForElementVisible(buttonWithClassBy);
        waitForElementClickable(buttonWithClassBy);
        click(buttonWithClassBy);
    }

    public void enterSearch(final String searchString) throws Exception {
        sendKeys(searchFilter, searchString);
    }

    public UserStorageBrowserPage doLogin(String username, String password) throws Exception {
        click(LOGIN_DROPDOWN_BY);

        waitForElementVisible(USERNAME_INPUT_BY);
        waitForElementVisible(PASSWORD_INPUT_BY);

        sendKeys(find(USERNAME_INPUT_BY), username);
        sendKeys(find(PASSWORD_INPUT_BY), password);

        click(find(LOGIN_SUBMIT_BUTTON_BY));

        waitForElementInvisible(LOGIN_FORM_BY);

        return new UserStorageBrowserPage(driver);
    }

    public UserStorageBrowserPage doLogout() throws Exception {
        waitForElementClickable(USER_ACTIONS_LINK_BY);
        click(USER_ACTIONS_LINK_BY);

        waitForElementClickable(LOGOUT_LINK_BY);
        click(LOGOUT_LINK_BY);

        return new UserStorageBrowserPage(driver);
    }

    // Folder Related Transition functions
    public UserStorageBrowserPage clickFolder(String folderName) throws Exception {
        final String currentHeaderText = getHeaderText().equals("ROOT") ? "" : getHeaderText();
        final By folderBy = By.xpath("//*/td/a[contains(text(),'" + folderName + "')]");

        waitForElementPresent(folderBy);
        waitForElementVisible(folderBy);
        waitForElementClickable(folderBy);

        final WebElement folder = find(folderBy);

        click(folder);

        return new UserStorageBrowserPage(driver, currentHeaderText + "/" + folderName);
    }


    // CRUD for folders
    protected UserStorageBrowserPage createNewFolder(final String folderName) throws Exception {
        openNewMenu();

        waitForElementVisible(NEW_FOLDER_BY);
        click(NEW_FOLDER_BY);
        waitForElementClickable(By.id("fname"));
        WebElement newfolderInput = find(By.id("fname"));

        sendKeys(newfolderInput, folderName);

        final WebElement createFolderButton = find(By.xpath("//button[contains(text(),\"Create Folder\")]"));
        click(createFolderButton);

        final UserStorageBrowserPage nextPage = confirmJqiMsg("success");
        nextPage.waitForStorageLoad();

        return nextPage;
    }

    protected <V> V waitUntil(final ExpectedCondition<V> expectedCondition, final int timeoutInSeconds) {
        final WebDriverWait webDriverWait = new WebDriverWait(driver, timeoutInSeconds);
        return webDriverWait.until(expectedCondition);
    }

    protected void openNewMenu() throws Exception {
        waitForElementPresent(NEW_PULLDOWN_MENU_BY);
        waitForElementVisible(NEW_PULLDOWN_MENU_BY);

        click(NEW_PULLDOWN_MENU_BY);

        try {
            waitUntil(ExpectedConditions.visibilityOfElementLocated(NEW_FOLDER_BY), 2);
        } catch (Exception e) {
            // Try again  This fails some times, I don't know why.
            click(NEW_PULLDOWN_MENU_BY);
        }

        if (newdropdownButton.getAttribute("class").contains("disabled")) {
            try {
                final WebElement logout = find(By.id("logout"));
                throw new RuntimeException((logout == null) ? "You are not logged in."
                                               : "You are logged in, but something else is keeping "
                    + "this functionality disabled.");
            } catch (Exception e) {
                throw new RuntimeException("Can't create new items.  That "
                                               + "functionality is disabled.  Did you remember to login?");
            }
        } else {
            System.out.println("Everything is kosher > " + newdropdownButton.getAttribute("class"));
        }
    }

    public void selectFolderFromTree(final String folderName) throws Exception {
        // locate the folder with the name/path provided
        final By nextFolderSelectBy = By.xpath(String.format("//*[@class='layerItemName' and contains(text(),'%s')]",
                                                             folderName));

        waitForElementPresent(nextFolderSelectBy);
        waitForElementVisible(nextFolderSelectBy);

        // click on it
        click(nextFolderSelectBy);
    }


    public UserStorageBrowserPage startMove() throws Exception {
        if (!isDisabled(moveButton)) {
            click(moveButton);
        }

        return new UserStorageBrowserPage(driver);
    }

    public UserStorageBrowserPage doMove() throws Exception {
        waitForElementPresent(MOVE_TO_BUTTON_BY);
        waitForElementVisible(MOVE_TO_BUTTON_BY);
        click(MOVE_TO_BUTTON_BY);
        confirmJQIMessageText(MOVE_OK);

        return clickButton(OK);
    }


    public UserStorageBrowserPage startVOSpaceLink() throws Exception {
        openNewMenu();
        waitForElementVisible(NEW_VOSPACE_LINK_BY);
        click(NEW_VOSPACE_LINK_BY);
        return new UserStorageBrowserPage(driver);
    }

    public UserStorageBrowserPage doVOSpaceLink() throws Exception {
        final String currentHeaderText = getHeaderText();

        clickButton(LINK);
        confirmJQIMessageText(LINK_OK);
        clickButton(OK);
        waitForStorageLoad();

        return new UserStorageBrowserPage(driver, currentHeaderText);
    }


    public UserStorageBrowserPage deleteFolder() throws Exception {
        final String currentHeaderText = getHeaderText();

        if (!isDisabled(deleteButton)) {
            click(deleteButton);
        }

        // locate folder, select checkbox, select delete button
        confirmJQIMessageText(DELETE_CONFIRMATION_TEXT);
        clickButtonWithClass(YES, "btn-danger");

        // confirm folder delete
        confirmJQIColourMessage(SUCCESSFUL);
        clickButton(CLOSE);

        return new UserStorageBrowserPage(driver, currentHeaderText);
    }

    // Permissions functions
    public void clickEditIconForFirstRow() throws Exception {
        final By firstRowBy = By.xpath(String.format(EDIT_ICON_BY_TEMPLATE, "1"));
        waitForElementPresent(firstRowBy);
        waitForElementVisible(firstRowBy);
        click(firstRowBy);

        final By editModalTitleBy =
            By.cssSelector("body > div.jqibox > div.jqi > form > div.jqistates > div > div.lead.jqititle");
        waitForElementPresent(editModalTitleBy);
        waitForElementVisible(editModalTitleBy);
    }


    protected UserStorageBrowserPage setReadGroup(final String newGroup, final boolean isModifyNode) throws Exception {
        return setGroup(READ_GROUP_INPUT, newGroup, isModifyNode);
    }

    protected UserStorageBrowserPage setWriteGroup(final String newGroup, final boolean isModifyNode) throws Exception {
        return setGroup(WRITE_GROUP_INPUT, newGroup, isModifyNode);
    }

    UserStorageBrowserPage setGroup(final String idToFind, final String newGroup, final boolean isModifyNode)
        throws Exception {
        final String currentHeaderText = getHeaderText();
        clickEditIconForFirstRow();
        final By idToFindBy = By.id(idToFind);

        waitForElementPresent(idToFindBy);
        waitForElementVisible(idToFindBy);

        // Click on it to enable the save button
        click(idToFindBy);

        // Send group name
        sendKeys(find(idToFindBy), newGroup);

        waitForAjaxFinished();

        clickButton(SAVE);

        final String confirmationBoxMsg = isModifyNode ? MODIFIED : NOT_MODIFIED;

        return confirmJqiMsg(confirmationBoxMsg);
    }

    /**
     * Convenience function to click through most of the impromptu .prompt
     * confirmation patterns.
     *
     * @param message
     * @throws Exception
     */
    protected UserStorageBrowserPage confirmJqiMsg(String message) throws Exception {
        System.out.println(String.format("Confirming '%s'", message));
        confirmJQIMessageText(message);
        return clickButton(OK);
    }

    protected UserStorageBrowserPage setGroupOnly(final String idToFind, final String newGroup, final boolean confirm)
        throws Exception {
        waitForElementPresent(PUBLIC_CHECKBOX_BY);
        waitForElementVisible(PUBLIC_CHECKBOX_BY);
        waitForElementClickable(PUBLIC_CHECKBOX_BY);

        final WebElement permissionCheckbox = find(PUBLIC_CHECKBOX_BY);
        final WebElement groupInput = find(By.id(idToFind));
        final String currentPermission = permissionCheckbox.getAttribute("checked");

        if (currentPermission != null) {
            // clear checkbox
            click(permissionCheckbox);
        }

        // click on the box first - if a blank is sent in the test
        // could be that the save button is not activated otherwise
        click(groupInput);
        sendKeys(groupInput, newGroup);

        final UserStorageBrowserPage newPage = clickButton(SAVE);

        // read/writeGroupDiv should have 'has-error' class
        // confirm here is conditional because it won't
        // show up if an invalid group has been sent in.
        if (confirm) {
            newPage.confirmJqiMsg(MODIFIED);
            newPage.waitForPromptFinish();
        }

        newPage.waitForStorageLoad();
        return newPage;
    }


    protected UserStorageBrowserPage togglePublicAttributeForRow() throws Exception {
        clickEditIconForFirstRow();

        waitForElementPresent(PUBLIC_CHECKBOX_BY);
        waitForElementVisible(PUBLIC_CHECKBOX_BY);

        click(PUBLIC_CHECKBOX_BY);

        waitForAjaxFinished();

        clickButton(SAVE);
        final UserStorageBrowserPage nextPage = confirmJqiMsg(SUCCESSFUL);
        nextPage.waitForStorageLoad();

        return nextPage;
    }


    protected UserStorageBrowserPage applyRecursivePermissions(final String idToFind, final String newGroup)
        throws Exception {
        clickEditIconForFirstRow();
        waitForElementPresent(RECURSIVE_CHECKBOX_BY);
        waitForElementVisible(RECURSIVE_CHECKBOX_BY);

        final WebElement groupInput = find(By.id(idToFind));

        click(RECURSIVE_CHECKBOX_BY);

        // click on the box first - if a blank is sent in the test
        // could be that the save button is not activated otherwise
        click(groupInput);
        sendKeys(groupInput, newGroup);

        waitForAjaxFinished();

        clickButton(SAVE);

        final UserStorageBrowserPage nextPage = confirmJqiMsg(SUBMITTED);
        nextPage.waitForStorageLoad();

        return nextPage;
    }

    // Row Checkbox related
    public void clickCheckboxForRow(final int rowNum) throws Exception {
        final WebElement firstCheckbox = waitUntil(ExpectedConditions.elementToBeClickable(
            By.xpath("//*[@id=\"beacon\"]/tbody/tr[" + rowNum + "]/td[1]")));
        click(firstCheckbox);
    }

    public void clickCheckboxForFolder(String folderName) throws Exception {
        WebElement folder = find(By.xpath("//*[@id=\"beacon\"]/tbody/tr/td/a[contains(text()," + folderName + ")]"));
        WebElement folderParent = folder.findElement(By.xpath(".."));
        WebElement folderCheckbox = folderParent.findElement(By.xpath("//div[contains(@class=\"select-checkbox\")]"));
        folderCheckbox.click();
    }


    // Navigation functions
    public UserStorageBrowserPage navToRoot() throws Exception {
        final UserStorageBrowserPage rootPage = clickButtonAndWait(rootButton);
        rootPage.waitForStorageLoad();

        return rootPage;
    }

    public UserStorageBrowserPage navUpLevel() throws Exception {
        final String expectedHeaderText;
        final String currentHeaderText = getHeaderText();

        if (StringUtil.hasText(currentHeaderText)) {
            final String[] pathItems = currentHeaderText.split("/");
            final StringBuilder pathBuilder = new StringBuilder();

            for (final String pathItem : Arrays.copyOf(pathItems, pathItems.length - 1)) {
                if (StringUtil.hasText(pathItem)) {
                    pathBuilder.append("/").append(pathItem);
                }
            }

            expectedHeaderText = (pathBuilder.length() == 0) ? "ROOT" : pathBuilder.toString();
        } else {
            expectedHeaderText = currentHeaderText;
        }

        waitForElementPresent(LEVEL_UP_BY);
        waitForElementVisible(LEVEL_UP_BY);

        final UserStorageBrowserPage nextPage = clickButtonAndWait(LEVEL_UP_BY);
        nextPage.waitForStorageLoad();
        nextPage.waitForHeaderText(expectedHeaderText);

        return nextPage;
    }

    public UserStorageBrowserPage navToHome() throws Exception {
        waitForElementPresent(HOME_DIR_BY);
        click(HOME_DIR_BY);

        return new UserStorageBrowserPage(driver);
    }

    int getTableRowCount() {
        final List<WebElement> tableRows = beaconTable.findElements(By.tagName("tr"));
        return tableRows.size();
    }

    boolean verifyFolderName(int rowNum, String expectedValue) {
        List<WebElement> tableRows = beaconTable.findElements(By.tagName("tr"));
        WebElement selectedRow = tableRows.get(rowNum);
        WebElement namecolumn = selectedRow.findElement(By.cssSelector("a:nth-of-type(1)"));
        System.out.println(namecolumn.getText());
        return expectedValue.equals(namecolumn.getText());
    }

    boolean verifyFolderSize(int rowNum) throws Exception {
        List<WebElement> tableRows = beaconTable.findElements(By.tagName("tr"));
        WebElement selectedRow = tableRows.get(rowNum);
        List<WebElement> columns = selectedRow.findElements(By.tagName("td"));
        String sizeString = columns.get(2).getText();
        return sizeString != null;
    }

    public String getFolderName(int rowNum) throws Exception {
        List<WebElement> tableRows = beaconTable.findElements(By.tagName("tr"));
        WebElement selectedRow = tableRows.get(rowNum);
        WebElement namecolumn = selectedRow.findElement(By.cssSelector("a:nth-of-type(1)"));
        System.out.println("Foldername to be returned: " + namecolumn.getText());
        return namecolumn.getText();
    }

    public String getHeaderText() throws Exception {
        // Trim leading whitespace
        return folderNameHeader.getText().replaceAll("\\s+", "");
    }

    public void waitForHeaderText(final String headerText) throws Exception {
        System.out.println("Waiting to see " + headerText + " in the header.");
        waitForTextPresent(FOLDER_NAME_HEADER_BY, headerText);
        System.out.println("Saw " + headerText + " in the header.");
    }

    public String getValueForRowCol(int rowNum, int colNum) {
        final WebElement webElement = getElementForRowCol(rowNum, colNum);
        return (webElement == null) ? "" : webElement.getText();
    }

    public WebElement getElementForRowCol(int rowNum, int colNum) {
        try {
            return find(By.xpath("//*[@id='beacon']/tbody/tr[" + rowNum + "]/td[" + colNum + "]"));
        } catch (Exception e) {
            // element not found, return empty string
            return null;
        }
    }

    // Permissions Data
    public PermissionsFormData getValuesFromEditIcon() throws Exception {
        WebElement editIcon = find(By.xpath(String.format(EDIT_ICON_BY_TEMPLATE, "1")));

        return new PermissionsFormData(editIcon.getAttribute("data-readgroup"),
                                       editIcon.getAttribute("data-writegroup"));
    }

    boolean isLoggedIn() throws Exception {
        try {
            waitForElementPresent(ACCESS_ACTIONS_DROPDOWN_BY);
            final WebElement pullDown = find(ACCESS_ACTIONS_DROPDOWN_BY);

            return (pullDown.getAttribute("class").contains("user-actions")) && isHomeDirButtonDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }


    private boolean isDisabled(WebElement webEl) {
        return webEl.getAttribute("class").contains("disabled");
    }

    public boolean isReadAccess() {
        // need to check class of these buttons, look for 'disabled' in there
        return isDisabled(downloadButton) &&
            isDisabled(newdropdownButton) &&
            !isDisabled(searchFilter) &&
            !isDisabled(levelUpButton) &&
            !isDisabled(rootButton);
    }

    public boolean isSubFolder(String folderName) throws Exception {
        // Check number of elements in button bar
        // Check state of buttons
        boolean baseTest = getHeaderText().contains(folderName) &&
            levelUpButton.isDisplayed() &&
            deleteButton.isDisplayed() &&
            rootButton.isDisplayed() &&
            newdropdownButton.isDisplayed() &&
            moredetailsButton.isDisplayed();


        if (isLoggedIn()) {
            baseTest = baseTest && isHomeDirButtonDisplayed();
        }

        return baseTest;
    }

    public boolean isRootFolder() throws Exception {
        // navigation buttons are NOT displayed in root
        // folder. This will change as functionality is added
        // Currently the navbar only has one child, and it's ID is
        waitForElementPresent(NAVBAR_ELEMENTS_BY);

        if (isLoggedIn()) {
            return getHeaderText().contains(ROOT_FOLDER_NAME)
                && isHomeDirButtonDisplayed()
                && navbarButtonList.findElements(By.xpath("//*[@id=\"navbar-functions\"]/ul/li")).size() == 2;
        } else {
            return getHeaderText().contains(ROOT_FOLDER_NAME)
                && navbarButtonList.findElements(By.xpath("//*[@id=\"navbar-functions\"]/ul/li")).size() == 1;
        }
    }

    public boolean isHomeDirButtonDisplayed() throws Exception {
        final WebElement homeDirButton = find(HOME_DIR_BY);
        return (homeDirButton != null) && homeDirButton.isDisplayed();
    }

    public boolean isFileSelectedMode(int rowNumber) throws Exception {
        // Class of selected row is different:
        // visually it will be different, but for now the change
        // in css class is enough to check
        //*[@id="beacon"]/tbody/tr[1]
        WebElement selectedRow = beaconTable.findElement(By.xpath("//*[@id=\"beacon\"]/tbody/tr[" + rowNumber + "]"));

        if (selectedRow.getAttribute("class").contains("selected")) {

            // Behaviour is different if person is logged in or not
            if (isLoggedIn()) {
                return !(isDisabled(moveButton) && isDisabled(deleteButton) && isDisabled(downloadButton));
            } else {   // There will need to be a check for publicly available for download or not?
                return isDisabled(moveButton) && isDisabled(deleteButton) && !isDisabled(downloadButton);
            }
        } else {
            return false;
        }
    }


    public boolean isDefaultSort() {
        // Name column asc is default sort when page loads
        final WebElement nameColHeader = beaconTable.findElement(
            By.xpath("//*[@id=\"beacon_wrapper\"]/div[2]/div/div[1]/div[1]/div/table/thead/tr/th[2]"));
        return nameColHeader.getAttribute("class").equals("sorting_asc");
    }


    // Impromptu convenience functions
    public void confirmJQIMessageText(final String message) throws Exception {
        final By messageBy = By.className("jqimessage");

        waitForElementPresent(messageBy);
        waitForElementVisible(messageBy);

        try {
            waitForTextPresent(messageBy, message);
        } catch (Exception e) {
            System.err.println(String.format("Waited for '%s' in jqimessage, but saw '" + ((find(messageBy) == null)
                                                 ? "" : find(messageBy)
                                                 .getText() + "'"),
                                             message));

            throw e;
        }
    }

    public void confirmJQIColourMessage(String message) throws Exception {
        waitForElementPresent(By.xpath("//div[contains(@class, 'jqimessage')]/span[contains(text(), '" + message +
                                           "')]"));
    }


    public boolean isTableEmpty() {
        //*[@id="beacon"]/tbody/tr[6]
        final List<WebElement> rowList = beaconTable.findElements(By.xpath("//*/tbody/tr"));

        return rowList.isEmpty()
            || rowList.get(0).findElement(By.xpath("//*/td")).getAttribute("class").equals("dataTables_empty");
    }


    /**
     * Verify that the given row has the values passed in
     *
     * @param readGroup
     * @param isPublic
     * @return
     * @throws Exception
     */
    public boolean isPermissionDataForRow(int row, String writeGroup, String readGroup, boolean isPublic)
        throws Exception {
        // readGroup is the last column (#5)
        // isPublic defines what might be in that row: text should say 'Public' if isPublic is true
        String rowReadGroup = getValueForRowCol(row, 6);
        String rowWriteGroup = getValueForRowCol(row, 5);
        boolean isPermissionSetCorrect = false;

        if (isPublic) {
            if (rowReadGroup.equals("Public") && rowWriteGroup.equals(writeGroup)) {
                isPermissionSetCorrect = true;
            }
        } else if (rowReadGroup.equals(readGroup) && rowWriteGroup.equals(writeGroup)) {
            isPermissionSetCorrect = true;
        }

        return isPermissionSetCorrect;
    }


    boolean isGroupError(String idToFind) throws Exception {
        final By toFind = By.id(idToFind);
        waitForElementClickable(toFind);

        final WebElement readGroupDiv = find(toFind);

        return readGroupDiv.getAttribute("class").contains("has-error");
    }


    boolean quotaIsDisplayed() {
        boolean isDisplayed;
        final By quotaBy = By.xpath("//*[@id=\"beacon_wrapper\"]/div[1]/div/div[contains(@class, 'quota')]");

        try {
            waitForElementVisible(quotaBy);
            final WebElement quota = find(quotaBy);
            isDisplayed = StringUtil.hasText(quota.getText());
        } catch (Exception e) {
            isDisplayed = false;
        }

        return isDisplayed;
    }

    boolean isRowItemPermissionsEditable(int rowNum) throws Exception {
        return (find(By.xpath(String.format(EDIT_ICON_BY_TEMPLATE, rowNum + ""))) != null);
    }


    public boolean isPromptOpen() throws Exception {
        return (find(By.xpath("//div[@class='jqistate']")) != null);
    }

    // --------- Page state wait methods

    void waitForAjaxFinished() throws Exception {
        waitUntil(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver driver) {
                final JavascriptExecutor js = (JavascriptExecutor) driver;
                return (Boolean) js.executeScript("return jQuery.active == 0");
            }
        });
    }


    boolean isMainPage() throws Exception {
        return elementExists(By.xpath("//*[@id=\"main_section\"]"));
    }

    UserStorageBrowserPage waitForStorageLoad() throws Exception {
        // The beacon-progress bar state changes while it's loading
        // the page. Firefox doesn't display whole list until the bar is green
        // instead of striped. Could be this test isn't sufficient but it works
        // to have intTestFirefox not fail.

        waitUntil(ExpectedConditions.attributeContains(PROGRESS_BAR_BY, "class", "progress-bar-success"));
        waitForElementPresent(NAVBAR_ELEMENTS_BY);
        waitForElementPresent(FOLDER_NAME_HEADER_BY);

        return new UserStorageBrowserPage(driver);
    }


    void waitForPromptFinish() throws Exception {
        // jqifade modal div will be up sometimes for longer than it takes
        // for the scripts to click through. Wait for it to go away
        waitForElementInvisible(By.className("jqifade"));
    }


    // Error Page access
    public boolean verifyErrorMessage(String message) throws Exception {
        try {
            final WebElement errorDisplayDiv = waitForElementPresent(ERROR_DISPLAY);
            return errorDisplayDiv.getText().contains(message);
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}


final class PermissionsFormData {
    String readGroup;
    String writeGroup;

    PermissionsFormData(String readGroup, String writeGroup) {
        this.readGroup = readGroup;
        this.writeGroup = writeGroup;
    }

    boolean hasReadGroup() {
        return StringUtil.hasText(readGroup);
    }

    boolean hasWriteGroup() {
        return StringUtil.hasText(writeGroup);
    }
}
