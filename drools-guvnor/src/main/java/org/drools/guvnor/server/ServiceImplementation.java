/*
 * Copyright 2005 JBoss Inc
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

package org.drools.guvnor.server;

import static org.drools.guvnor.server.util.ClassicDRLImporter.getRuleName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.drools.ClockType;
import org.drools.RuleBase;
import org.drools.RuleBaseConfiguration;
import org.drools.RuleBaseFactory;
import org.drools.SessionConfiguration;
import org.drools.base.ClassTypeResolver;
import org.drools.common.AbstractRuleBase;
import org.drools.common.DroolsObjectOutputStream;
import org.drools.common.InternalRuleBase;
import org.drools.common.InternalWorkingMemory;
import org.drools.compiler.DrlParser;
import org.drools.compiler.DroolsParserException;
import org.drools.core.util.DroolsStreamUtils;
import org.drools.guvnor.client.common.AssetFormats;
import org.drools.guvnor.client.explorer.ExplorerNodeConfig;
import org.drools.guvnor.client.rpc.AssetPageRequest;
import org.drools.guvnor.client.rpc.AssetPageResponse;
import org.drools.guvnor.client.rpc.AssetPageRow;
import org.drools.guvnor.client.rpc.BuilderResult;
import org.drools.guvnor.client.rpc.BuilderResultLine;
import org.drools.guvnor.client.rpc.BulkTestRunResult;
import org.drools.guvnor.client.rpc.DetailedSerializationException;
import org.drools.guvnor.client.rpc.DiscussionRecord;
import org.drools.guvnor.client.rpc.LogEntry;
import org.drools.guvnor.client.rpc.MetaData;
import org.drools.guvnor.client.rpc.MetaDataQuery;
import org.drools.guvnor.client.rpc.PackageConfigData;
import org.drools.guvnor.client.rpc.PushResponse;
import org.drools.guvnor.client.rpc.RepositoryService;
import org.drools.guvnor.client.rpc.RuleAsset;
import org.drools.guvnor.client.rpc.ScenarioResultSummary;
import org.drools.guvnor.client.rpc.ScenarioRunResult;
import org.drools.guvnor.client.rpc.SingleScenarioResult;
import org.drools.guvnor.client.rpc.SnapshotDiff;
import org.drools.guvnor.client.rpc.SnapshotDiffs;
import org.drools.guvnor.client.rpc.SnapshotInfo;
import org.drools.guvnor.client.rpc.TableConfig;
import org.drools.guvnor.client.rpc.TableDataResult;
import org.drools.guvnor.client.rpc.TableDataRow;
import org.drools.guvnor.client.rpc.ValidatedResponse;
import org.drools.guvnor.server.builder.AuditLogReporter;
import org.drools.guvnor.server.builder.BRMSPackageBuilder;
import org.drools.guvnor.server.builder.ContentAssemblyError;
import org.drools.guvnor.server.builder.ContentPackageAssembler;
import org.drools.guvnor.server.contenthandler.ContentHandler;
import org.drools.guvnor.server.contenthandler.ContentManager;
import org.drools.guvnor.server.contenthandler.ICanHasAttachment;
import org.drools.guvnor.server.contenthandler.IRuleAsset;
import org.drools.guvnor.server.contenthandler.IValidating;
import org.drools.guvnor.server.contenthandler.ModelContentHandler;
import org.drools.guvnor.server.repository.MailboxService;
import org.drools.guvnor.server.repository.UserInbox;
import org.drools.guvnor.server.security.AdminType;
import org.drools.guvnor.server.security.CategoryPathType;
import org.drools.guvnor.server.security.PackageNameType;
import org.drools.guvnor.server.security.PackageUUIDType;
import org.drools.guvnor.server.security.RoleTypes;
import org.drools.guvnor.server.selector.SelectorManager;
import org.drools.guvnor.server.util.AssetFormatHelper;
import org.drools.guvnor.server.util.AssetLockManager;
import org.drools.guvnor.server.util.BRMSSuggestionCompletionLoader;
import org.drools.guvnor.server.util.Discussion;
import org.drools.guvnor.server.util.ISO8601;
import org.drools.guvnor.server.util.LoggingHelper;
import org.drools.guvnor.server.util.MetaDataMapper;
import org.drools.guvnor.server.util.TableDisplayHandler;
import org.drools.ide.common.client.modeldriven.SuggestionCompletionEngine;
import org.drools.ide.common.client.modeldriven.testing.Scenario;
import org.drools.lang.descr.PackageDescr;
import org.drools.lang.descr.TypeDeclarationDescr;
import org.drools.repository.AssetHistoryIterator;
import org.drools.repository.AssetItem;
import org.drools.repository.AssetItemIterator;
import org.drools.repository.AssetItemPageResult;
import org.drools.repository.CategoryItem;
import org.drools.repository.PackageItem;
import org.drools.repository.PackageIterator;
import org.drools.repository.RepositoryFilter;
import org.drools.repository.RulesRepository;
import org.drools.repository.RulesRepository.DateQuery;
import org.drools.repository.RulesRepositoryAdministrator;
import org.drools.repository.RulesRepositoryException;
import org.drools.repository.StateItem;
import org.drools.repository.VersionableItem;
import org.drools.repository.security.PermissionManager;
import org.drools.rule.Package;
import org.drools.runtime.rule.ConsequenceException;
import org.drools.testframework.RuleCoverageListener;
import org.drools.testframework.ScenarioRunner;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.security.Identity;
import org.jboss.seam.web.Session;
import org.mvel2.MVEL;
import org.mvel2.templates.TemplateRuntime;

import com.google.gwt.user.client.rpc.SerializationException;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

/**
 * This is the implementation of the repository service to drive the GWT based
 * front end.
 * Generally requests for this are passed through from RepositoryServiceServlet - and Seam manages instances of this. 
 *
 * @author Michael Neale
 */
@Name("org.drools.guvnor.client.rpc.RepositoryService")
@AutoCreate
public class ServiceImplementation implements RepositoryService {

    /**
     * Maximum number of rules to display in "list rules in package" method
     */
    private static final int            MAX_RULES_TO_SHOW_IN_PACKAGE_LIST = 5000;

    @In
    public RulesRepository              repository;

    private static final long           serialVersionUID                  = 510l;

    private static final DateFormat     dateFormatter                     = DateFormat.getInstance();

    private static final LoggingHelper  log                               = LoggingHelper.getLogger( ServiceImplementation.class );

    private MetaDataMapper              metaDataMapper                    = new MetaDataMapper();

    /**
     * Used for a simple cache of binary packages to avoid serialization from
     * the database - for test scenarios.
     */
    public static Map<String, RuleBase> ruleBaseCache                     = Collections.synchronizedMap( new HashMap<String, RuleBase>() );

    /**
     * This is used for pushing messages back to the client.
     */
    private static Backchannel          backchannel                       = new Backchannel();

    public RulesRepository getRulesRepository() {
        return this.repository;
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String[] loadChildCategories(String categoryPath) {
        List<String> resultList = new ArrayList<String>();
        CategoryFilter filter = new CategoryFilter();

        CategoryItem item = repository.loadCategory( categoryPath );
        List children = item.getChildTags();
        for ( int i = 0; i < children.size(); i++ ) {
            String childCategoryName = ((CategoryItem) children.get( i )).getName();
            if ( filter.acceptNavigate( categoryPath, childCategoryName ) ) {
                resultList.add( childCategoryName );
            }
        }

        String[] resultArr = resultList.toArray( new String[resultList.size()] );
        return resultArr;
    }

    @WebRemote
    public Boolean createCategory(String path, String name, String description) {
        checkSecurityIsAdmin();

        log.info( "USER:" + getCurrentUserName() + " CREATING cateogory: [" + name + "] in path [" + path + "]" );

        if ( path == null || "".equals( path ) ) {
            path = "/";
        }
        path = cleanHTML( path );

        repository.loadCategory( path ).addCategory( name, description );
        repository.save();
        return Boolean.TRUE;
    }

    /**
     * This will create a new asset. It will be saved, but not checked in. The
     * initial state will be the draft state.
     * Returns the UUID of the asset.
     */
    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String createNewRule(String ruleName, String description, String initialCategory, String initialPackage, String format) throws SerializationException {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageNameType( initialPackage ), RoleTypes.PACKAGE_DEVELOPER );
        }

        log.info( "USER:" + getCurrentUserName() + " CREATING new asset name [" + ruleName + "] in package [" + initialPackage + "]" );

        try {

            PackageItem pkg = repository.loadPackage( initialPackage );
            AssetItem asset = pkg.addAsset( ruleName, description, initialCategory, format );

            applyPreBuiltTemplates( ruleName, format, asset );
            repository.save();

            push( "categoryChange", initialCategory );
            push( "packageChange", pkg.getName() );

            return asset.getUUID();
        } catch ( RulesRepositoryException e ) {
            if ( e.getCause() instanceof ItemExistsException ) {
                return "DUPLICATE";
            }
            log.error( "An error occurred creating new asset" + ruleName + "] in package [" + initialPackage + "]: ", e );
            throw new SerializationException( e.getMessage() );

        }

    }

    /**
     * This will create a new asset which refers to an existing asset
     */
    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String createNewImportedRule(String sharedAssetName, String initialPackage) throws SerializationException {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageNameType( initialPackage ), RoleTypes.PACKAGE_DEVELOPER );
        }

        log.info( "USER:" + repository.getSession().getUserID() + " CREATING shared asset imported from global area named [" + sharedAssetName + "] in package [" + initialPackage + "]" );

        try {
            PackageItem pkg = repository.loadPackage( initialPackage );
            AssetItem asset = pkg.addAssetImportedFromGlobalArea( sharedAssetName );
            repository.save();

            return asset.getUUID();
        } catch ( RulesRepositoryException e ) {
            if ( e.getCause() instanceof ItemExistsException ) {
                return "DUPLICATE";
            }
            log.error( "An error occurred creating shared asset" + sharedAssetName + "] in package [" + initialPackage + "]: ", e );
            throw new SerializationException( e.getMessage() );

        }

    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void deleteUncheckedRule(String uuid, String initialPackage) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new AdminType(), RoleTypes.PACKAGE_ADMIN );
        }

        AssetItem asset = repository.loadAssetByUUID( uuid );

        String pkgName = asset.getPackageName();

        asset.remove();

        repository.save();
        push( "packageChange", pkgName );
    }

    /**
     * For some format types, we add some sugar by adding a new template.
     */
    private void applyPreBuiltTemplates(String ruleName, String format, AssetItem asset) {
        if ( format.equals( AssetFormats.DSL_TEMPLATE_RULE ) ) {
            asset.updateContent( "when\n\nthen\n" );
        } else if ( format.equals( AssetFormats.FUNCTION ) ) {
            asset.updateContent( "function <returnType> " + ruleName + "(<args here>) {\n\n\n}" );
        } else if ( format.equals( AssetFormats.DSL ) ) {
            asset.updateContent( "[when]Condition sentence template {var}=" + "rule language mapping {var}\n" + "[then]Action sentence template=rule language mapping" );
        } else if ( format.equals( AssetFormats.DECISION_SPREADSHEET_XLS ) ) {
            asset.updateBinaryContentAttachment( this.getClass().getResourceAsStream( "/SampleDecisionTable.xls" ) );
            asset.updateBinaryContentAttachmentFileName( "SampleDecisionTable.xls" );
        } else if ( format.equals( AssetFormats.DRL ) ) {
            asset.updateContent( "when\n\t#conditions\nthen\n\t#actions" );
        } else if ( format.equals( AssetFormats.ENUMERATION ) ) {

        }
    }

    /**
     * Role-based Authorization check: This method only returns packages that the user has
     * permission to access. User has permission to access the particular package when:
     * The user has a package.readonly role or higher (i.e., package.admin, package.developer)
     * to this package.
     */
    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public PackageConfigData[] listPackages() {
        RepositoryFilter pf = new PackageFilter();
        return listPackages( false, pf );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public PackageConfigData[] listArchivedPackages() {
        RepositoryFilter pf = new PackageFilter();
        return listPackages( true, pf );
    }

    public PackageConfigData loadGlobalPackage() {
        PackageConfigData data = new PackageConfigData();
        PackageItem item = repository.loadGlobalArea();

        data.uuid = item.getUUID();
        data.header = getDroolsHeader( item );
        data.externalURI = item.getExternalURI();
        data.catRules = item.getCategoryRules();
        data.description = item.getDescription();
        data.archived = item.isArchived();
        data.name = item.getName();
        data.lastModified = item.getLastModified().getTime();
        data.dateCreated = item.getCreatedDate().getTime();
        data.checkinComment = item.getCheckinComment();
        data.lasContributor = item.getLastContributor();
        data.state = item.getStateDescription();
        data.isSnapshot = item.isSnapshot();

        if ( data.isSnapshot ) {
            data.snapshotName = item.getSnapshotName();
        }

        return data;
    }

    private PackageConfigData[] listPackages(boolean archive, RepositoryFilter filter) {
        List<PackageConfigData> result = new ArrayList<PackageConfigData>();
        PackageIterator pkgs = repository.listPackages();
        handleIteratePackages( archive, filter, result, pkgs );

        sortPackages( result );
        return result.toArray( new PackageConfigData[result.size()] );
    }

    private PackageConfigData[] listSubPackages(PackageItem parentPkg, boolean archive, RepositoryFilter filter) {
        List<PackageConfigData> children = new LinkedList<PackageConfigData>();

        PackageIterator pkgs = parentPkg.listSubPackages();
        handleIteratePackages( archive, filter, children, pkgs );

        sortPackages( children );
        return children.toArray( new PackageConfigData[children.size()] );
    }

    private void handleIteratePackages(boolean archive, RepositoryFilter filter, List<PackageConfigData> result, PackageIterator pkgs) {
        pkgs.setArchivedIterator( archive );
        while ( pkgs.hasNext() ) {
            PackageItem pkg = pkgs.next();

            PackageConfigData data = new PackageConfigData();
            data.uuid = pkg.getUUID();
            data.name = pkg.getName();
            data.archived = pkg.isArchived();
            handleIsPackagesListed( archive, filter, result, data );

            data.subPackages = listSubPackages( pkg, archive, filter );
        }
    }

    private void handleIsPackagesListed(boolean archive, RepositoryFilter filter, List<PackageConfigData> result, PackageConfigData data) {
        if ( !archive && (filter == null || filter.accept( data, RoleTypes.PACKAGE_READONLY )) ) {
            result.add( data );
        } else if ( archive && data.archived && (filter == null || filter.accept( data, RoleTypes.PACKAGE_READONLY )) ) {
            result.add( data );
        }
    }

    void sortPackages(List<PackageConfigData> result) {
        Collections.sort( result, new Comparator<PackageConfigData>() {

            public int compare(final PackageConfigData d1, final PackageConfigData d2) {
                return d1.name.compareTo( d2.name );
            }

        } );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    /**
     * loadRuleListForCategories
     *
     * Role-based Authorization check: This method only returns rules that the user has
     * permission to access. The user is considered to has permission to access the particular category when:
     * The user has ANALYST_READ role or higher (i.e., ANALYST) to this category
     */
    public TableDataResult loadRuleListForCategories(String categoryPath, int skip, int numRows, String tableConfig) throws SerializationException {

        // First check the user has permission to access this categoryPath.
        if ( Contexts.isSessionContextActive() ) {
            if ( !Identity.instance().hasPermission( new CategoryPathType( categoryPath ), RoleTypes.ANALYST_READ ) ) {

                TableDisplayHandler handler = new TableDisplayHandler( tableConfig );
                return handler.loadRuleListTable( new AssetItemPageResult() );
            }
        }

        AssetItemPageResult result = repository.findAssetsByCategory( categoryPath, false, skip, numRows );
        TableDisplayHandler handler = new TableDisplayHandler( tableConfig );
        return handler.loadRuleListTable( result );

    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public TableDataResult loadRuleListForState(String stateName, int skip, int numRows, String tableConfig) throws SerializationException {

        //TODO: May need to use a filter that acts on both package based and category based. 
        RepositoryFilter filter = new AssetItemFilter();
        AssetItemPageResult result = repository.findAssetsByState( stateName, false, skip, numRows, filter );
        TableDisplayHandler handler = new TableDisplayHandler( tableConfig );
        return handler.loadRuleListTable( result );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public TableConfig loadTableConfig(String listName) {
        TableDisplayHandler handler = new TableDisplayHandler( listName );
        return handler.loadTableConfig();
    }

    /**
     * This actually does the hard work of loading up an asset based on its
     * format.
     *
     * Role-based Authorization check: This method can be accessed if user has
     * following permissions:
     * 1. The user has a ANALYST_READ role or higher (i.e., ANALYST) and this role has permission 
     * to access the category which the asset belongs to.
     * Or.
     * 2. The user has a package.readonly role or higher (i.e., package.admin, package.developer)
     * and this role has permission to access the package which the asset belongs to.
     */
    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public RuleAsset loadRuleAsset(String uuid) throws SerializationException {

        long time = System.currentTimeMillis();

        AssetItem item = repository.loadAssetByUUID( uuid );
        RuleAsset asset = new RuleAsset();

        asset.uuid = uuid;

        // load standard meta data
        asset.metaData = populateMetaData( item );

        //Verify if the user has permission to access the asset through package based permission.
        //If failed, then verify if the user has permission to access the asset through category
        //based permission
        if ( Contexts.isSessionContextActive() ) {
            boolean passed = false;

            try {
                Identity.instance().checkPermission( new PackageNameType( asset.metaData.packageName ), RoleTypes.PACKAGE_READONLY );
            } catch ( RuntimeException e ) {
                if ( asset.metaData.categories.length == 0 ) {
                    Identity.instance().checkPermission( new CategoryPathType( null ), RoleTypes.ANALYST_READ );
                } else {
                    RuntimeException exception = null;

                    for ( String cat : asset.metaData.categories ) {
                        try {
                            Identity.instance().checkPermission( new CategoryPathType( cat ), RoleTypes.ANALYST_READ );
                            passed = true;
                        } catch ( RuntimeException re ) {
                            exception = re;
                        }
                    }
                    if ( !passed ) {
                        throw exception;
                    }
                }
            }
        }

        PackageItem pkgItem = item.getPackage();

        ContentHandler handler = ContentManager.getHandler( asset.metaData.format );
        handler.retrieveAssetContent( asset, pkgItem, item );

        asset.isreadonly = asset.metaData.hasSucceedingVersion;

        if ( pkgItem.isSnapshot() ) {
            asset.isreadonly = true;
        }

        log.debug( "Package: " + pkgItem.getName() + ", asset: " + item.getName() + ". Load time taken for asset: " + (System.currentTimeMillis() - time) );
        UserInbox.recordOpeningEvent( item );
        return asset;

    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public RuleAsset[] loadRuleAssets(String[] uuids) throws SerializationException {
        return loadRuleAssets( Arrays.asList( uuids ) );
    }

    RuleAsset[] loadRuleAssets(Collection<String> uuids) throws SerializationException {
        if ( uuids == null ) {
            return null;
        }
        Collection<RuleAsset> assets = new HashSet<RuleAsset>();

        for ( String uuid : uuids ) {
            assets.add( loadRuleAsset( uuid ) );
        }

        return assets.toArray( new RuleAsset[assets.size()] );
    }

    private RuleAsset loadAsset(AssetItem item) throws SerializationException {

        RuleAsset asset = new RuleAsset();
        asset.uuid = item.getUUID();
        asset.metaData = populateMetaData( item );
        ContentHandler handler = ContentManager.getHandler( asset.metaData.format );
        handler.retrieveAssetContent( asset, item.getPackage(), item );

        return asset;
    }

    /**
     * read in the meta data, populating all dublin core and versioning stuff.
     */
    MetaData populateMetaData(VersionableItem item) {
        MetaData meta = new MetaData();

        meta.status = (item.getState() != null) ? item.getState().getName() : "";

        metaDataMapper.copyToMetaData( meta, item );

        meta.createdDate = calendarToDate( item.getCreatedDate() );
        meta.lastModifiedDate = calendarToDate( item.getLastModified() );

        meta.hasPreceedingVersion = item.getPrecedingVersion() != null;
        meta.hasSucceedingVersion = item.getSucceedingVersion() != null;
        return meta;
    }

    /**
     * Populate meta data with asset specific info.
     */
    MetaData populateMetaData(AssetItem item) {
        MetaData meta = populateMetaData( (VersionableItem) item );

        meta.packageName = item.getPackageName();
        meta.packageUUID = item.getPackage().getUUID();
        meta.setBinary( item.isBinary() );

        List categories = item.getCategories();
        fillMetaCategories( meta, categories );
        meta.dateEffective = calendarToDate( item.getDateEffective() );
        meta.dateExpired = calendarToDate( item.getDateExpired() );
        return meta;

    }

    private void fillMetaCategories(MetaData meta, List categories) {
        meta.categories = new String[categories.size()];
        for ( int i = 0; i < meta.categories.length; i++ ) {
            CategoryItem cat = (CategoryItem) categories.get( i );
            meta.categories[i] = cat.getFullPath();
        }
    }

    private Date calendarToDate(Calendar createdDate) {
        if ( createdDate == null ) {
            return null;
        }
        return createdDate.getTime();
    }

    private Calendar dateToCalendar(Date date) {
        if ( date == null ) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        return cal;
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    /**
     *
     * Role-based Authorization check: This method can be accessed if user has
     * following permissions:
     * 1. The user has a Analyst role and this role has permission to access the category
     * which the asset belongs to.
     * Or.
     * 2. The user has a package.developer role or higher (i.e., package.admin)
     * and this role has permission to access the package which the asset belongs to.
     */
    public String checkinVersion(RuleAsset asset) throws SerializationException {

        //Verify if the user has permission to access the asset through package based permission.
        //If failed, then verify if the user has permission to access the asset through category
        //based permission
        if ( Contexts.isSessionContextActive() ) {
            boolean passed = false;

            try {
                Identity.instance().checkPermission( new PackageNameType( asset.metaData.packageName ), RoleTypes.PACKAGE_DEVELOPER );
            } catch ( RuntimeException e ) {
                if ( asset.metaData.categories.length == 0 ) {
                    Identity.instance().checkPermission( new CategoryPathType( null ), RoleTypes.ANALYST );
                } else {
                    RuntimeException exception = null;

                    for ( String cat : asset.metaData.categories ) {
                        try {
                            Identity.instance().checkPermission( new CategoryPathType( cat ), RoleTypes.ANALYST );
                            passed = true;
                        } catch ( RuntimeException re ) {
                            exception = re;
                        }
                    }
                    if ( !passed ) {
                        throw exception;
                    }
                }
            }
        }

        log.info( "USER:" + getCurrentUserName() + " CHECKING IN asset: [" + asset.metaData.name + "] UUID: [" + asset.uuid + "] " );

        AssetItem repoAsset = repository.loadAssetByUUID( asset.uuid );
        if ( isAssetUpdatedInRepository( asset, repoAsset ) ) {
            return "ERR: Unable to save this asset, as it has been recently updated by [" + repoAsset.getLastContributor() + "]";
        }

        MetaData meta = asset.metaData;

        metaDataMapper.copyFromMetaData( meta, repoAsset );

        updateEffectiveAndExpiredDate( repoAsset, meta );

        repoAsset.updateCategoryList( meta.categories );
        getContentHandler( repoAsset ).storeAssetContent( asset, repoAsset );

        if ( !(asset.metaData.format.equals( AssetFormats.TEST_SCENARIO )) || asset.metaData.format.equals( AssetFormats.ENUMERATION ) ) {
            PackageItem pkg = repoAsset.getPackage();
            pkg.updateBinaryUpToDate( false );
            ServiceImplementation.ruleBaseCache.remove( pkg.getUUID() );
        }
        repoAsset.checkin( meta.checkinComment );

        return repoAsset.getUUID();
    }

    private ContentHandler getContentHandler(AssetItem repoAsset) {
        return ContentManager.getHandler( repoAsset.getFormat() );
    }

    private void updateEffectiveAndExpiredDate(AssetItem repoAsset, MetaData meta) {
        repoAsset.updateDateEffective( dateToCalendar( meta.dateEffective ) );
        repoAsset.updateDateExpired( dateToCalendar( meta.dateExpired ) );
    }

    private boolean isAssetUpdatedInRepository(RuleAsset asset, AssetItem repoAsset) {
        return asset.metaData.lastModifiedDate.before( repoAsset.getLastModified().getTime() );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public TableDataResult loadAssetHistory(String uuid) throws SerializationException {

        List<TableDataRow> result = new ArrayList<TableDataRow>();

        AssetItem item = repository.loadAssetByUUID( uuid );

        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageUUIDType( item.getPackage().getUUID() ), RoleTypes.PACKAGE_READONLY );
        }

        AssetHistoryIterator it = item.getHistory();

        // MN Note: this uses the lazy iterator, but then loads the whole lot
        // up, and returns it.
        // The reason for this is that the GUI needs to show things in numeric
        // order by the version number.
        // When a version is restored, its previous version is NOT what you
        // thought it was - due to how JCR works
        // (its more like CVS then SVN). So to get a linear progression of
        // versions, we use the incrementing version number,
        // and load it all up and sort it. This is not ideal.
        // In future, we may do a "restore" instead just by copying content into
        // a new version, not restoring a node,
        // in which case the iterator will be in order (or you can just walk all
        // the way back).
        // So if there are performance problems with looking at lots of
        // historical versions, look at this nasty bit of code.
        while ( it.hasNext() ) {
            AssetItem historical = (AssetItem) it.next();
            long versionNumber = historical.getVersionNumber();
            if ( isHistory( item, versionNumber ) ) {
                result.add( createHistoricalRow( result, historical ) );
            }
        }

        if ( result.size() == 0 ) {
            return null;
        }
        TableDataResult table = new TableDataResult();
        table.data = result.toArray( new TableDataRow[result.size()] );

        return table;
    }

    private TableDataRow createHistoricalRow(List<TableDataRow> result, AssetItem historical) {
        TableDataRow tableDataRow = new TableDataRow();
        tableDataRow.id = historical.getVersionSnapshotUUID();
        tableDataRow.values = new String[4];
        tableDataRow.values[0] = Long.toString( historical.getVersionNumber() );
        tableDataRow.values[1] = historical.getCheckinComment();
        tableDataRow.values[2] = dateFormatter.format( historical.getLastModified().getTime() );
        tableDataRow.values[3] = historical.getStateDescription();
        return tableDataRow;
    }

    private boolean isHistory(AssetItem item, long versionNumber) {
        return versionNumber != 0 && versionNumber != item.getVersionNumber();
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public TableDataResult loadArchivedAssets(int skip, int numRows) throws SerializationException {
        List<TableDataRow> result = new ArrayList<TableDataRow>();
        RepositoryFilter filter = new AssetItemFilter();

        AssetItemIterator it = repository.findArchivedAssets();
        it.skip( skip );
        int count = 0;
        while ( it.hasNext() ) {

            AssetItem archived = (AssetItem) it.next();

            if ( filter.accept( archived, "read" ) ) {
                result.add( createArchivedRow( archived ) );
                count++;
            }
            if ( count == numRows ) {
                break;
            }
        }

        return createArchivedTable( result, it );
    }

    private TableDataResult createArchivedTable(List<TableDataRow> result, AssetItemIterator it) {
        TableDataResult table = new TableDataResult();
        table.data = result.toArray( new TableDataRow[result.size()] );
        table.currentPosition = it.getPosition();
        table.total = it.getSize();
        table.hasNext = it.hasNext();
        return table;
    }

    private TableDataRow createArchivedRow(AssetItem archived) {
        TableDataRow row = new TableDataRow();
        row.id = archived.getUUID();
        row.values = new String[5];
        row.values[0] = archived.getName();
        row.values[1] = archived.getFormat();
        row.values[2] = archived.getPackageName();
        row.values[3] = archived.getLastContributor();
        row.values[4] = Long.toString( archived.getLastModified().getTime().getTime() );
        return row;
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void restoreVersion(String versionUUID, String assetUUID, String comment) {
        AssetItem old = repository.loadAssetByUUID( versionUUID );
        AssetItem head = repository.loadAssetByUUID( assetUUID );

        log.info( "USER:" + getCurrentUserName() + " RESTORE of asset: [" + head.getName() + "] UUID: [" + head.getUUID() + "] with historical version number: [" + old.getVersionNumber() );

        repository.restoreHistoricalAsset( old, head, comment );

    }

    @WebRemote
    public String createPackage(String name, String description) throws RulesRepositoryException {
        checkSecurityIsAdmin();

        log.info( "USER: " + getCurrentUserName() + " CREATING package [" + name + "]" );
        PackageItem item = repository.createPackage( name, description );

        return item.getUUID();
    }

    @WebRemote
    public String createSubPackage(String name, String description, String parentNode) throws SerializationException {
        checkSecurityIsAdmin();

        log.info( "USER: " + getCurrentUserName() + " CREATING subPackage [" + name + "], parent [" + parentNode + "]" );
        PackageItem item = repository.createSubPackage( name, description, parentNode );
        return item.getUUID();
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public PackageConfigData loadPackageConfig(String uuid) {
        PackageItem item = repository.loadPackageByUUID( uuid );
        // the uuid passed in is the uuid of that deployment bundle, not the
        // package uudi.
        // we have to figure out the package name.
        checkSecurityNameTypePackageReadOnly( item );

        PackageConfigData data = createPackageConfigData( item );
        if ( data.isSnapshot ) {
            data.snapshotName = item.getSnapshotName();
        }
        return data;
    }

    private PackageConfigData createPackageConfigData(PackageItem item) {
        PackageConfigData data = new PackageConfigData();
        data.uuid = item.getUUID();
        data.header = getDroolsHeader( item );
        data.externalURI = item.getExternalURI();
        data.catRules = item.getCategoryRules();
        data.description = item.getDescription();
        data.archived = item.isArchived();
        data.name = item.getName();
        data.lastModified = item.getLastModified().getTime();
        data.dateCreated = item.getCreatedDate().getTime();
        data.checkinComment = item.getCheckinComment();
        data.lasContributor = item.getLastContributor();
        data.state = item.getStateDescription();
        data.isSnapshot = item.isSnapshot();
        return data;
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public ValidatedResponse savePackage(PackageConfigData data) throws SerializationException {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageUUIDType( data.uuid ), RoleTypes.PACKAGE_DEVELOPER );
        }

        log.info( "USER:" + getCurrentUserName() + " SAVING package [" + data.name + "]" );

        PackageItem item = repository.loadPackage( data.name );

        // If package is being unarchived.
        boolean unarchived = (data.archived == false && item.isArchived() == true);
        Calendar packageLastModified = item.getLastModified();

        updateDroolsHeader( data.header, item );
        updateCategoryRules( data, item );

        item.updateExternalURI( data.externalURI );
        item.updateDescription( data.description );
        item.archiveItem( data.archived );
        item.updateBinaryUpToDate( false );
        ServiceImplementation.ruleBaseCache.remove( data.uuid );
        item.checkin( data.description );

        // If package is archived, archive all the assets under it
        if ( data.archived ) {
            handleArchivedForSavePackage( data, item );
        } else if ( unarchived ) {
            handleUnarchivedForSavePackage( data, item, packageLastModified );
        }

        BRMSSuggestionCompletionLoader loader = new BRMSSuggestionCompletionLoader();
        loader.getSuggestionEngine( item );

        return validateBRMSSuggestionCompletionLoaderResponse( loader );
    }

    private ValidatedResponse validateBRMSSuggestionCompletionLoaderResponse(BRMSSuggestionCompletionLoader loader) {
        ValidatedResponse res = new ValidatedResponse();
        if ( loader.hasErrors() ) {
            res.hasErrors = true;
            String err = "";
            for ( Iterator iter = loader.getErrors().iterator(); iter.hasNext(); ) {
                err += (String) iter.next();
                if ( iter.hasNext() ) err += "\n";
            }
            res.errorHeader = "Package validation errors";
            res.errorMessage = err;
        }
        return res;
    }

    private void handleUnarchivedForSavePackage(PackageConfigData data, PackageItem item, Calendar packageLastModified) {
        for ( Iterator<AssetItem> iter = item.getAssets(); iter.hasNext(); ) {
            AssetItem assetItem = iter.next();
            // Unarchive the assets archived after the package 
            // ( == at the same time that the package was archived)
            if ( assetItem.getLastModified().compareTo( packageLastModified ) >= 0 ) {
                assetItem.archiveItem( false );
                assetItem.checkin( data.description );
            }
        }
    }

    private void handleArchivedForSavePackage(PackageConfigData data, PackageItem item) {
        for ( Iterator<AssetItem> iter = item.getAssets(); iter.hasNext(); ) {
            AssetItem assetItem = iter.next();
            if ( !assetItem.isArchived() ) {
                assetItem.archiveItem( true );
                assetItem.checkin( data.description );
            }
        }
    }

    private void updateCategoryRules(PackageConfigData data, PackageItem item) {
        KeyValueTO keyValueTO = convertMapToCsv( data.catRules );
        item.updateCategoryRules( keyValueTO.getKeys(), keyValueTO.getValues() );
    }

    // HashMap DOES NOT guarantee order in different iterations!
    private static KeyValueTO convertMapToCsv(final Map map) {
        StringBuilder keysBuilder = new StringBuilder();
        StringBuilder valuesBuilder = new StringBuilder();
        for ( Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            if ( keysBuilder.length() > 0 ) {
                keysBuilder.append(",");
            }

            if ( valuesBuilder.length() > 0 ) {
                valuesBuilder.append(",");
            }

            keysBuilder.append(entry.getKey());
            valuesBuilder.append(entry.getValue());
        }
        return new KeyValueTO( keysBuilder.toString(), valuesBuilder.toString() );
    }

    private static class KeyValueTO {
        private String keys;
        private String values;

        public KeyValueTO(final String keys, final String values) {
            this.keys = keys;
            this.values = values;
        }

        public String getKeys() {
            return keys;
        }

        public String getValues() {
            return values;
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public AssetPageResponse findAssetPage(AssetPageRequest request) throws SerializationException {
        log.debug( "Finding asset page of packageUuid (" + request.getPackageUuid() + ")" );
        long start = System.currentTimeMillis();
        PackageItem packageItem = repository.loadPackageByUUID( request.getPackageUuid() );
        AssetItemIterator it;
        if ( request.getFormatInList() != null ) {
            if ( request.getFormatIsRegistered() != null ) {
                throw new IllegalArgumentException( "Combining formatInList and formatIsRegistered is not yet supported." );
            } else {
                it = packageItem.listAssetsByFormat( request.getFormatInList() );
            }
        } else {
            if ( request.getFormatIsRegistered() != null ) {
                it = packageItem.listAssetsNotOfFormat( AssetFormatHelper.listRegisteredTypes() );
            } else {
                it = packageItem.queryAssets( "" );
            }
        }

        AssetPageResponse response = new AssetPageResponse();
        long totalRowSize = it.getSize();
        if ( totalRowSize > Integer.MAX_VALUE ) {
            throw new IllegalStateException( "The totalRowSize (" + totalRowSize + ") is too big." );
        }
        response.setTotalRowSize( (int) totalRowSize );
        it.skip( request.getStartRowIndex() );
        response.setStartRowIndex( request.getStartRowIndex() );

        List<AssetPageRow> rowList = fillAssetPageRowsForFindAssetPage( request, it );
        response.setAssetPageRowList( rowList );
        response.setLastPage( !it.hasNext() );

        long methodDuration = System.currentTimeMillis() - start;
        log.debug( "Found asset page of packageUuid (" + request.getPackageUuid() + ") in " + methodDuration + " ms." );
        return response;
    }

    private List<AssetPageRow> fillAssetPageRowsForFindAssetPage(AssetPageRequest request, AssetItemIterator it) {
        int pageSize = request.getPageSize();
        List<AssetPageRow> rowList = new ArrayList<AssetPageRow>( request.getPageSize() );

        while ( it.hasNext() && (pageSize < 0 || rowList.size() <= pageSize) ) {
            AssetItem assetItem = (AssetItem) it.next();
            AssetPageRow row = new AssetPageRow();
            row.setUuid( assetItem.getUUID() );
            row.setFormat( assetItem.getFormat() );
            row.setPackageName( assetItem.getPackageName() );
            row.setName( assetItem.getName() );
            row.setDescription( assetItem.getDescription() );
            row.setDescriptionAbbreviated(StringUtils.abbreviate(assetItem.getDescription(),80));
            row.setStateName( assetItem.getStateDescription() );
            row.setCreator( assetItem.getCreator() );
            row.setCreatedDate( assetItem.getCreatedDate().getTime() );
            row.setLastContributor( assetItem.getLastContributor() );
            row.setLastModified( assetItem.getLastModified().getTime() );
            row.setCategorySummary( assetItem.getCategorySummary() );
            row.setExternalSource( assetItem.getExternalSource() );
            rowList.add( row );
        }
        return rowList;
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public TableDataResult listAssetsWithPackageName(String packageName, String formats[], int skip, int numRows, String tableConfig) throws SerializationException {
        PackageItem pkg = repository.loadPackage( packageName );
        return listAssets( pkg.getUUID(), formats, skip, numRows, tableConfig );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public TableDataResult listAssets(String packageUuid, String formats[], int skip, int numRows, String tableConfig) throws SerializationException {
        log.debug( "Loading asset list for [" + packageUuid + "]" );
        if ( numRows == 0 ) {
            throw new DetailedSerializationException( "Unable to return zero results (bug)", "probably have the parameters around the wrong way, sigh..." );
        }
        long start = System.currentTimeMillis();
        PackageItem pkg = repository.loadPackageByUUID( packageUuid );
        AssetItemIterator it;
        if ( formats.length > 0 ) {
            it = pkg.listAssetsByFormat( formats );
        } else {
            it = pkg.listAssetsNotOfFormat( AssetFormatHelper.listRegisteredTypes() );
        }
        TableDisplayHandler handler = new TableDisplayHandler( tableConfig );
        log.debug( "time for asset list load: " + (System.currentTimeMillis() - start) );
        return handler.loadRuleListTable( it, skip, numRows );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public TableDataResult quickFindAsset(String searchText, boolean searchArchived, int skip, int numRows) throws SerializationException {
        String search = searchText.replace( '*', '%' );

        if ( !search.endsWith( "%" ) ) {
            search += "%";
        }

        List<AssetItem> resultList = new ArrayList<AssetItem>();

        long start = System.currentTimeMillis();
        AssetItemIterator it = repository.findAssetsByName( search, searchArchived );
        log.debug( "Search time: " + (System.currentTimeMillis() - start) );

        RepositoryFilter filter = new AssetItemFilter();

        while ( it.hasNext() ) {
            AssetItem ai = it.next();
            if ( filter.accept( ai, RoleTypes.PACKAGE_READONLY ) ) {
                resultList.add( ai );
            }
        }

        TableDisplayHandler handler = new TableDisplayHandler( "searchresults" );
        return handler.loadRuleListTable( resultList, skip, numRows );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public TableDataResult queryFullText(String text, boolean seekArchived, int skip, int numRows) throws SerializationException {
        if ( numRows == 0 ) {
            throw new DetailedSerializationException( "Unable to return zero results (bug)", "probably have the parameters around the wrong way, sigh..." );
        }
        AssetItemIterator it = repository.queryFullText( text, seekArchived );

        // Add filter for READONLY permission
        List<AssetItem> resultList = new ArrayList<AssetItem>();
        RepositoryFilter filter = new PackageFilter();

        while ( it.hasNext() ) {
            AssetItem ai = it.next();
            if ( checkPackagePermissionHelper( filter, ai, RoleTypes.PACKAGE_READONLY ) ) {
                resultList.add( ai );
            }
        }

        TableDisplayHandler handler = new TableDisplayHandler( "searchresults" );
        return handler.loadRuleListTable( resultList, skip, numRows );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public TableDataResult queryMetaData(final MetaDataQuery[] qr, Date createdAfter, Date createdBefore, Date modifiedAfter, Date modifiedBefore, boolean seekArchived, int skip, int numRows) throws SerializationException {
        if ( numRows == 0 ) {
            throw new DetailedSerializationException( "Unable to return zero results (bug)", "probably have the parameters around the wrong way, sigh..." );
        }

        Map<String, String[]> q = new HashMap<String, String[]>() {
            {
                for ( int i = 0; i < qr.length; i++ ) {
                    String vals = (qr[i].valueList == null) ? "" : qr[i].valueList.trim();
                    if ( vals.length() > 0 ) {
                        put( qr[i].attribute, vals.split( ",\\s?" ) );
                    }
                }
            }
        };

        DateQuery[] dates = new DateQuery[2];

        dates[0] = new DateQuery( "jcr:created", isoDate( createdAfter ), isoDate( createdBefore ) );
        dates[1] = new DateQuery( AssetItem.LAST_MODIFIED_PROPERTY_NAME, isoDate( modifiedAfter ), isoDate( modifiedBefore ) );
        AssetItemIterator it = repository.query( q, seekArchived, dates );
        // Add Filter to check Permission
        List<AssetItem> resultList = new ArrayList<AssetItem>();

        RepositoryFilter packageFilter = new PackageFilter();
        RepositoryFilter categoryFilter = new CategoryFilter();

        while ( it.hasNext() ) {
            AssetItem ai = it.next();
            if ( checkPackagePermissionHelper( packageFilter, ai, RoleTypes.PACKAGE_READONLY ) || checkCategoryPermissionHelper( categoryFilter, ai, RoleTypes.ANALYST_READ ) ) {
                resultList.add( ai );
            }
        }

        return new TableDisplayHandler( "searchresults" ).loadRuleListTable( resultList, skip, numRows );
    }

    private boolean checkPackagePermissionHelper(RepositoryFilter filter, AssetItem item, String roleType) {
        return filter.accept( getConfigDataHelper( item.getPackage().getUUID() ), roleType );
    }

    private boolean checkCategoryPermissionHelper(RepositoryFilter filter, AssetItem item, String roleType) {
        List<CategoryItem> tempCateList = item.getCategories();
        for ( Iterator<CategoryItem> i = tempCateList.iterator(); i.hasNext(); ) {
            CategoryItem categoryItem = i.next();

            if ( filter.accept( categoryItem.getName(), roleType ) ) {
                return true;
            }
        }

        return false;
    }

    private PackageConfigData getConfigDataHelper(String uuidStr) {
        PackageConfigData data = new PackageConfigData();
        data.uuid = uuidStr;
        return data;
    }

    private String isoDate(Date d) {
        if ( d != null ) {
            Calendar cal = Calendar.getInstance();
            cal.setTime( d );
            return ISO8601.format( cal );
        }
        return null;
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String createState(String name) throws SerializationException {
        log.info( "USER:" + getCurrentUserName() + " CREATING state: [" + name + "]" );
        try {
            name = cleanHTML( name );
            String uuid = repository.createState( name ).getNode().getUUID();
            repository.save();
            return uuid;
        } catch ( RepositoryException e ) {
            throw new SerializationException( "Unable to create the status." );
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void removeState(String name) throws SerializationException {
        log.info( "USER:" + getCurrentUserName() + " REMOVING state: [" + name + "]" );

        try {
            repository.loadState( name ).remove();
            repository.save();

        } catch ( RulesRepositoryException e ) {
            throw new DetailedSerializationException( "Unable to remove status. It is probably still used (even by archived items).", e.getMessage() );
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void renameState(String oldName, String newName) throws SerializationException {
        log.info( "USER:" + getCurrentUserName() + " RENAMING state: [" + oldName + "] to [" + newName + "]" );
        repository.renameState( oldName, newName );

    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String[] listStates() throws SerializationException {
        StateItem[] states = repository.listStates();
        String[] result = new String[states.length];
        for ( int i = 0; i < states.length; i++ ) {
            result[i] = states[i].getName();
        }
        return result;
    }

    /**
    *
    * Role-based Authorization check: This method can be accessed if user has
    * following permissions:
    * 1. The user has a Analyst role and this role has permission to access the category
    * which the asset belongs to.
    * Or.
    * 2. The user has a package.developer role or higher (i.e., package.admin)
    * and this role has permission to access the package which the asset belongs to.
    */
    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void changeState(String uuid, String newState, boolean wholePackage) {

        if ( !wholePackage ) {
            AssetItem asset = repository.loadAssetByUUID( uuid );

            //Verify if the user has permission to access the asset through package based permission.
            //If failed, then verify if the user has permission to access the asset through category
            //based permission
            if ( Contexts.isSessionContextActive() ) {
                boolean passed = false;

                try {
                    Identity.instance().checkPermission( new PackageUUIDType( asset.getPackage().getUUID() ), RoleTypes.PACKAGE_DEVELOPER );
                } catch ( RuntimeException e ) {
                    if ( asset.getCategories().size() == 0 ) {
                        Identity.instance().checkPermission( new CategoryPathType( null ), RoleTypes.ANALYST );
                    } else {
                        RuntimeException exception = null;

                        for ( CategoryItem cat : asset.getCategories() ) {
                            try {
                                Identity.instance().checkPermission( new CategoryPathType( cat.getName() ), RoleTypes.ANALYST );
                                passed = true;
                            } catch ( RuntimeException re ) {
                                exception = re;
                            }
                        }
                        if ( !passed ) {
                            throw exception;
                        }
                    }
                }
            }

            log.info( "USER:" + getCurrentUserName() + " CHANGING ASSET STATUS. Asset name, uuid: " + "[" + asset.getName() + ", " + asset.getUUID() + "]" + " to [" + newState + "]" );
            String oldState = asset.getStateDescription();
            asset.updateState( newState );

            push( "statusChange", oldState );
            push( "statusChange", newState );

            addToDiscussionForAsset( asset.getUUID(), oldState + " -> " + newState );
        } else {
            checkSecurityIsPackageDeveloper( uuid );

            PackageItem pkg = repository.loadPackageByUUID( uuid );
            log.info( "USER:" + getCurrentUserName() + " CHANGING Package STATUS. Asset name, uuid: " + "[" + pkg.getName() + ", " + pkg.getUUID() + "]" + " to [" + newState + "]" );
            pkg.changeStatus( newState );
        }
        repository.save();
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void changeAssetPackage(String uuid, String newPackage, String comment) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageNameType( newPackage ), RoleTypes.PACKAGE_DEVELOPER );
        }

        log.info( "USER:" + getCurrentUserName() + " CHANGING PACKAGE OF asset: [" + uuid + "] to [" + newPackage + "]" );
        repository.moveRuleItemPackage( newPackage, uuid, comment );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void promoteAssetToGlobalArea(String uuid) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageNameType( RulesRepository.RULE_GLOBAL_AREA ), RoleTypes.PACKAGE_DEVELOPER );
        }

        log.info( "USER:" + getCurrentUserName() + " CHANGING PACKAGE OF asset: [" + uuid + "] to [ globalArea ]" );
        repository.moveRuleItemPackage( RulesRepository.RULE_GLOBAL_AREA, uuid, "promote asset to globalArea" );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String copyAsset(String assetUUID, String newPackage, String newName) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageNameType( newPackage ), RoleTypes.PACKAGE_DEVELOPER );
        }

        log.info( "USER:" + getCurrentUserName() + " COPYING asset: [" + assetUUID + "] to [" + newName + "] in PACKAGE [" + newPackage + "]" );
        return repository.copyAsset( assetUUID, newPackage, newName );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public SnapshotInfo[] listSnapshots(String packageName) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageNameType( packageName ), RoleTypes.PACKAGE_DEVELOPER );
        }

        String[] snaps = repository.listPackageSnapshots( packageName );
        SnapshotInfo[] res = new SnapshotInfo[snaps.length];
        for ( int i = 0; i < snaps.length; i++ ) {
            PackageItem snap = repository.loadPackageSnapshot( packageName, snaps[i] );
            SnapshotInfo info = new SnapshotInfo();
            res[i] = info;
            info.comment = snap.getCheckinComment();
            info.name = snaps[i];
            info.uuid = snap.getUUID();
        }
        return res;
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void createPackageSnapshot(String packageName, String snapshotName, boolean replaceExisting, String comment) {
        checkSecurityIsPackageNameTypeAdmin( packageName );

        log.info( "USER:" + getCurrentUserName() + " CREATING PACKAGE SNAPSHOT for package: [" + packageName + "] snapshot name: [" + snapshotName );

        if ( replaceExisting ) {
            repository.removePackageSnapshot( packageName, snapshotName );
        }

        repository.createPackageSnapshot( packageName, snapshotName );
        PackageItem item = repository.loadPackageSnapshot( packageName, snapshotName );
        item.updateCheckinComment( comment );
        repository.save();

    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void copyOrRemoveSnapshot(String packageName, String snapshotName, boolean delete, String newSnapshotName) throws SerializationException {
        checkSecurityIsPackageNameTypeAdmin( packageName );

        if ( delete ) {
            log.info( "USER:" + getCurrentUserName() + " REMOVING SNAPSHOT for package: [" + packageName + "] snapshot: [" + snapshotName + "]" );
            repository.removePackageSnapshot( packageName, snapshotName );
        } else {
            if ( newSnapshotName.equals( "" ) ) {
                throw new SerializationException( "Need to have a new snapshot name." );
            }
            log.info( "USER:" + getCurrentUserName() + " COPYING SNAPSHOT for package: [" + packageName + "] snapshot: [" + snapshotName + "] to [" + newSnapshotName + "]" );

            repository.copyPackageSnapshot( packageName, snapshotName, newSnapshotName );
        }

    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void removeCategory(String categoryPath) throws SerializationException {
        log.info( "USER:" + getCurrentUserName() + " REMOVING CATEGORY path: [" + categoryPath + "]" );

        try {
            repository.loadCategory( categoryPath ).remove();
            repository.save();
        } catch ( RulesRepositoryException e ) {
            log.info( "Unable to remove category [" + categoryPath + "]. It is probably still used: " + e.getMessage() );

            throw new DetailedSerializationException( "Unable to remove category. It is probably still used.", e.getMessage() );
        }
    }

    @WebRemote
    public void clearRulesRepository() {
        checkSecurityIsAdmin();

        RulesRepositoryAdministrator admin = new RulesRepositoryAdministrator( repository.getSession() );
        admin.clearRulesRepository();
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public SuggestionCompletionEngine loadSuggestionCompletionEngine(String packageName) throws SerializationException {
        checkSecurityIsPackageReadOnly( packageName );

        SuggestionCompletionEngine result = null;
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            PackageItem pkg = repository.loadPackage( packageName );
            BRMSSuggestionCompletionLoader loader = null;
            List<JarInputStream> jars = BRMSPackageBuilder.getJars( pkg );
            if ( jars != null && !jars.isEmpty() ) {
                ClassLoader cl = BRMSPackageBuilder.createClassLoader( jars );

                Thread.currentThread().setContextClassLoader( cl );

                loader = new BRMSSuggestionCompletionLoader( cl );
            } else {
                loader = new BRMSSuggestionCompletionLoader();
            }

            result = loader.getSuggestionEngine( pkg );

        } catch ( RulesRepositoryException e ) {
            log.error( "An error occurred loadSuggestionCompletionEngine: " + e.getMessage() );
            throw new SerializationException( e.getMessage() );
        } finally {
            Thread.currentThread().setContextClassLoader( originalCL );
        }
        return result;
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public BuilderResult buildPackage(String packageUUID, boolean force) throws SerializationException {
        return buildPackage( packageUUID, force, null, null, null, false, null, null, false, null );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public BuilderResult buildPackage(String packageUUID, boolean force, String buildMode, String statusOperator, String statusDescriptionValue, boolean enableStatusSelector, String categoryOperator, String category, boolean enableCategorySelector, String customSelectorName) throws SerializationException {
        checkSecurityIsPackageDeveloper( packageUUID );
        PackageItem item = repository.loadPackageByUUID( packageUUID );
        try {
            return buildPackage( item, force, buildMode, statusOperator, statusDescriptionValue, enableStatusSelector, categoryOperator, category, enableCategorySelector, customSelectorName );
        } catch ( NoClassDefFoundError e ) {
            throw new DetailedSerializationException( "Unable to find a class that was needed when building the package  [" + e.getMessage() + "]", "Perhaps you are missing them from the model jars, or from the BRMS itself (lib directory)." );
        } catch ( UnsupportedClassVersionError e ) {
            throw new DetailedSerializationException( "Can not build the package. One or more of the classes that are needed were compiled with an unsupported Java version.", "For example the pojo classes were compiled with Java 1.6 and Guvnor is running on Java 1.5. [" + e.getMessage() + "]" );
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String[] getCustomSelectors() throws SerializationException {
        return SelectorManager.getInstance().getCustomSelectors();
    }

    private BuilderResult buildPackage(PackageItem item, boolean force) throws DetailedSerializationException {
        return buildPackage( item, force, null, null, null, false, null, null, false, null );
    }

    private BuilderResult buildPackage(PackageItem item, boolean force, String buildMode, String statusOperator, String statusDescriptionValue, boolean enableStatusSelector, String categoryOperator, String category, boolean enableCategorySelector, String selectorConfigName) throws DetailedSerializationException {
        if ( !force && item.isBinaryUpToDate() ) {
            // we can just return all OK if its up to date.
            return null;
        }
        ContentPackageAssembler asm = new ContentPackageAssembler( item, true, buildMode, statusOperator, statusDescriptionValue, enableStatusSelector, categoryOperator, category, enableCategorySelector, selectorConfigName );
        if ( asm.hasErrors() ) {
            BuilderResult result = new BuilderResult();
            result.setLines( generateBuilderResults( asm ) );
            return result;
        }
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutput out = new DroolsObjectOutputStream( bout );
            out.writeObject( asm.getBinaryPackage() );

            item.updateCompiledPackage( new ByteArrayInputStream( bout.toByteArray() ) );
            out.flush();
            out.close();

            updateBinaryPackage( item, asm );
            repository.save();
        } catch ( Exception e ) {
            e.printStackTrace();
            log.error( "An error occurred building the package [" + item.getName() + "]: " + e.getMessage() );
            throw new DetailedSerializationException( "An error occurred building the package.", e.getMessage() );
        }

        return null;

    }

    private void updateBinaryPackage(PackageItem item, ContentPackageAssembler asm) throws SerializationException {
        item.updateBinaryUpToDate( true );

        // adding the MapBackedClassloader that is the classloader from the rulebase classloader
        Collection<ClassLoader> loaders = asm.getBuilder().getRootClassLoader().getClassLoaders();
        RuleBaseConfiguration conf = new RuleBaseConfiguration( loaders.toArray( new ClassLoader[loaders.size()] ) );
        RuleBase rb = RuleBaseFactory.newRuleBase( conf );
        rb.addPackage( asm.getBinaryPackage() );
    }

    private BuilderResultLine[] generateBuilderResults(ContentPackageAssembler asm) {
        BuilderResultLine[] result = new BuilderResultLine[asm.getErrors().size()];
        for ( int i = 0; i < result.length; i++ ) {
            ContentAssemblyError err = asm.getErrors().get( i );
            BuilderResultLine res = new BuilderResultLine();
            res.assetName = err.getName();
            res.assetFormat = err.getFormat();
            res.message = err.getErrorReport();
            res.uuid = err.getUUID();
            result[i] = res;
        }
        return result;
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String buildPackageSource(String packageUUID) throws SerializationException {
        checkSecurityIsPackageDeveloper( packageUUID );

        PackageItem item = repository.loadPackageByUUID( packageUUID );
        ContentPackageAssembler asm = new ContentPackageAssembler( item, false );
        return asm.getDRL();
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String buildAssetSource(RuleAsset asset) throws SerializationException {
        checkSecurityIsPackageDeveloper( asset );

        ContentHandler handler = ContentManager.getHandler( asset.metaData.format );

        StringBuffer buf = new StringBuffer();
        if ( handler.isRuleAsset() ) {

            BRMSPackageBuilder builder = new BRMSPackageBuilder();
            // now we load up the DSL files
            PackageItem packageItem = repository.loadPackage( asset.metaData.packageName );
            builder.setDSLFiles( BRMSPackageBuilder.getDSLMappingFiles( packageItem, new BRMSPackageBuilder.DSLErrorEvent() {
                public void recordError(AssetItem asset, String message) {
                    // ignore at this point...
                }
            } ) );

            if ( asset.metaData.isBinary() ) {
                AssetItem item = repository.loadAssetByUUID( asset.uuid );

                handler.storeAssetContent( asset, item );
                ((IRuleAsset) handler).assembleDRL( builder, item, buf );
            } else {
                ((IRuleAsset) handler).assembleDRL( builder, asset, buf );
            }

        }

        return buf.toString();
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public BuilderResult buildAsset(RuleAsset asset) throws SerializationException {
        checkSecurityIsPackageDeveloper( asset );
        BuilderResult result = new BuilderResult();

        try {

            ContentHandler handler = ContentManager.getHandler( asset.metaData.format );

            if ( asset.metaData.isBinary() ) {
                AssetItem item = repository.loadAssetByUUID( asset.uuid );

                handler.storeAssetContent( asset, item );

                if ( handler instanceof IValidating ) {
                    return ((IValidating) handler).validateAsset( item );
                }

                ContentPackageAssembler asm = new ContentPackageAssembler( item );
                if ( !asm.hasErrors() ) {
                    return null;
                }
                result.setLines( generateBuilderResults( asm ) );

            } else {
                if ( handler instanceof IValidating ) {
                    return ((IValidating) handler).validateAsset( asset );
                }

                PackageItem packageItem = repository.loadPackageByUUID( asset.metaData.packageUUID );

                ContentPackageAssembler asm = new ContentPackageAssembler( asset, packageItem );
                if ( !asm.hasErrors() ) {
                    return null;
                }
                result.setLines( generateBuilderResults( asm ) );
            }
        } catch ( Exception e ) {
            log.error( "Unable to build asset.", e );
            result = new BuilderResult();

            BuilderResultLine res = new BuilderResultLine();
            res.assetName = asset.metaData.name;
            res.assetFormat = asset.metaData.format;
            res.message = "Unable to validate this asset. (Check log for detailed messages).";
            res.uuid = asset.uuid;
            result.getLines()[0] = res;

            return result;

        }
        return result;
    }

    @WebRemote
    public void copyPackage(String sourcePackageName, String destPackageName) throws SerializationException {
        checkSecurityIsAdmin();

        try {
            log.info( "USER:" + getCurrentUserName() + " COPYING package [" + sourcePackageName + "] to  package [" + destPackageName + "]" );

            repository.copyPackage( sourcePackageName, destPackageName );
        } catch ( RulesRepositoryException e ) {
            log.error( "Unable to copy package.", e );
            throw e;
        }

        // If we allow package owner to copy package, we will have to update the
        // permission store
        // for the newly copied package.
        // Update permission store
        /*
         * String copiedUuid = ""; try { PackageItem source =
         * repository.loadPackage( destPackageName ); copiedUuid =
         * source.getUUID(); } catch (RulesRepositoryException e) { log.error( e ); }
         * PackageBasedPermissionStore pbps = new PackageBasedPermissionStore();
         * pbps.addPackageBasedPermission(new PackageBasedPermission(copiedUuid,
         * Identity.instance().getPrincipal().getName(),
         * RoleTypes.PACKAGE_ADMIN));
         */
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String renameAsset(String uuid, String newName) {
        AssetItem item = repository.loadAssetByUUID( uuid );
        checkSecurityIsPackageDeveloper( item );

        return repository.renameAsset( uuid, newName );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void archiveAsset(String uuid) {
        archiveOrUnarchiveAsset( uuid, true );
    }

    public void unArchiveAsset(String uuid) {
        archiveOrUnarchiveAsset( uuid, false );
    }

    private void archiveOrUnarchiveAsset(String uuid, boolean archive) {
        try {
            AssetItem item = repository.loadAssetByUUID( uuid );

            checkSecurityIsPackageDeveloper( item );

            if ( item.getPackage().isArchived() ) {
                throw new RulesRepositoryException( "The package [" + item.getPackageName() + "] that asset [" + item.getName() + "] belongs to is archived. You need to unarchive it first." );
            }

            log.info( "USER:" + getCurrentUserName() + " ARCHIVING asset: [" + item.getName() + "] UUID: [" + item.getUUID() + "] " );

            try {
                ContentHandler handler = getContentHandler( item );
                if ( handler instanceof ICanHasAttachment ) {
                    ((ICanHasAttachment) handler).onAttachmentRemoved( item );
                }
            } catch ( IOException e ) {
                log.error( "Unable to remove asset attachment", e );
            }

            item.archiveItem( archive );
            PackageItem pkg = item.getPackage();
            pkg.updateBinaryUpToDate( false );
            ruleBaseCache.remove( pkg.getUUID() );
            if ( archive ) {
                item.checkin( "archived" );
            } else {
                item.checkin( "unarchived" );
            }

            push( "packageChange", pkg.getName() );

        } catch ( RulesRepositoryException e ) {
            log.error( "Unable to get item format.", e );
            throw e;
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void archiveAssets(String[] uuids, boolean value) {
        for ( String uuid : uuids ) {
            archiveOrUnarchiveAsset( uuid, value );
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void removeAsset(String uuid) {
        try {
            AssetItem item = repository.loadAssetByUUID( uuid );
            checkSecurityIsPackageDeveloper( item );

            item.remove();
            repository.save();
        } catch ( RulesRepositoryException e ) {
            log.error( "Unable to remove asset.", e );
            throw e;
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void removeAssets(String[] uuids) {
        for ( String uuid : uuids ) {
            removeAsset( uuid );
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void removePackage(String uuid) {
        checkSecurityIsPackageAdmin( uuid );

        try {
            PackageItem item = repository.loadPackageByUUID( uuid );
            log.info( "USER:" + getCurrentUserName() + " REMOVEING package [" + item.getName() + "]" );
            item.remove();
            repository.save();
        } catch ( RulesRepositoryException e ) {
            log.error( "Unable to remove package.", e );
            throw e;
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String renamePackage(String uuid, String newName) {
        checkSecurityIsPackageAdmin( uuid );
        log.info( "USER:" + getCurrentUserName() + " RENAMING package [UUID: " + uuid + "] to package [" + newName + "]" );

        return repository.renamePackage( uuid, newName );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public byte[] exportPackages(String packageName) {
        checkSecurityIsPackageNameTypeAdmin( packageName );
        log.info( "USER:" + getCurrentUserName() + " export package [name: " + packageName + "] " );

        byte[] result = null;

        try {
            result = repository.dumpPackageFromRepositoryXml( packageName );
        } catch ( PathNotFoundException e ) {
            throw new RulesRepositoryException( e );
        } catch ( IOException e ) {
            throw new RulesRepositoryException( e );
        } catch ( RepositoryException e ) {
            throw new RulesRepositoryException( e );
        }
        return result;
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    //TODO: Not working. GUVNOR-475
            public
            void importPackages(byte[] byteArray, boolean importAsNew) {
        repository.importPackageToRepository( byteArray, importAsNew );
    }

    @WebRemote
    public void rebuildSnapshots() throws SerializationException {
        checkSecurityIsAdmin();

        Iterator pkit = repository.listPackages();
        while ( pkit.hasNext() ) {
            PackageItem pkg = (PackageItem) pkit.next();
            String[] snaps = repository.listPackageSnapshots( pkg.getName() );
            for ( String snapName : snaps ) {
                PackageItem snap = repository.loadPackageSnapshot( pkg.getName(), snapName );
                BuilderResult res = this.buildPackage( snap.getUUID(), true );
                if ( res != null ) {
                    StringBuffer buf = new StringBuffer();
                    for ( int i = 0; i < res.getLines().length; i++ ) {
                        buf.append( res.getLines()[i].toString() );
                        buf.append( '\n' );
                    }
                    throw new DetailedSerializationException( "Unable to rebuild snapshot [" + snapName, buf.toString() + "]" );
                }
            }
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String[] listRulesInPackage(String packageName) throws SerializationException {

        // check security
        checkSecurityIsPackageReadOnly( packageName );

        // load package
        PackageItem item = repository.loadPackage( packageName );

        ContentPackageAssembler asm = new ContentPackageAssembler( item, false );

        List<String> result = new ArrayList<String>();
        try {

            String drl = asm.getDRL();
            if ( drl == null || "".equals( drl ) ) {
                return new String[0];
            } else {
                parseRulesToPackageList( asm, result );
            }

            return result.toArray( new String[result.size()] );
        } catch ( DroolsParserException e ) {
            log.error( "Unable to list rules in package", e );
            return new String[0];
        }
    }

    private void parseRulesToPackageList(ContentPackageAssembler asm, List<String> result) throws DroolsParserException {
        int count = 0;
        StringTokenizer stringTokenizer = new StringTokenizer( asm.getDRL(), "\n\r" );
        while ( stringTokenizer.hasMoreTokens() ) {
            String line = stringTokenizer.nextToken().trim();
            if ( line.startsWith( "rule " ) ) {
                String name = getRuleName( line );
                result.add( name );
                count++;
                if ( count == MAX_RULES_TO_SHOW_IN_PACKAGE_LIST ) {
                    result.add( "More then " + MAX_RULES_TO_SHOW_IN_PACKAGE_LIST + " rules." );
                    break;
                }
            }
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String[] listRulesInGlobalArea() throws SerializationException {
        return listRulesInPackage( RulesRepository.RULE_GLOBAL_AREA );
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public SingleScenarioResult runScenario(String packageName, Scenario scenario) throws SerializationException {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageNameType( packageName ), RoleTypes.PACKAGE_DEVELOPER );
        }

        return runScenario( packageName, scenario, null );

    }

    private SingleScenarioResult runScenario(String packageName, Scenario scenario, RuleCoverageListener coverage) throws SerializationException {
        PackageItem item = this.repository.loadPackage( packageName );
        SingleScenarioResult result = null;
        // nasty classloader needed to make sure we use the same tree the whole
        // time.
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            final RuleBase rb = loadCacheRuleBase( item );

            ClassLoader cl = ((InternalRuleBase) ServiceImplementation.ruleBaseCache.get( item.getUUID() )).getRootClassLoader();
            Thread.currentThread().setContextClassLoader( cl );
            result = runScenario( scenario, item, cl, rb, coverage );
        } catch ( Exception e ) {
            if ( e instanceof DetailedSerializationException ) {
                DetailedSerializationException err = (DetailedSerializationException) e;
                result = new SingleScenarioResult();
                if ( err.getErrs() != null ) {
                    result.result = new ScenarioRunResult( err.getErrs(), null );
                } else {
                    throw err;
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader( originalCL );
        }
        return result;
    }

    /*
     * Set the Rule base in a cache
     */
    private RuleBase loadCacheRuleBase(PackageItem item) throws DetailedSerializationException {
        RuleBase rb = null;
        if ( item.isBinaryUpToDate() && ServiceImplementation.ruleBaseCache.containsKey( item.getUUID() ) ) {
            rb = ServiceImplementation.ruleBaseCache.get( item.getUUID() );
        } else {
            // load up the classloader we are going to use
            List<JarInputStream> jars = BRMSPackageBuilder.getJars( item );
            ClassLoader buildCl = BRMSPackageBuilder.createClassLoader( jars );

            // we have to build the package, and try again.
            if ( item.isBinaryUpToDate() ) {
                rb = loadRuleBase( item, buildCl );
                ServiceImplementation.ruleBaseCache.put( item.getUUID(), rb );
            } else {
                BuilderResult result = this.buildPackage( item, false );
                if ( result == null || result.getLines().length == 0 ) {
                    rb = loadRuleBase( item, buildCl );
                    ServiceImplementation.ruleBaseCache.put( item.getUUID(), rb );
                } else throw new DetailedSerializationException( "Build error", result.getLines() );
            }

        }
        return rb;
    }

    private RuleBase loadRuleBase(PackageItem item, ClassLoader cl) throws DetailedSerializationException {
        try {
            return deserKnowledgebase( item, cl );
        } catch ( ClassNotFoundException e ) {
            log.error( "Unable to load rule base.", e );
            throw new DetailedSerializationException( "A required class was not found.", e.getMessage() );
        } catch ( Exception e ) {
            log.error( "Unable to load rule base.", e );
            log.info( "...but trying to rebuild binaries..." );
            try {
                BuilderResult res = this.buildPackage( item, true );
                if ( res != null && res.getLines().length > 0 ) {
                    log.error( "There were errors when rebuilding the knowledgebase." );
                    throw new DetailedSerializationException( "There were errors when rebuilding the knowledgebase.", "" );
                }
            } catch ( Exception e1 ) {
                log.error( "Unable to rebuild the rulebase: " + e.getMessage() );
                throw new DetailedSerializationException( "Unable to rebuild the rulebase.", e.getMessage() );
            }
            try {
                return deserKnowledgebase( item, cl );
            } catch ( Exception e2 ) {
                log.error( "Unable to reload knowledgebase: " + e.getMessage() );
                throw new DetailedSerializationException( "Unable to reload knowledgebase.", e.getMessage() );
            }

        }
    }

    private RuleBase deserKnowledgebase(PackageItem item, ClassLoader cl) throws IOException, ClassNotFoundException {
        RuleBase rb = RuleBaseFactory.newRuleBase( new RuleBaseConfiguration( cl ) );
        Package bin = (Package) DroolsStreamUtils.streamIn( item.getCompiledPackageBytes(), cl );
        rb.addPackage( bin );
        return rb;
    }

    private SingleScenarioResult runScenario(Scenario scenario, PackageItem item, ClassLoader cl, RuleBase rb, RuleCoverageListener coverage) throws DetailedSerializationException {

        // RuleBase rb = ruleBaseCache.get(item.getUUID());
        Package bin = rb.getPackages()[0];

        Set<String> imps = bin.getImports().keySet();
        Set<String> allImps = new HashSet<String>( imps );
        if ( bin.getGlobals() != null ) {
            for ( Iterator iterator = bin.getGlobals().keySet().iterator(); iterator.hasNext(); ) {
                allImps.add( bin.getGlobals().get( iterator.next() ) );
            }
        }
        allImps.add( bin.getName() + ".*" ); // need this for Generated beans to
        // work

        ClassTypeResolver res = new ClassTypeResolver( allImps, cl );
        SessionConfiguration sessionConfiguration = new SessionConfiguration();
        sessionConfiguration.setClockType( ClockType.PSEUDO_CLOCK );
        sessionConfiguration.setKeepReference( false );
        InternalWorkingMemory workingMemory = (InternalWorkingMemory) rb.newStatefulSession( sessionConfiguration, null );
        if ( coverage != null ) workingMemory.addEventListener( coverage );
        try {
            AuditLogReporter logger = new AuditLogReporter( workingMemory );
            new ScenarioRunner( scenario, res, workingMemory );
            SingleScenarioResult r = new SingleScenarioResult();
            r.auditLog = logger.buildReport();
            r.result = new ScenarioRunResult( null, scenario );
            return r;
        } catch ( ClassNotFoundException e ) {
            log.error( "Unable to load a required class.", e );
            throw new DetailedSerializationException( "Unable to load a required class.", e.getMessage() );
        } catch ( ConsequenceException e ) {
            String messageShort = "There was an error executing the consequence of rule [" + e.getRule().getName() + "]";
            String messageLong = e.getMessage();
            if ( e.getCause() != null ) {
                messageLong += "\nCAUSED BY " + e.getCause().getMessage();
            }

            log.error( messageShort + ": " + messageLong, e );
            throw new DetailedSerializationException( messageShort, messageLong );
        } catch ( Exception e ) {
            log.error( "Unable to run the scenario.", e );
            throw new DetailedSerializationException( "Unable to run the scenario.", e.getMessage() );
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public BulkTestRunResult runScenariosInPackage(String packageUUID) throws SerializationException {
        checkSecurityIsPackageDeveloper( packageUUID );
        PackageItem item = repository.loadPackageByUUID( packageUUID );
        return runScenariosInPackage( item );
    }

    public BulkTestRunResult runScenariosInPackage(PackageItem item) throws DetailedSerializationException, SerializationException {
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        ClassLoader cl = null;

        try {
            if ( item.isBinaryUpToDate() && ServiceImplementation.ruleBaseCache.containsKey( item.getUUID() ) ) {
                RuleBase rb = ServiceImplementation.ruleBaseCache.get( item.getUUID() );
                AbstractRuleBase arb = (AbstractRuleBase) rb;
                // load up the existing class loader from before
                cl = arb.getConfiguration().getClassLoader();
                Thread.currentThread().setContextClassLoader( cl );
            } else {
                // load up the classloader we are going to use
                List<JarInputStream> jars = BRMSPackageBuilder.getJars( item );
                cl = BRMSPackageBuilder.createClassLoader( jars );
                Thread.currentThread().setContextClassLoader( cl );

                // we have to build the package, and try again.
                if ( item.isBinaryUpToDate() ) {
                    ServiceImplementation.ruleBaseCache.put( item.getUUID(), loadRuleBase( item, cl ) );
                } else {
                    BuilderResult result = this.buildPackage( item, false );
                    if ( result == null || result.getLines().length == 0 ) {
                        ServiceImplementation.ruleBaseCache.put( item.getUUID(), loadRuleBase( item, cl ) );
                    } else {
                        return new BulkTestRunResult( result, null, 0, null );
                    }
                }
            }

            AssetItemIterator it = item.listAssetsByFormat( new String[]{AssetFormats.TEST_SCENARIO} );
            List<ScenarioResultSummary> resultSummaries = new ArrayList<ScenarioResultSummary>();
            RuleBase rb = ruleBaseCache.get( item.getUUID() );
            Package bin = rb.getPackages()[0];

            RuleCoverageListener coverage = new RuleCoverageListener( expectedRules( bin ) );

            while ( it.hasNext() ) {
                AssetItem as = it.next();
                if ( !as.getDisabled() ) {
                    RuleAsset asset = loadAsset( as );
                    Scenario sc = (Scenario) asset.content;
                    runScenario( item.getName(), sc, coverage );//runScenario(sc, res, workingMemory).scenario;

                    int[] totals = sc.countFailuresTotal();
                    resultSummaries.add( new ScenarioResultSummary( totals[0], totals[1], asset.metaData.name, asset.metaData.description, asset.uuid ) );
                }
            }

            ScenarioResultSummary[] summaries = resultSummaries.toArray( new ScenarioResultSummary[resultSummaries.size()] );

            BulkTestRunResult result = new BulkTestRunResult( null, resultSummaries.toArray( summaries ), coverage.getPercentCovered(), coverage.getUnfiredRules() );
            return result;

        } finally {
            Thread.currentThread().setContextClassLoader( originalCL );
        }
    }

    private HashSet<String> expectedRules(Package bin) {
        HashSet<String> h = new HashSet<String>();
        for ( int i = 0; i < bin.getRules().length; i++ ) {
            h.add( bin.getRules()[i].getName() );
        }
        return h;
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String[] listTypesInPackage(String packageUUID) throws SerializationException {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageUUIDType( packageUUID ), "package.readoly" );
        }

        PackageItem pkg = this.repository.loadPackageByUUID( packageUUID );
        List<String> res = new ArrayList<String>();
        AssetItemIterator it = pkg.listAssetsByFormat( new String[]{AssetFormats.MODEL, AssetFormats.DRL_MODEL} );

        JarInputStream jis = null;

        try {
            while ( it.hasNext() ) {
                AssetItem asset = (AssetItem) it.next();
                if ( !asset.isArchived() ) {
                    if ( asset.getFormat().equals( AssetFormats.MODEL ) ) {
                        jis = typesForModel( res, asset );
                    } else {
                        typesForOthers( res, asset );
                    }

                }
            }
            return res.toArray( new String[res.size()] );
        } catch ( IOException e ) {
            log.error( "Unable to read the jar files in the package: " + e.getMessage() );
            throw new DetailedSerializationException( "Unable to read the jar files in the package.", e.getMessage() );
        } finally {
            IOUtils.closeQuietly( jis );
        }

    }

    private void typesForOthers(List<String> res, AssetItem asset) {
        // its delcared model
        DrlParser parser = new DrlParser();
        try {
            PackageDescr desc = parser.parse( asset.getContent() );
            List<TypeDeclarationDescr> types = desc.getTypeDeclarations();
            for ( TypeDeclarationDescr typeDeclarationDescr : types ) {
                res.add( typeDeclarationDescr.getTypeName() );
            }
        } catch ( DroolsParserException e ) {
            log.error( "An error occurred parsing rule: " + e.getMessage() );

        }
    }

    private JarInputStream typesForModel(List<String> res, AssetItem asset) throws IOException {
        JarInputStream jis;
        jis = new JarInputStream( asset.getBinaryContentAttachment() );
        JarEntry entry = null;
        while ( (entry = jis.getNextJarEntry()) != null ) {
            if ( !entry.isDirectory() ) {
                if ( entry.getName().endsWith( ".class" ) ) {
                    res.add( ModelContentHandler.convertPathToName( entry.getName() ) );
                }
            }
        }
        return jis;
    }

    @WebRemote
    public LogEntry[] showLog() {
        checkSecurityIsAdmin();

        return LoggingHelper.getMessages();
    }

    @WebRemote
    public void cleanLog() {
        checkSecurityIsAdmin();

        LoggingHelper.cleanLog();
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void renameCategory(String fullPathAndName, String newName) {
        repository.renameCategory( fullPathAndName, newName );
    }

    public static String getDroolsHeader(PackageItem pkg) {
        if ( pkg.containsAsset( "drools" ) ) {
            return pkg.loadAsset( "drools" ).getContent();
        } else {
            return "";
        }
    }

    public static void updateDroolsHeader(String string, PackageItem pkg) {
        pkg.checkout();
        AssetItem conf;
        if ( pkg.containsAsset( "drools" ) ) {
            conf = pkg.loadAsset( "drools" );
            conf.updateContent( string );

            conf.checkin( "" );
        } else {
            conf = pkg.addAsset( "drools", "" );
            conf.updateFormat( "package" );
            conf.updateContent( string );

            conf.checkin( "" );
        }

    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public String[] loadDropDownExpression(String[] valuePairs, String expression) {
        Map<String, String> context = new HashMap<String, String>();
        for ( int i = 0; i < valuePairs.length; i++ ) {
            if ( valuePairs[i] == null ) {
                return new String[0];
            }
            String[] pair = valuePairs[i].split( "=" );
            context.put( pair[0], pair[1] );
        }
        // first interpolate the pairs
        expression = (String) TemplateRuntime.eval( expression, context );

        // now we can eval it for real...
        Object result = MVEL.eval( expression );
        if ( result instanceof String[] ) {
            return (String[]) result;
        } else if ( result instanceof List ) {
            List l = (List) result;
            String[] xs = new String[l.size()];
            for ( int i = 0; i < xs.length; i++ ) {
                Object el = l.get( i );
                xs[i] = el.toString();
            }
            return xs;
        } else {
            return null;
        }
    }

    @WebRemote
    @Restrict("#{identity.loggedIn}")
    public void rebuildPackages() throws SerializationException {
        Iterator pkit = repository.listPackages();
        StringBuffer errs = new StringBuffer();
        while ( pkit.hasNext() ) {
            PackageItem pkg = (PackageItem) pkit.next();
            try {
                BuilderResult res = this.buildPackage( pkg.getUUID(), true );
                if ( res != null ) {
                    errs.append( "Unable to build package name [" + pkg.getName() + "]\n" );
                    StringBuffer buf = new StringBuffer();
                    for ( int i = 0; i < res.getLines().length; i++ ) {
                        buf.append( res.getLines()[i].toString() );
                        buf.append( '\n' );
                    }
                    log.warn( buf.toString() );

                }
            } catch ( Exception e ) {
                log.error( "An error occurred building package [" + pkg.getName() + "]\n" );
                errs.append( "An error occurred building package [" + pkg.getName() + "]\n" );
            }
        }

        if ( errs.toString().length() > 0 ) {
            throw new DetailedSerializationException( "Unable to rebuild all packages.", errs.toString() );
        }
    }

    @Restrict("#{identity.loggedIn}")
    public Map<String, List<String>> listUserPermissions() {
        checkSecurityIsAdmin();

        PermissionManager pm = new PermissionManager( repository );
        return pm.listUsers();
    }

    @Restrict("#{identity.loggedIn}")
    public Map<String, List<String>> retrieveUserPermissions(String userName) {
        checkSecurityIsAdmin();

        PermissionManager pm = new PermissionManager( repository );
        return pm.retrieveUserPermissions( userName );
    }

    @Restrict("#{identity.loggedIn}")
    public void updateUserPermissions(String userName, Map<String, List<String>> perms) {
        checkSecurityIsAdmin();

        PermissionManager pm = new PermissionManager( repository );

        log.info( "Updating user permissions for userName [" + userName + "] to [" + perms + "]" );
        pm.updateUserPermissions( userName, perms );
        repository.save();
    }

    @Restrict("#{identity.loggedIn}")
    public String[] listAvailablePermissionTypes() {
        checkSecurityIsAdmin();

        return RoleTypes.listAvailableTypes();
    }

    @Restrict("#{identity.loggedIn}")
    public void deleteUser(String userName) {
        log.info( "Removing user permissions for user name [" + userName + "]" );
        PermissionManager pm = new PermissionManager( repository );
        pm.removeUserPermissions( userName );
        repository.save();
    }

    @Restrict("#{identity.loggedIn}")
    public void createUser(String userName) {
        log.info( "Creating user permissions, user name [" + userName + "]" );
        PermissionManager pm = new PermissionManager( repository );
        pm.createUser( userName );
        repository.save();
    }

    /* (non-Javadoc)
    * @see org.drools.guvnor.client.rpc.RepositoryService#getAssetLockerUserName(java.lang.String)
    */
    @Restrict("#{identity.loggedIn}")
    public String getAssetLockerUserName(String uuid) {
        AssetLockManager alm = AssetLockManager.instance();

        String userName = alm.getAssetLockerUserName( uuid );

        log.info( "Asset locked by [" + userName + "]" );

        return userName;
    }

    /* (non-Javadoc)
     * @see org.drools.guvnor.client.rpc.RepositoryService#lockAsset(java.lang.String)
     */
    @Restrict("#{identity.loggedIn}")
    public void lockAsset(String uuid) {
        AssetLockManager alm = AssetLockManager.instance();

        String userName;
        if ( Contexts.isApplicationContextActive() ) {
            userName = Identity.instance().getUsername();
        } else {
            userName = "anonymous";
        }

        log.info( "Locking asset uuid=" + uuid + " for user [" + userName + "]" );

        alm.lockAsset( uuid, userName );
    }

    /* (non-Javadoc)
     * @see org.drools.guvnor.client.rpc.RepositoryService#unLockAsset(java.lang.String)
     */
    @Restrict("#{identity.loggedIn}")
    public void unLockAsset(String uuid) {
        AssetLockManager alm = AssetLockManager.instance();
        log.info( "Unlocking asset [" + uuid + "]" );
        alm.unLockAsset( uuid );
    }

    @Restrict("#{identity.loggedIn}")
    public void installSampleRepository() throws SerializationException {
        checkIfADMIN();
        repository.importRepository( this.getClass().getResourceAsStream( "/mortgage-sample-repository.xml" ) );
        this.rebuildPackages();
        this.rebuildSnapshots();
    }

    @Restrict("#{identity.loggedIn}")
    public List<DiscussionRecord> loadDiscussionForAsset(String assetId) {
        return new Discussion().fromString( getRulesRepository().loadAssetByUUID( assetId ).getStringProperty( Discussion.DISCUSSION_PROPERTY_KEY ) );
    }

    @Restrict("#{identity.loggedIn}")
    public List<DiscussionRecord> addToDiscussionForAsset(String assetId, String comment) {
        RulesRepository repo = getRulesRepository();
        AssetItem asset = repo.loadAssetByUUID( assetId );
        Discussion dp = new Discussion();
        List<DiscussionRecord> discussion = dp.fromString( asset.getStringProperty( Discussion.DISCUSSION_PROPERTY_KEY ) );
        discussion.add( new DiscussionRecord( repo.getSession().getUserID(), StringEscapeUtils.escapeXml( comment ) ) );
        asset.updateStringProperty( dp.toString( discussion ), Discussion.DISCUSSION_PROPERTY_KEY, false );
        repo.save();

        push( "discussion", assetId );

        MailboxService.getInstance().recordItemUpdated( asset );

        return discussion;
    }

    @Restrict("#{identity.loggedIn}")
    public void clearAllDiscussionsForAsset(final String assetId) {
        checkIfADMIN();
        RulesRepository repo = getRulesRepository();
        AssetItem asset = repo.loadAssetByUUID( assetId );
        asset.updateStringProperty( "", "discussion" );
        repo.save();

        push( "discussion", assetId );

    }

    @Restrict("#{identity.loggedIn}")
    public TableDataResult loadInbox(String inboxName) throws DetailedSerializationException {
        try {
            UserInbox ib = new UserInbox( repository );
            if ( inboxName.equals( ExplorerNodeConfig.RECENT_VIEWED_ID ) ) {
                return UserInbox.toTable( ib.loadRecentOpened(), false );
            } else if ( inboxName.equals( ExplorerNodeConfig.RECENT_EDITED_ID ) ) {
                return UserInbox.toTable( ib.loadRecentEdited(), false );
            } else {
                return UserInbox.toTable( ib.loadIncoming(), true );
            }
        } catch ( Exception e ) {
            log.error( "Unable to load Inbox: " + e.getMessage() );
            throw new DetailedSerializationException( "Unable to load Inbox", e.getMessage() );
        }
    }

    /**
     * Pushes a message back to (all) clients.
     */
    private void push(String messageType, String message) {
        backchannel.publish( new PushResponse( messageType, message ) );
    }

    public List<PushResponse> subscribe() {
        if ( Contexts.isApplicationContextActive() && !Session.instance().isInvalid() ) {
            try {
                return backchannel.await( getCurrentUserName() );
            } catch ( InterruptedException e ) {
                return new ArrayList<PushResponse>();
            }
        } else {
            return new ArrayList<PushResponse>();
        }
    }

    private String getCurrentUserName() {
        return repository.getSession().getUserID();
    }

    private void checkIfADMIN() {
        if ( Contexts.isApplicationContextActive() ) {
            Identity.instance().checkPermission( new AdminType(), RoleTypes.ADMIN );
        }
    }

    public String cleanHTML(String s) {
        return s.replace( "<", "&lt;" ).replace( ">", "&gt;" );
    }

    public SnapshotDiffs compareSnapshots(String packageName, String firstSnapshotName, String secondSnapshotName) {
        SnapshotDiffs diffs = new SnapshotDiffs();
        List<SnapshotDiff> list = new ArrayList<SnapshotDiff>();

        PackageItem leftPackage = repository.loadPackageSnapshot( packageName, firstSnapshotName );
        PackageItem rightPackage = repository.loadPackageSnapshot( packageName, secondSnapshotName );

        // Older one has to be on the left.
        if ( isRightOlderThanLeft( leftPackage, rightPackage ) ) {
            PackageItem temp = leftPackage;
            leftPackage = rightPackage;
            rightPackage = temp;

            diffs.leftName = secondSnapshotName;
            diffs.rightName = firstSnapshotName;
        } else {
            diffs.leftName = firstSnapshotName;
            diffs.rightName = secondSnapshotName;
        }

        Iterator<AssetItem> leftExistingIter = leftPackage.getAssets();
        while ( leftExistingIter.hasNext() ) {
            AssetItem left = leftExistingIter.next();
            if ( isPackageItemDeleted( rightPackage, left ) ) {
                SnapshotDiff diff = new SnapshotDiff();

                diff.name = left.getName();
                diff.diffType = SnapshotDiff.TYPE_DELETED;
                diff.leftUuid = left.getUUID();

                list.add( diff );
            }
        }

        Iterator<AssetItem> rightExistingIter = rightPackage.getAssets();
        while ( rightExistingIter.hasNext() ) {
            AssetItem right = rightExistingIter.next();
            AssetItem left = null;
            if ( right != null && leftPackage.containsAsset( right.getName() ) ) {
                left = leftPackage.loadAsset( right.getName() );
            }

            // Asset is deleted or added
            if ( right == null || left == null ) {
                SnapshotDiff diff = new SnapshotDiff();

                if ( left == null ) {
                    diff.name = right.getName();
                    diff.diffType = SnapshotDiff.TYPE_ADDED;
                    diff.rightUuid = right.getUUID();
                }

                list.add( diff );
            } else if ( isAssetArchivedOrRestored( right, left ) ) { // Has the asset been archived or restored
                SnapshotDiff diff = new SnapshotDiff();

                diff.name = right.getName();
                diff.leftUuid = left.getUUID();
                diff.rightUuid = right.getUUID();

                if ( left.isArchived() ) {
                    diff.diffType = SnapshotDiff.TYPE_RESTORED;
                } else {
                    diff.diffType = SnapshotDiff.TYPE_ARCHIVED;
                }

                list.add( diff );
            } else if ( isAssetItemUpdated( right, left ) ) { // Has the asset been updated
                SnapshotDiff diff = new SnapshotDiff();

                diff.name = right.getName();
                diff.leftUuid = left.getUUID();
                diff.rightUuid = right.getUUID();
                diff.diffType = SnapshotDiff.TYPE_UPDATED;

                list.add( diff );
            }
        }

        diffs.diffs = list.toArray( new SnapshotDiff[list.size()] );
        return diffs;
    }

    private boolean isAssetArchivedOrRestored(AssetItem right, AssetItem left) {
        return right.isArchived() != left.isArchived();
    }

    private boolean isAssetItemUpdated(AssetItem right, AssetItem left) {
        return right.getLastModified().compareTo( left.getLastModified() ) != 0;
    }

    private boolean isPackageItemDeleted(PackageItem rightPackage, AssetItem left) {
        return !rightPackage.containsAsset( left.getName() );
    }

    private boolean isRightOlderThanLeft(PackageItem leftPackage, PackageItem rightPackage) {
        return leftPackage.getLastModified().compareTo( rightPackage.getLastModified() ) > 0;
    }

    /**
     * Load and process the repository configuration templates.
     */
    public String processTemplate(String name, Map<String, Object> data) {
        try {
            Configuration cfg = new Configuration();
            cfg.setObjectWrapper( new DefaultObjectWrapper() );
            cfg.setTemplateUpdateDelay( 0 );

            Template temp = new Template( name, new InputStreamReader( ServiceImplementation.class.getResourceAsStream( "/repoconfig/" + name + ".xml" ) ), cfg );
            StringWriter strw = new StringWriter();
            temp.process( data, strw );
            return StringEscapeUtils.escapeXml( strw.toString() );
        } catch ( Exception e ) {
            return "";
        }
    }

    /**
     * Check to see if app context is active (not in hosted)
     */
    public Boolean isHostedMode() {
        Boolean hm = Contexts.isApplicationContextActive() ? Boolean.FALSE : Boolean.TRUE;
        return hm;
    }

    private void checkSecurityIsPackageNameTypeAdmin(String packageName) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageNameType( packageName ), RoleTypes.PACKAGE_ADMIN );
        }
    }

    private void checkSecurityIsPackageDeveloper(String packageUUID) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageUUIDType( packageUUID ), RoleTypes.PACKAGE_DEVELOPER );
        }
    }

    private void checkSecurityIsPackageDeveloper(RuleAsset asset) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageNameType( asset.metaData.packageName ), RoleTypes.PACKAGE_DEVELOPER );
        }
    }

    private void checkSecurityIsPackageReadOnly(String packageName) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageNameType( packageName ), RoleTypes.PACKAGE_READONLY );
        }
    }

    private void checkSecurityIsPackageDeveloper(AssetItem item) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageUUIDType( item.getPackage().getUUID() ), RoleTypes.PACKAGE_DEVELOPER );
        }
    }

    private void checkSecurityIsPackageAdmin(String uuid) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageUUIDType( uuid ), RoleTypes.PACKAGE_ADMIN );
        }
    }

    private void checkSecurityIsAdmin() {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new AdminType(), RoleTypes.ADMIN );
        }
    }

    private void checkSecurityNameTypePackageReadOnly(PackageItem item) {
        if ( Contexts.isSessionContextActive() ) {
            Identity.instance().checkPermission( new PackageNameType( item.getName() ), RoleTypes.PACKAGE_READONLY );
        }
    }

}
