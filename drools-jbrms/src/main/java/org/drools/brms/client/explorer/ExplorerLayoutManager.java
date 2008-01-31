package org.drools.brms.client.explorer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.drools.brms.client.LoggedInUserInfo;
import org.drools.brms.client.admin.ArchivedAssetManager;
import org.drools.brms.client.admin.BackupManager;
import org.drools.brms.client.admin.CategoryManager;
import org.drools.brms.client.admin.StateManager;
import org.drools.brms.client.common.ErrorPopup;
import org.drools.brms.client.common.GenericCallback;
import org.drools.brms.client.common.LoadingPopup;
import org.drools.brms.client.packages.PackageManagerView;
import org.drools.brms.client.packages.SuggestionCompletionCache;
import org.drools.brms.client.rpc.PackageConfigData;
import org.drools.brms.client.rpc.RepositoryServiceFactory;
import org.drools.brms.client.rpc.RuleAsset;
import org.drools.brms.client.ruleeditor.NewAssetWizard;
import org.drools.brms.client.ruleeditor.RuleViewer;
import org.drools.brms.client.rulelist.AssetItemGrid;
import org.drools.brms.client.rulelist.AssetItemGridDataLoader;
import org.drools.brms.client.rulelist.EditItemEvent;
import org.drools.brms.client.rulelist.QuickFindWidget;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Ext;
import com.gwtext.client.data.Node;
import com.gwtext.client.data.NodeTraversalCallback;
import com.gwtext.client.util.DelayedTask;
import com.gwtext.client.util.Format;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.ButtonConfig;
import com.gwtext.client.widgets.QuickTips;
import com.gwtext.client.widgets.TabPanel;
import com.gwtext.client.widgets.TabPanelItem;
import com.gwtext.client.widgets.Toolbar;
import com.gwtext.client.widgets.ToolbarButton;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.event.TabPanelItemListener;
import com.gwtext.client.widgets.event.TabPanelItemListenerAdapter;
import com.gwtext.client.widgets.event.TabPanelListener;
import com.gwtext.client.widgets.event.TabPanelListenerAdapter;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.layout.BorderLayout;
import com.gwtext.client.widgets.layout.ContentPanel;
import com.gwtext.client.widgets.layout.LayoutRegion;
import com.gwtext.client.widgets.layout.LayoutRegionConfig;
import com.gwtext.client.widgets.layout.event.ContentPanelListener;
import com.gwtext.client.widgets.layout.event.ContentPanelListenerAdapter;
import com.gwtext.client.widgets.tree.AsyncTreeNode;
import com.gwtext.client.widgets.tree.AsyncTreeNodeConfig;

import com.gwtext.client.widgets.tree.TreeNode;
import com.gwtext.client.widgets.tree.TreeNodeConfig;
import com.gwtext.client.widgets.tree.TreePanel;
import com.gwtext.client.widgets.tree.TreePanelConfig;
import com.gwtext.client.widgets.tree.XMLTreeLoader;
import com.gwtext.client.widgets.tree.XMLTreeLoaderConfig;
import com.gwtext.client.widgets.tree.event.TreeNodeListener;
import com.gwtext.client.widgets.tree.event.TreeNodeListenerAdapter;
import com.gwtext.client.widgets.tree.event.TreePanelListener;
import com.gwtext.client.widgets.tree.event.TreePanelListenerAdapter;

public class ExplorerLayoutManager {

    private Map screens = new HashMap();

    private BorderLayout layout;
    private boolean packagesLoaded = false;

    ExplorerViewCenterPanel centertabbedPanel;

	private VerticalPanel packagesPanel;

    public BorderLayout getBaseLayout() {
        return layout;
    }

    public ExplorerLayoutManager(LoggedInUserInfo uif) {
        Field.setMsgTarget("side");
        QuickTips.init();

        layout = createBorderLayout();

        centertabbedPanel = new ExplorerViewCenterPanel();

        ContentPanel ncp = new ContentPanel("north", "North Title");


        // setup the main / center panel
        ContentPanel centerPanel = new ContentPanel("center-panel");
        /*
        VerticalPanel contentPanel = new VerticalPanel();
        contentPanel.setWidth("100%");
        contentPanel.setHeight("100%");

        contentPanel.add(centertabbedPanel);
        */
        centerPanel.add(centertabbedPanel);

        layout.add(LayoutRegionConfig.CENTER, centerPanel);

        DockPanel dock = new DockPanel();
        dock.setVerticalAlignment(DockPanel.ALIGN_MIDDLE);
        dock.add(new HTML("<div class='headerBarblue'><img src='images/hdrlogo_drools50px.gif' /></div>"),DockPanel.WEST);

        dock.add(uif, DockPanel.EAST);

        dock.setStyleName("headerBarblue");

        dock.setWidth("100%");
        ncp.add(dock);


        layout.add(LayoutRegionConfig.NORTH, ncp);


        // add a navigation tree menu

        final TabPanel tp = new TabPanel("tab-1");
        tp.setWidth("100%");
        tp.setHeight("100%");

        TabPanelItem tpCategory = tp.addTab("tpi1", "Rules", false);
        TabPanelItem tpPackageExplorer = tp.addTab("tpi2", "Packages", false);
        TabPanelItem tpArchivedAssetManager = tp.addTab("tpi3", "Deployment",
                false);
        TabPanelItem tpBackupManager = tp.addTab("tpi4", "Admin", false);

        VerticalPanel vp1 = new VerticalPanel();
        packagesPanel = new VerticalPanel();
        VerticalPanel vp3 = new VerticalPanel();
        VerticalPanel vp4 = new VerticalPanel();

        // vp1.add(categoriesExplorer(layout, "drools-rules"));

        /** **************************** */
        ContentPanel baseCategory = new ContentPanel("eg-explorer",
                "BRMS Explorer");
        baseCategory.setWidth(" 100%");

        TreePanel categoryTree = basicTreeStructure(ExplorerNodeConfig
                .getRulesStructure(), new TreePanelListenerAdapter() {

            public void onClick(final TreeNode self, EventObject e) {

                //this refreshes the list.
                if (self.getAttribute("id").equals(
                        ExplorerNodeConfig.CATEGORY_ID)) {
                    self.getParentNode().replaceChild(
                            ExplorerNodeConfig.getCategoriesStructure(), self);
                } else if (self.getAttribute("id").equals(
                        ExplorerNodeConfig.STATES_ID)) {
                    self.getParentNode().replaceChild(
                            ExplorerNodeConfig.getStatesStructure(), self);
                } else if (self.getAttribute("id").equals("FIND")) {
                	centertabbedPanel.openFind();
                } else {

                    final String key = (String) self.getUserObject();
                    final boolean isState = key.startsWith("-");

                    if (!centertabbedPanel.showIfOpen(key)) {
                        AssetItemGrid list = new AssetItemGrid(new EditItemEvent() {
                            public void open(String uuid) {
                                centertabbedPanel.openAsset( uuid);
                            }
                        },
                        AssetItemGrid.RULE_LIST_TABLE_ID,
                        new AssetItemGridDataLoader() {
                            public void loadData(int skip, int numberOfRows, GenericCallback cb) {
                            	if (isState) {
                            		RepositoryServiceFactory.getService().loadRuleListForState(key.substring(1) , skip, numberOfRows, cb);
                            	} else {
                            		RepositoryServiceFactory.getService().loadRuleListForCategories(key, skip, numberOfRows, cb);
                            	}
                            }
                        }
                        );

                        centertabbedPanel.addTab(((isState) ?"State: " : "Category: ") + self.getText(), true, list, key);
                    }

                }

            }
        });
        centertabbedPanel.openFind();

        baseCategory.add(categoryTree);
        Toolbar tb = new Toolbar(Ext.generateId());
        vp1.add(tb);
        tb.addButton(new ToolbarButton(new ButtonConfig() {
        	{
                setText("New rule");
                setButtonListener(new ButtonListenerAdapter() {
                    public void onClick(Button button, EventObject e) {
                        NewAssetWizard n = new NewAssetWizard(new EditItemEvent() {
                            public void open(String key) {
                               centertabbedPanel.openAsset(key);
                            }
                        }, true, null, "New rule" );
                        n.show();
                    }
                });
        	}
        }));
        vp1.add(baseCategory);
        vp1.setWidth("100%");


        /** **************************** */




        vp3.add(createExamplesExplorer(layout, "drools-deployment"));

        /** ****************** */
        ContentPanel cp = new ContentPanel("eg-explorer", "BRMS Explorer");
        cp.setWidth(" 100%");

        TreePanel adminTree = basicTreeStructure(ExplorerNodeConfig
                .getAdminStructure(), new TreePanelListenerAdapter() {
            public void onClick(TreeNode self, EventObject e) {


                int id = Integer.parseInt(self.getAttribute("id"));
                switch (id) {
                case 0:
                    centertabbedPanel.addTab("Category Manager", true, new CategoryManager(), "catman");
                    break;
                case 1:
                    centertabbedPanel.addTab("Archived Manager", true, new ArchivedAssetManager(), "archman");
                    break;

                case 2:
                    centertabbedPanel.addTab("State Manager", true, new StateManager(), "stateman");
                    break;
                case 3:
                    centertabbedPanel.addTab("Backup Manager", true, new BackupManager(), "bakman");
                    break;

                case 4:
                    break;
                }

            }
        });

        cp.add(adminTree);
        vp4.add(cp);

        /** ****************** */

        tpCategory.setContent(vp1);


        tpPackageExplorer.setContent(packagesPanel);
        tpArchivedAssetManager.setContent(vp3);
        tpBackupManager.setContent(vp4);
        tpPackageExplorer.addTabPanelItemListener(new TabPanelItemListenerAdapter() {
			public void onActivate(TabPanelItem tab) {
        		if (!packagesLoaded) {
        			packagesPanel.add(packageExplorer(centertabbedPanel));
        			packagesLoaded = true;
        		}
        	}
        });


        tp.activate(0);


        ContentPanel tree = new ContentPanel();
        tree.add(tp);
        layout.add(LayoutRegionConfig.WEST, tree);
    }


    private TreePanel basicTreeStructure(TreeNode basenode, TreePanelListenerAdapter listener) {

        TreePanel adminTreePanel = genericExplorerWidget(basenode);

        adminTreePanel.addTreePanelListener(listener);
        return adminTreePanel;
    }

    private BorderLayout createBorderLayout() {

        LayoutRegionConfig north = new LayoutRegionConfig();
        north.setSplit(false);
        north.setInitialSize(50);
        north.setTitlebar(false);
        north.setAutoScroll(false);

        LayoutRegionConfig west = new LayoutRegionConfig();
        west.setSplit(true);
        west.setInitialSize(300);
        west.setMinSize(175);
        west.setMaxSize(400);
        west.setTitlebar(true);
        west.setCollapsible(true);
        west.setAnimate(true);
        west.setCollapsed(false);
        west.setAutoScroll(false);


        LayoutRegionConfig center = new LayoutRegionConfig();
        center.setTitlebar(false);
        center.setAutoScroll(true);
        center.setTabPosition("top");

        return new BorderLayout("100%", "100%", north, null, west, null, center );
    }

    private ContentPanel createExamplesExplorer(final BorderLayout layout,
            final String menuconfig) {

        // create and configure the main tree
        final TreePanel menuTree = new TreePanel("eg-tree",
                new TreePanelConfig() {
                    {
                        setAnimate(true);
                        setEnableDD(true);
                        setContainerScroll(true);
                        setRootVisible(true);
                    }
                });



        final XMLTreeLoader loader = new XMLTreeLoader(
                new XMLTreeLoaderConfig() {
                    {

                        setDataUrl("site-nav.xml");
                        setMethod("get");
                        setRootTag(menuconfig);
                        setFolderTag("node");
                        setFolderTitleMapping("@title");
                        setLeafTitleMapping("@title");
                        setLeafTag("leaf");
                    }
                });

        AsyncTreeNode root = new AsyncTreeNode("", new AsyncTreeNodeConfig() {
            {
                setLoader(loader);
            }
        });

        // setup a tree listener that reads the content panel associated with
        // the
        // node that is clicked and then displays it in the main / center panel
        TreePanelListener treePanelListener = new TreePanelListenerAdapter() {
            public void onClick(TreeNode self, EventObject e) {
                String screenName = BRMSContentManager.getScreenName(self, self
                        .getText());
                if (screens.containsKey(screenName)) {
                    BRMSContentManager panel = (BRMSContentManager) screens
                            .get(screenName);
                    LayoutRegion region = layout
                            .getRegion(LayoutRegionConfig.CENTER);
                    region.removeAll(true);
                    ContentPanel[] panels = panel.getPanels();
                    for (int i = 0; i < panels.length; i++) {
                        ContentPanel contentPanel = panels[i];
                        layout.add(contentPanel);
                    }
                    region.showPanel(0);
                }
                System.out.println("Clicked on Node: " + self.getText());
                System.out.println("Clicked on Node: "
                        + self.getAttribute("id"));
            }
        };

        // register listener
        menuTree.addTreePanelListener(treePanelListener);

        menuTree.setRootNode(root);
        // menuTree.getRootNode().appendChild( child )
        menuTree.render();

        // loads tree data asynchronously
        root.expand();

        menuTree.expandAll();

        ContentPanel cp = new ContentPanel("eg-explorer", "BRMS Explorer");

        cp.setWidth(" 100%");
        cp.add(menuTree);

        return cp;
    }

    /**
     * Build the package explorer panel.
     */
    private ContentPanel packageExplorer(
            final ExplorerViewCenterPanel tabPanel) {

        final ContentPanel cp = new ContentPanel(Ext.generateId(), "Package Explorer");
        cp.setWidth(" 100%");

        TreeNode root = new TreeNode("Packages");
        root.setAttribute("icon", "images/silk/chart_organisation.gif");

		final TreePanel panel = genericExplorerWidget(root);
        cp.add(panel);
        loadPackages(root);


        TreePanelListener treePanelListener = new TreePanelListenerAdapter() {
            public void onClick(TreeNode node, EventObject e) {
        		if (node.getUserObject() instanceof PackageConfigData) {
        			String uuid = ((PackageConfigData) node.getUserObject()).uuid;
		        			centertabbedPanel.openPackageEditor(uuid, new Command() {
								public void execute() {
									//refresh the package tree.
									packagesPanel.remove(1);
									packagesPanel.add(packageExplorer(centertabbedPanel));
								}
		        			});
	    		} else if (node.getUserObject() instanceof Object[] ){
        			Object[] uo = (Object[]) node.getUserObject();
        			final String[] fmts = (String[]) uo[0];
        			final PackageConfigData pc = (PackageConfigData) node.getParentNode().getUserObject();
        			String key = key(fmts, pc);
        			if (!centertabbedPanel.showIfOpen(key)) {
                        AssetItemGrid list = new AssetItemGrid(new EditItemEvent() {
                            public void open(String uuid) {
                                centertabbedPanel.openAsset(uuid);
                            }
                        },
                        AssetItemGrid.RULE_LIST_TABLE_ID,
                        new AssetItemGridDataLoader() {
                            public void loadData(int skip, int numRows, GenericCallback cb) {
                            	RepositoryServiceFactory.getService().listAssets(pc.uuid, fmts, skip, numRows, cb);
                            }
                        }
                        );

        				tabPanel.addTab(uo[1] + " [" + pc.name + "]", true, list, key);
        			}
        		}
            }




            public void onCollapse(final TreeNode node) {
            	if (node.getText().equals("Packages")) {
            		Node[] children = node.getChildNodes();
	            	for (int i = 0; i < children.length; i++) {
						node.removeChild(children[i]);
					}
	            	loadPackages(node);
            	}
            }


        };
        // register listener
        panel.addTreePanelListener(treePanelListener);
        panel.render();

        return cp;
    }

	private void loadPackages(final TreeNode root) {
		RepositoryServiceFactory.getService().listPackages(
                new GenericCallback() {
                    public void onSuccess(Object data) {
                        PackageConfigData value[] = (PackageConfigData[]) data;
                        for (int i = 0; i < value.length; i++) {
                            root.appendChild(loadPackage(root, value[i]));
                        }
                        root.expand();
                    }
                });
	}

	private String key(String[] fmts,
			PackageConfigData userObject) {
		String key = userObject.uuid;
		for (int i = 0; i < fmts.length; i++) {
			key = key + fmts[i];
		}
		return key;
	}

	private TreeNode loadPackage(final TreeNode root,
			PackageConfigData packagedata) {
		TreeNode pn = ExplorerNodeConfig.getPackageItemStructure(packagedata.name, packagedata.uuid);
		pn.setUserObject(packagedata);
		return pn;
	}

    private TreePanel genericExplorerWidget(final TreeNode childNode) {
        // create and configure the main tree
        final TreePanel menuTree = new TreePanel(Ext.generateId(),
                new TreePanelConfig() {
                    {
                        setAnimate(true);
                        setEnableDD(true);
                        setContainerScroll(true);
                        setRootVisible(true);
                    }
                });

        menuTree.setRootNode(childNode);
        menuTree.render();

        menuTree.expandAll();

        return menuTree;
    }



}
