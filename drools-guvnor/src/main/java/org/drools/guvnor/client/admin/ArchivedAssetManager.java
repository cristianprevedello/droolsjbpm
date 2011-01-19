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

package org.drools.guvnor.client.admin;

import org.drools.guvnor.client.common.GenericCallback;
import org.drools.guvnor.client.common.PrettyFormLayout;
import org.drools.guvnor.client.messages.Constants;
import org.drools.guvnor.client.resources.Images;
import org.drools.guvnor.client.rpc.PackageConfigData;
import org.drools.guvnor.client.rpc.RepositoryServiceFactory;
import org.drools.guvnor.client.rpc.ValidatedResponse;
import org.drools.guvnor.client.ruleeditor.MultiViewRow;
import org.drools.guvnor.client.rulelist.AssetItemGrid;
import org.drools.guvnor.client.rulelist.AssetItemGridDataLoader;
import org.drools.guvnor.client.rulelist.OpenItemCommand;
import org.drools.guvnor.client.util.TabOpener;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Toolbar;
import com.gwtext.client.widgets.ToolbarButton;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;

/**
 * @author Fernando Meyer
 */

public class ArchivedAssetManager extends Composite {

    private static Images images    = (Images) GWT.create( Images.class );

    private AssetItemGrid grid;
    private ListBox       packages  = new ListBox( true );
    private Constants     constants = GWT.create( Constants.class );

    public ArchivedAssetManager() {

        PrettyFormLayout pf = new PrettyFormLayout();

        VerticalPanel header = new VerticalPanel();
        header.add( new HTML( constants.ArchivedItems() ) );

        pf.addHeader( images.backupLarge(),
                      header ); 

        final TabOpener tabOpener = TabOpener.getInstance();

        OpenItemCommand edit = new OpenItemCommand() {
            public void open(String key) {
                tabOpener.openAsset( key );
            }

            public void open(MultiViewRow[] rows) {
                for ( MultiViewRow row : rows ) {
                    tabOpener.openAsset( row.uuid );
                }
            }
        };
        grid = new AssetItemGrid( edit,
                                  AssetItemGrid.ARCHIVED_RULE_LIST_TABLE_ID,
                                  new AssetItemGridDataLoader() {
                                      public void loadData(int startRow,
                                                           int numberOfRows,
                                                           GenericCallback<org.drools.guvnor.client.rpc.TableDataResult> cb) {
                                          RepositoryServiceFactory.getService().loadArchivedAssets( startRow,
                                                                                                    numberOfRows,
                                                                                                    cb );
                                      }
                                  } );

        loadPackages();
        Toolbar tb = new Toolbar();
        final ToolbarButton restorePackage = new ToolbarButton();
        restorePackage.addListener( new ButtonListenerAdapter() {
            public void onClick(Button button,
                                EventObject e) {
                restorePackage( packages.getValue( packages.getSelectedIndex() ) );
            }

        } );
        restorePackage.setText( constants.RestoreSelectedPackage() );
        tb.addButton( restorePackage );

        final ToolbarButton delPackage = new ToolbarButton();
        delPackage.setText( constants.PermanentlyDeletePackage() );
        delPackage.addListener( new ButtonListenerAdapter() {
            public void onClick(Button button,
                                EventObject e) {
                if ( Window.confirm( constants.AreYouSurePackageDelete() ) ) {
                    deletePackage( packages.getValue( packages.getSelectedIndex() ) );
                }
            }
        } );
        tb.addButton( delPackage );

        pf.startSection( constants.ArchivedPackagesList() );

        pf.addRow( tb );
        pf.addRow( packages );

        pf.endSection();

        tb = new Toolbar();
        final ToolbarButton restoreAsset = new ToolbarButton();
        restoreAsset.setText( constants.RestoreSelectedAsset() );
        tb.addButton( restoreAsset );
        restoreAsset.addListener( new ButtonListenerAdapter() {
            public void onClick(Button button,
                                EventObject e) {
                if ( grid.getSelectedRowUUIDs() == null ) {
                    Window.alert( constants.PleaseSelectAnItemToRestore() );
                    return;
                }
                RepositoryServiceFactory.getService().archiveAssets( grid.getSelectedRowUUIDs(),
                                                                     false,
                                                                     new GenericCallback<java.lang.Void>() {
                                                                         public void onSuccess(Void arg0) {
                                                                             Window.alert( constants.ItemRestored() );
                                                                             grid.refreshGrid();
                                                                         }
                                                                     } );
            };
        } );

        final ToolbarButton deleteAsset = new ToolbarButton();
        deleteAsset.setText( constants.DeleteSelectedAsset() );
        tb.addButton( deleteAsset );

        deleteAsset.addListener( new ButtonListenerAdapter() {
            public void onClick(Button button,
                                EventObject e) {
                if ( grid.getSelectedRowUUIDs() == null ) {
                    Window.alert( constants.PleaseSelectAnItemToPermanentlyDelete() );
                    return;
                }
                if ( !Window.confirm( constants.AreYouSureDeletingAsset() ) ) {
                    return;
                }
                RepositoryServiceFactory.getService().removeAssets( grid.getSelectedRowUUIDs(),
                                                                    new GenericCallback<java.lang.Void>() {

                                                                        public void onSuccess(Void arg0) {
                                                                            Window.alert( constants.ItemDeleted() );
                                                                            grid.refreshGrid();
                                                                        }
                                                                    } );
            }
        } );

        pf.startSection( constants.ArchivedAssets() );
        pf.addRow( tb );

        pf.addRow( grid );

        pf.endSection();

        initWidget( pf );
    }

    private void deletePackage(final String uuid) {
        RepositoryServiceFactory.getService().removePackage( uuid,
                                                             new GenericCallback<java.lang.Void>() {
                                                                 public void onSuccess(Void data) {
                                                                     Window.alert( constants.PackageDeleted() );
                                                                     packages.clear();
                                                                     loadPackages();
                                                                 }
                                                             } );
    }

    private void restorePackage(String uuid) {
        RepositoryServiceFactory.getService().loadPackageConfig( uuid,
                                                                 new GenericCallback<PackageConfigData>() {
                                                                     public void onSuccess(PackageConfigData cf) {
                                                                         cf.archived = false;
                                                                         RepositoryServiceFactory.getService().savePackage( cf,
                                                                                                                            new GenericCallback<ValidatedResponse>() {
                                                                                                                                public void onSuccess(ValidatedResponse data) {
                                                                                                                                    Window.alert( constants.PackageRestored() );
                                                                                                                                    packages.clear();
                                                                                                                                    loadPackages();
                                                                                                                                }
                                                                                                                            } );
                                                                     }
                                                                 } );
    }

    private ListBox loadPackages() {

        RepositoryServiceFactory.getService().listArchivedPackages( new GenericCallback<PackageConfigData[]>() {
            public void onSuccess(PackageConfigData[] configs) {
                for ( int i = 0; i < configs.length; i++ ) {
                    packages.addItem( configs[i].name,
                                      configs[i].uuid );
                }
                if ( configs.length == 0 ) {
                    packages.addItem( constants.noArchivedPackages() );
                }
            }
        } );

        return packages;
    }

}