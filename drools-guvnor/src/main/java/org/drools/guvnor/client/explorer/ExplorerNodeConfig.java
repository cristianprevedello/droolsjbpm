/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.guvnor.client.explorer;

import java.util.Map;

import org.drools.guvnor.client.common.AssetFormats;
import org.drools.guvnor.client.common.GenericCallback;
import org.drools.guvnor.client.messages.Constants;
import org.drools.guvnor.client.resources.Images;
import org.drools.guvnor.client.rpc.PackageConfigData;
import org.drools.guvnor.client.rpc.RepositoryServiceFactory;
import org.drools.guvnor.client.security.Capabilities;
import org.drools.guvnor.client.security.CapabilitiesManager;
import org.drools.guvnor.client.util.Util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

/*
 * This class contains static node config for BRMS' explorer widgets
 */
public class ExplorerNodeConfig {

    private static Constants   constants              = ((Constants) GWT.create( Constants.class ));
    private static Images      images                 = (Images) GWT.create( Images.class );

    //Browse
    public static final String FIND_ID                = "find";
    public static final String CATEGORY_ROOT_ID = "rootcategory";                             //NON-NLS
    public static final String CATEGORY_ID      = "category";                                 //NON-NLS
    public static final String STATES_ID        = "states";                                   //NON-NLS
    public static final String STATES_ROOT_ID   = "rootstates";                               //NON-NLS
    public static final String RECENT_EDITED_ID       = "recentEdited";
    public static final String RECENT_VIEWED_ID       = "recentViewed";
    public static final String INCOMING_ID            = "incoming";

    //QA
    public static final String TEST_SCENARIOS_ID      = "testScenarios";
    public static final String TEST_SCENARIOS_ROOT_ID = "roottestScenarios";
    public static final String ANALYSIS_ID            = "analysis";
    public static final String ANALYSIS_ROOT_ID       = "rootanalysis";

    //Package snapshot
    public static final String PACKAGE_SNAPSHOTS      = "packageSnapshots";

    public static TreeItem getPackageItemStructure(String packageName, String uuid, final Map<TreeItem, String> itemWidgets) {
        TreeItem pkg = new TreeItem( Util.getHeader( images.packages(), packageName ) );
        itemWidgets.put( pkg, uuid );

        TreeItem item = new TreeItem( Util.getHeader( images.ruleAsset(), constants.BusinessRuleAssets() ) );
        item.setUserObject( AssetFormats.BUSINESS_RULE_FORMATS );
        pkg.addItem( item );

        item = new TreeItem( Util.getHeader( images.technicalRuleAssets(), constants.TechnicalRuleAssets() ) );
        itemWidgets.put( item, AssetFormats.DRL );
        item.setUserObject( new String[]{AssetFormats.DRL} );
        pkg.addItem( item );

        item = new TreeItem( Util.getHeader( images.functionAssets(), constants.Functions() ) );
        itemWidgets.put( item, AssetFormats.FUNCTION );
        item.setUserObject( new String[]{AssetFormats.FUNCTION} );
        pkg.addItem( item );

        item = new TreeItem( Util.getHeader( images.dsl(), constants.DSLConfigurations() ) );
        itemWidgets.put( item, AssetFormats.DSL );
        item.setUserObject( new String[]{AssetFormats.DSL} );
        pkg.addItem( item );

        item = new TreeItem( Util.getHeader( images.modelAsset(), constants.Model() ) );
        itemWidgets.put( item, AssetFormats.DRL_MODEL );
        item.setUserObject( new String[]{AssetFormats.DRL_MODEL, AssetFormats.MODEL} );
        pkg.addItem( item );

        if ( Preferences.getBooleanPref( "flex-bpel-editor" ) ) {
            item = new TreeItem( Util.getHeader( images.ruleflowSmall(), constants.RuleFlows() ) );
            itemWidgets.put( item, AssetFormats.RULE_FLOW_RF );
            item.setUserObject( new String[]{AssetFormats.RULE_FLOW_RF, AssetFormats.BPMN2_PROCESS, AssetFormats.BPEL_PACKAGE} );
            pkg.addItem( item );
        } else {
            item = new TreeItem( Util.getHeader( images.ruleflowSmall(), constants.RuleFlows() ) );
            itemWidgets.put( item, AssetFormats.RULE_FLOW_RF );
            item.setUserObject( new String[]{AssetFormats.RULE_FLOW_RF, AssetFormats.BPMN2_PROCESS} );
            pkg.addItem( item );

        }

        item = new TreeItem( Util.getHeader( images.enumeration(), constants.Enumerations() ) );
        itemWidgets.put( item, AssetFormats.ENUMERATION );
        item.setUserObject( new String[]{AssetFormats.ENUMERATION} );
        pkg.addItem( item );

        item = new TreeItem( Util.getHeader( images.testManager(), constants.TestScenarios() ) );
        itemWidgets.put( item, AssetFormats.TEST_SCENARIO );
        item.setUserObject( new String[]{AssetFormats.TEST_SCENARIO} );
        pkg.addItem( item );

        item = new TreeItem( Util.getHeader( images.newFile(), constants.XMLProperties() ) );
        itemWidgets.put( item, AssetFormats.PROPERTIES );
        item.setUserObject( new String[]{AssetFormats.PROPERTIES} );
        pkg.addItem( item );

        item = new TreeItem( Util.getHeader( images.newFile(), constants.OtherAssetsDocumentation() ) );
        itemWidgets.put( item, AssetFormats.PROPERTIES );
        item.setUserObject( new String[0] );
        pkg.addItem( item );

        item = new TreeItem( Util.getHeader( images.workingset(), constants.WorkingSets() ) );
        itemWidgets.put( item, AssetFormats.WORKING_SET );
        item.setUserObject( new String[]{AssetFormats.WORKING_SET} );
        pkg.addItem( item );

        return pkg;
    }

    public static void setupDeploymentTree(Tree tree, Map<TreeItem, String> itemWidgets) {
        TreeItem root = tree.addItem( Util.getHeader( images.chartOrganisation(), constants.PackageSnapshots() ) );
        root.setState( true );
        itemWidgets.put( root, PACKAGE_SNAPSHOTS );
        deploymentListPackages( root );
    }

    private static void deploymentListPackages(final TreeItem root) {
        RepositoryServiceFactory.getService().listPackages( new GenericCallback<PackageConfigData[]>() {
            public void onSuccess(PackageConfigData[] values) {
                PackageHierarchy ph = new PackageHierarchy();

                for ( PackageConfigData val : values ) {
                    ph.addPackage( val );
                }
                for ( PackageHierarchy.Folder hf : ph.getRoot().getChildren() ) {
                    buildDeploymentTree( root, hf );
                }
            }
        } );
    }

    private static void buildDeploymentTree(TreeItem root, PackageHierarchy.Folder fldr) {
        if ( fldr.getConfig() != null ) {
            TreeItem pkg = new TreeItem( Util.getHeader( images.snapshotSmall(), fldr.getConfig().name ) );
            pkg.setUserObject( fldr.getConfig() );
            pkg.addItem( new TreeItem( constants.PleaseWaitDotDotDot() ) );
            root.addItem( pkg );
        } else {
            TreeItem tn = new TreeItem( Util.getHeader( images.emptyPackage(), fldr.getName() ) );
            root.addItem( tn );
            for ( PackageHierarchy.Folder c : fldr.getChildren() ) {
                buildDeploymentTree( tn, c );
            }
        }
    }

    public static Tree getAdminStructure(Map<TreeItem, String> itemWidgets) {
        Tree tree = new Tree();
        tree.setAnimationEnabled( true );

        Object[][] adminStructure =
                new Object[][]{{constants.Category(), images.categorySmall(), "0"}, {constants.Status(), images.statusSmall(), "2"}, {constants.Archive(), images.backupSmall(), "1"}, {constants.EventLog(), images.error(), "4"}, {constants.UserPermission(), images.icoUsersSmall(), "5"}, {constants.ImportExport(), images.saveEdit(), "3"},
                        {constants.RulesVerification(), images.ruleVerification(), "7"}, {constants.RepositoryConfiguration(), images.config(), "8"}, {constants.About(), images.information(), "6"}};

        for ( int i = 0; i < adminStructure.length; i++ ) {

            Object[] packageData = adminStructure[i];
            TreeItem localChildNode = new TreeItem( Util.getHeader( (ImageResource) packageData[1], (String) packageData[0] ) );
            itemWidgets.put( localChildNode, (String) packageData[2] );

            tree.addItem( localChildNode );
        }
        return tree;
    }

    public static void setupBrowseTree(Tree tree, Map<TreeItem, String> itemWidgets) {

        TreeItem root = tree.addItem( Util.getHeader( images.ruleAsset(), constants.AssetsTreeView() ) );

        TreeItem find = root.addItem( Util.getHeader( images.find(), constants.Find() ) );
        itemWidgets.put( find, FIND_ID );

        TreeItem inbox = getInboxStructure( itemWidgets );
        inbox.setState( true );
        root.addItem( inbox );

        if ( CapabilitiesManager.getInstance().shouldShow( Capabilities.SHOW_PACKAGE_VIEW ) ) {
            final TreeItem byStatus = new TreeItem( Util.getHeader( images.statusSmall(), constants.ByStatus() ) );
            itemWidgets.put( byStatus, STATES_ROOT_ID );
            setupStatesStructure( byStatus, itemWidgets );
            root.addItem( byStatus );
        }

        TreeItem byCategory = new TreeItem( Util.getHeader( images.chartOrganisation(), constants.ByCategory() ) );
        itemWidgets.put( byCategory, CATEGORY_ROOT_ID );

        setupCategoriesStructure( byCategory, itemWidgets );
        root.addItem( byCategory );
    }

    private static TreeItem getInboxStructure(Map<TreeItem, String> itemWidgets) {
        TreeItem inbox = new TreeItem( Util.getHeader( images.inbox(), constants.Inbox() ) );

        TreeItem incomingChanges = new TreeItem( Util.getHeader( images.categorySmall(), constants.IncomingChanges() ) );
        itemWidgets.put( incomingChanges, INCOMING_ID );
        inbox.addItem( incomingChanges );

        TreeItem recentOpened = new TreeItem( Util.getHeader( images.categorySmall(), constants.RecentlyOpened() ) );
        itemWidgets.put( recentOpened, RECENT_VIEWED_ID );
        inbox.addItem( recentOpened );

        TreeItem recentEdited = new TreeItem( Util.getHeader( images.categorySmall(), constants.RecentlyEdited() ) );
        itemWidgets.put( recentEdited, RECENT_EDITED_ID );
        inbox.addItem( recentEdited );

        return inbox;
    }

    public static void setupCategoriesStructure(TreeItem byCategory, final Map<TreeItem, String> itemWidgets) {
        doCategoryNode( byCategory, "/", itemWidgets );
    }

    private static void doCategoryNode(final TreeItem treeItem, final String path, final Map<TreeItem, String> itemWidgets) {
        infanticide( treeItem );
        RepositoryServiceFactory.getService().loadChildCategories( path, createGenericCallbackForLoadChildCategories( treeItem, path, itemWidgets ) );
    }

    private static GenericCallback<String[]> createGenericCallbackForLoadChildCategories(final TreeItem treeItem, final String path, final Map<TreeItem, String> itemWidgets) {
        return new GenericCallback<String[]>() {
            public void onSuccess(String[] value) {
                if ( value.length == 0 ) {
                    newRepoDialogIfShowAdminAndPathMatches( path );
                    infanticide( treeItem );
                } else {
                    createChildNodes( treeItem, path, itemWidgets, value );
                }
            }

            private void createChildNodes(final TreeItem treeItem, final String path, final Map<TreeItem, String> itemWidgets, String[] value) {
                for ( int i = 0; i < value.length; i++ ) {

                    final String current = value[i];
                    final TreeItem childNode = new TreeItem( Util.getHeader( images.categorySmall(), current ) );

                    //ID for category tabs. 
                    String widgetId = CATEGORY_ID + "-" + ((path.equals( "/" )) ? current : path + "/" + current);
                    itemWidgets.put( childNode, widgetId );
                    treeItem.addItem( childNode );

                    childNode.addItem( new TreeItem( Util.getHeader( images.categorySmall(), constants.PleaseWaitDotDotDot() ) ) );
                    childNode.getTree().addOpenHandler( createOpenHandlerForTree( itemWidgets, childNode ) );
                }
            }

            private void newRepoDialogIfShowAdminAndPathMatches(final String path) {
                if ( path.equals( "/" ) && CapabilitiesManager.getInstance().shouldShow( Capabilities.SHOW_ADMIN ) ) {
                    RepositoryServiceFactory.getService().listPackages( createGenericCallbackForListPackages() );
                }
            }

            private GenericCallback<PackageConfigData[]> createGenericCallbackForListPackages() {
                return new GenericCallback<PackageConfigData[]>() {
                    public void onSuccess(PackageConfigData[] result) {
                        if ( result.length == 1 ) {
                            doNewRepoDialog();
                        }
                    }
                };
            }

            private OpenHandler<TreeItem> createOpenHandlerForTree(final Map<TreeItem, String> itemWidgets, final TreeItem childNode) {
                return new OpenHandler<TreeItem>() {
                    boolean expanding = false;

                    public void onOpen(OpenEvent<TreeItem> event) {
                        if ( !expanding && event.getTarget() == childNode ) {
                            expanding = true;
                            String widgetID = itemWidgets.get( event.getTarget() );
                            String path = widgetID.substring( widgetID.indexOf( "-" ) + 1 );
                            infanticide( childNode );
                            doCategoryNode( childNode, path, itemWidgets );
                            expanding = false;
                        }
                    }
                };
            }

            private void doNewRepoDialog() {
                NewRepoDialog diag = new NewRepoDialog();
                diag.show();
            }

        };
    }

    private static void infanticide(final TreeItem treeNode) {
        treeNode.removeItems();
    }

    public static void setupStatesStructure(final TreeItem byStatus, final Map<TreeItem, String> itemWidgets) {
        RepositoryServiceFactory.getService().listStates( new GenericCallback<String[]>() {
            public void onSuccess(String[] value) {
                for ( int i = 0; i < value.length; i++ ) {
                    TreeItem childNode = new TreeItem( Util.getHeader( images.categorySmall(), value[i] ) );

                    //ID for state tabs. 
                    String widgetID = STATES_ID + "-" + value[i];
                    itemWidgets.put( childNode, widgetID );
                    byStatus.addItem( childNode );
                }
            }
        } );
    }

    public static Tree getQAStructure(final Map<TreeItem, String> itemWidgets) {
        Tree tree = new Tree();
        tree.setAnimationEnabled( true );

        final TreeItem scenarios = new TreeItem( Util.getHeader( images.testManager(), constants.TestScenariosInPackages() ) );
        scenarios.addItem( new TreeItem( constants.PleaseWaitDotDotDot() ) );
        tree.addItem( scenarios );
        itemWidgets.put( scenarios, TEST_SCENARIOS_ROOT_ID );

        final TreeItem analysis = new TreeItem( Util.getHeader( images.analyze(), constants.Analysis() ) );
        analysis.addItem( new TreeItem( constants.PleaseWaitDotDotDot() ) );
        itemWidgets.put( analysis, ANALYSIS_ROOT_ID );

        if ( Preferences.getBooleanPref( "verifier" ) ) {
            tree.addItem( analysis );
        }

        return tree;
    }
}
