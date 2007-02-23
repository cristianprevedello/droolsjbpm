package org.drools.brms.server;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpSession;

import org.drools.brms.client.common.AssetFormats;
import org.drools.brms.client.rpc.MetaData;
import org.drools.brms.client.rpc.PackageConfigData;
import org.drools.brms.client.rpc.RepositoryService;
import org.drools.brms.client.rpc.RuleAsset;
import org.drools.brms.client.rpc.TableConfig;
import org.drools.brms.client.rpc.TableDataResult;
import org.drools.brms.client.rpc.TableDataRow;
import org.drools.brms.server.util.MetaDataMapper;
import org.drools.brms.server.util.RepositoryManager;
import org.drools.brms.server.util.TableDisplayHandler;
import org.drools.repository.AssetHistoryIterator;
import org.drools.repository.AssetItem;
import org.drools.repository.AssetItemIterator;
import org.drools.repository.CategoryItem;
import org.drools.repository.PackageItem;
import org.drools.repository.RulesRepository;
import org.drools.repository.RulesRepositoryException;
import org.drools.repository.StateItem;
import org.drools.repository.VersionableItem;

import com.google.gwt.user.client.rpc.SerializableException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/** 
 * This is the implementation of the repository service to drive the GWT based front end.
 * TODO: refactor this to use "Action" pattern or SSB pattern for 
 * transaction demarcation.
 * 
 * @author Michael Neale
 */
public class JBRMSServiceServlet extends RemoteServiceServlet
    implements
    RepositoryService {

    private static final long serialVersionUID = 3150768417428383474L;
    private static final DateFormat dateFormatter = DateFormat.getInstance();
    



    private MetaDataMapper metaDataMapper;

    
    public String[] loadChildCategories(String categoryPath) {

        CategoryItem item = getRulesRepository().loadCategory( categoryPath );
        List children = item.getChildTags();
        String[] list = new String[children.size()];
        for ( int i = 0; i < list.length; i++ ) {
            list[i] = ((CategoryItem) children.get( i )).getName();
        }
        return list;

    }

    public Boolean createCategory(String path,
                                  String name,
                                  String description) {
        
        if (path == null || "".equals(path)) {
            path = "/";
        }
        CategoryItem item = getRulesRepository().loadCategory( path );
        item.addCategory( name, description );
        return Boolean.TRUE;
    }
    
    
    /**
     * This will create a new asset. It will be saved, but not checked in.
     * The initial state will be the draft state.
     */
    public String createNewRule(String ruleName,
                                 String description,
                                 String initialCategory,
                                 String initialPackage,
                                 String format) throws SerializableException {        
        try {
            PackageItem pkg = getRulesRepository().loadPackage( initialPackage );
            AssetItem asset = pkg.addAsset( ruleName, description, initialCategory, format );
            
            if (format.equals( AssetFormats.DSL_TEMPLATE_RULE )) {
                asset.updateContent( "when\n\nthen\n" );
            } else if (format.equals( AssetFormats.FUNCTION )) {
                asset.updateContent( "function " + ruleName + "(<args here>)\n\n\nend" );
            } else if (format.equals( AssetFormats.DSL )) {
                asset.updateContent( "[when]Condition sentence template {var}=" +
                        "rule language mapping {var}\n" +
                        "[then]Action sentence template=rule language mapping");
            }
            getRulesRepository().save();
            return asset.getUUID();
        } catch (RulesRepositoryException e) {
            throw new SerializableException(e.getMessage());
        }

    }

    public String[] listRulePackages() {
        Iterator pkgs = getRulesRepository().listPackages();
        List result = new ArrayList();
        while(pkgs.hasNext()) {
            PackageItem pkg = (PackageItem) pkgs.next();
            result.add( pkg.getName() );
        }
        String[] resultArr = (String[]) result.toArray( new String[result.size()] );
        Arrays.sort( resultArr );
        return resultArr;
    }

    




    public TableDataResult loadRuleListForCategories(String categoryPath) throws SerializableException {
        long start = System.currentTimeMillis();
        RulesRepository repo = getRulesRepository();

        List list = repo.findAssetsByCategory( categoryPath );
        TableDisplayHandler handler = new TableDisplayHandler();
        System.out.println("time for load: " + (System.currentTimeMillis() - start) );
        return handler.loadRuleListTable( list.iterator(), -1 );
        
    }

    public TableConfig loadTableConfig(String listName) {
        TableDisplayHandler handler = new TableDisplayHandler();
        return handler.loadTableConfig(listName);
        
    }


    
    /** Get the rule repository for the "current" user */
    RulesRepository getRulesRepository() {
        RepositoryManager helper = new RepositoryManager();
        return helper.getRepositoryFrom( getSession() );
    }

    private HttpSession getSession() {
        return this.getThreadLocalRequest().getSession();
    }







    /**
     * This actually does the hard work of loading up an asset based
     * on its format.
     */
    public RuleAsset loadRuleAsset(String uuid) throws SerializableException {
        RulesRepository repo = getRulesRepository();
        AssetItem item = repo.loadAssetByUUID( uuid );
        RuleAsset asset = new RuleAsset();
        asset.uuid = uuid;

        
        //load standard meta data
        asset.metaData = populateMetaData( item );
        
        //load the content
        AssetContentFormatHandler handler = new AssetContentFormatHandler();
        handler.retrieveAssetContent(asset, item);
        
        return asset;
    }
    

    /** 
     * read in the meta data, populating all dublin core and versioning stuff.
     */
    MetaData populateMetaData(VersionableItem item) {
        MetaData meta = new MetaData();
        
        meta.state = (item.getState() != null) ? item.getState().getName() : "";

        getMetaDataMapper().copyToMetaData( meta, item );
        
        meta.createdDate = calendarToDate(item.getCreatedDate());
        meta.lastModifiedDate = calendarToDate( item.getLastModified() );
        
        return meta;
    }
    
    /**
     * Populate meta data with asset specific info.
     */
    MetaData populateMetaData(AssetItem item) {
        MetaData meta = populateMetaData( (VersionableItem ) item);
        meta.packageName = item.getPackageName();
        
        List cats = item.getCategories();
        meta.categories = new String[cats.size()];
        for ( int i = 0; i < meta.categories.length; i++ ) {
            CategoryItem cat = (CategoryItem) cats.get(i);
            meta.categories[i] = cat.getFullPath();          
        }
        meta.dateEffective = calendarToDate( item.getDateEffective() );
        meta.dateExpired = calendarToDate( item.getDateExpired() );
        return meta;
        
    }

    private Date calendarToDate(Calendar createdDate) {
        if (createdDate == null) return null;
        return createdDate.getTime();
    }
    
    private Calendar dateToCalendar(Date date) {
        if (date == null) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        return cal;
    }    

    private MetaDataMapper getMetaDataMapper() {
        if (this.metaDataMapper == null)  this.metaDataMapper = new MetaDataMapper();
        return this.metaDataMapper;
    }

    public String checkinVersion(RuleAsset asset) throws SerializableException {        
        RulesRepository repo = getRulesRepository();
        
        AssetItem repoAsset = repo.loadAssetByUUID( asset.uuid );
        
        MetaData meta = asset.metaData;
        
        getMetaDataMapper().copyFromMetaData( meta, repoAsset );
        
        repoAsset.updateDateEffective( dateToCalendar( meta.dateEffective ) );
        repoAsset.updateDateExpired( dateToCalendar( meta.dateExpired ) );        
        repoAsset.updateState( StateItem.DRAFT_STATE_NAME );
        
        repoAsset.updateCategoryList( meta.categories );
        AssetContentFormatHandler handler = new AssetContentFormatHandler();
        handler.storeAssetContent( asset, repoAsset );
        
        repoAsset.checkin( meta.checkinComment );
                
        
        return repoAsset.getUUID();
    }

    public TableDataResult loadAssetHistory(String uuid) throws SerializableException {
        
        List result = new ArrayList();
        
        RulesRepository repo = getRulesRepository();
        
        AssetItem item = repo.loadAssetByUUID( uuid );

        AssetHistoryIterator it = item.getHistory();
        //VersionIterator it = item.getNode().getVersionHistory().getAllVersions();
        while ( it.hasNext() ) {
            //Version element = (Version) it.next();
            AssetItem historical = (AssetItem) it.next();//new AssetItem(repo, element);
            String versionNumber = historical.getVersionNumber();
            if (!versionNumber.equals( "" ) 
                            && !versionNumber.equals( item.getVersionNumber() ))
                {
                TableDataRow row = new TableDataRow();
                    row.id = historical.getVersionSnapshotUUID();
                    row.values = new String[4];
                    row.values[0] = historical.getVersionNumber();
                    row.values[1] = historical.getCheckinComment();                
                    row.values[2] = dateFormatter.format( historical.getLastModified().getTime() );
                    row.values[3] = historical.getStateDescription();
                    result.add( row );                    
            }
        }

        
        
//        Iterator versions = item.getPredecessorVersionsIterator();
//        
//        
//        
//        while(versions.hasNext()) {
//            
//            TableDataRow row = new TableDataRow();
//            AssetItem historical = (AssetItem) versions.next();
//                row.id = historical.getVersionSnapshotUUID();
//                row.values = new String[4];
//                row.values[0] = historical.getVersionNumber();
//                row.values[1] = historical.getCheckinComment();                
//                row.values[2] = dateFormatter.format( historical.getLastModified().getTime() );
//                row.values[3] = historical.getStateDescription();
//                result.add( row );
//        }



        
        
        if (result.size() == 0) return null;
        TableDataResult table = new TableDataResult();
        table.data = (TableDataRow[]) result.toArray(new TableDataRow[result.size()]);
        
        return table;
    }

    public void restoreVersion(String versionUUID,
                                 String assetUUID,
                                 String comment) {
        RulesRepository repo = getRulesRepository();    

        repo.restoreHistoricalAsset( repo.loadAssetByUUID( versionUUID ), 
                                     repo.loadAssetByUUID( assetUUID ), 
                                     comment );
        
    }

    public String createPackage(String name,
                                String description) throws SerializableException {
        PackageItem item = getRulesRepository().createPackage( name, description );
        
        return item.getUUID();
    }

    public PackageConfigData loadPackage(String name) {
        PackageItem item = getRulesRepository().loadPackage( name );
        
        PackageConfigData data = new PackageConfigData();
        data.uuid = item.getUUID();
        data.header = item.getHeader();
        data.externalURI = item.getExternalURI();
        data.description = item.getDescription();
        data.name = item.getName();
        data.lastModified = item.getLastModified().getTime();
        data.lasContributor = item.getLastContributor();
        data.state = item.getStateDescription();
        
        
        return data;
    }

    public String savePackage(PackageConfigData data) throws SerializableException {
        PackageItem item = getRulesRepository().loadPackage( data.name );
        
        item.updateHeader( data.header );
        item.updateExternalURI( data.externalURI );
        item.updateDescription( data.description );
        
        item.checkin( data.description );
        
        return item.getUUID();
    }

    public TableDataResult listAssetsByFormat(String packageName,
                                              String formats[],
                                              int numRows,
                                              int startRow) throws SerializableException {
        long start = System.currentTimeMillis();
        PackageItem pkg = getRulesRepository().loadPackage( packageName );
        AssetItemIterator it = pkg.listAssetsByFormat( formats );
        if (numRows != -1) {
            it.skip( startRow );
        }
        TableDisplayHandler handler = new TableDisplayHandler();
        System.out.println("time for load: " + (System.currentTimeMillis() - start) );
        return handler.loadRuleListTable( it, numRows );
        

    }

    public String createState(String name) throws SerializableException {
        try {
            return getRulesRepository().createState( name ).getNode().getUUID();
        } catch ( RepositoryException e ) {            
            throw new SerializableException( "Unable to create the status." );
        }        
    }

    public String[] listStates() throws SerializableException {
        StateItem[] states = getRulesRepository().listStates();
        String[] result = new String[states.length];
        for ( int i = 0; i < states.length; i++ ) {
            result[i] = states[i].getName();
        }
        return result;
    }

    public void changeState(String uuid,
                            String newState,
                            boolean wholePackage) {
        RulesRepository repo = getRulesRepository();
        if (!wholePackage) {
            AssetItem asset = repo.loadAssetByUUID( uuid );
            asset.updateState( newState );
        } else {
            PackageItem pkg = repo.loadPackageByUUID( uuid );
            pkg.changeStatus(newState);            
        }
        repo.save();
    }

    public void changeAssetPackage(String uuid,
                                   String newPackage,
                                   String comment) {
        getRulesRepository().moveRuleItemPackage( newPackage, uuid, comment );
        
    }

    public String copyAsset(String assetUUID,
                          String newPackage,
                          String newName) {
        return getRulesRepository().copyAsset( assetUUID, newPackage, newName );        
    }

    public String[] listSnapshots(String packageName) {
        return getRulesRepository().listPackageSnapshots( packageName );        
    }

    public void createPackageSnapshot(String packageName,
                                      String snapshotName,
                                      boolean replaceExisting,
                                      String comment) {
        RulesRepository repo = getRulesRepository();
        
        if (replaceExisting) {
            repo.removePackageSnapshot( packageName, snapshotName );                        
        } 
        
        repo.createPackageSnapshot( packageName, snapshotName );
        PackageItem item = repo.loadPackageSnapshot( packageName, snapshotName );
        item.updateCheckinComment( comment );
        repo.save();
        
    }
    




    
    


}
