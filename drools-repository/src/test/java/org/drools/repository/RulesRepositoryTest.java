package org.drools.repository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import junit.framework.TestCase;

public class RulesRepositoryTest extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testDefaultPackage() throws Exception {
        RulesRepository repo = RepositorySessionUtil.getRepository();
        
        Iterator it = repo.listPackages();
        boolean foundDefault = false;
        while(it.hasNext()) {
            PackageItem item = (PackageItem) it.next();
            if (item.getName().equals( RulesRepository.DEFAULT_PACKAGE )) {
                foundDefault = true;
            }
        }
        assertTrue(foundDefault);
        
        PackageItem def = repo.loadDefaultPackage();
        assertNotNull(def);
        assertEquals("default", def.getName());
        
        
    }
    
    public void testAddVersionARule() throws Exception {
        RulesRepository repo = RepositorySessionUtil.getRepository();
        PackageItem pack = repo.createPackage( "testAddVersionARule", "description" );
        repo.save();
        
        AssetItem rule = pack.addAsset( "my rule", "foobar" );
        assertEquals("my rule", rule.getName());
        
        rule.updateContent( "foo foo" );
        rule.checkin( "version0" );
        
        pack.addAsset( "other rule", "description" );
        
        rule.updateContent( "foo bar" );
        rule.checkin( "version1" );
        
        PackageItem pack2 =  repo.loadPackage( "testAddVersionARule" );
        
        Iterator it =  pack2.getAssets();
        
        it.next();
        it.next();
        
        assertFalse(it.hasNext());
        
        AssetItem prev = (AssetItem) rule.getPrecedingVersion();
       
        assertEquals("foo bar", rule.getContent());
        assertEquals("foo foo", prev.getContent());
        
        
        
    }

    
    public void testLoadRuleByUUID() throws Exception {
        RulesRepository repo = RepositorySessionUtil.getRepository();
        
        PackageItem rulePackageItem = repo.loadDefaultPackage();
        AssetItem rule = rulePackageItem.addAsset( "testLoadRuleByUUID", "this is a description");
        
        repo.save();
                
        String uuid = rule.getNode().getUUID();

        AssetItem loaded = repo.loadAssetByUUID(uuid);
        assertNotNull(loaded);
        assertEquals("testLoadRuleByUUID", loaded.getName());
        assertEquals( "this is a description", loaded.getDescription());
        
        String oldVersionNumber = loaded.getVersionNumber();
        
        loaded.updateContent( "xxx" );
        loaded.checkin( "woo" );
        
        
        
        
        AssetItem reload = repo.loadAssetByUUID( uuid );
        assertEquals("testLoadRuleByUUID", reload.getName());
        assertEquals("xxx", reload.getContent());
        System.out.println(reload.getVersionNumber());
        System.out.println(loaded.getVersionNumber());
        assertFalse(reload.getVersionNumber().equals( oldVersionNumber ));
        

        // try loading rule package that was not created 
        try {
            repo.loadAssetByUUID("01010101-0101-0101-0101-010101010101");
            fail("Exception not thrown loading rule package that was not created.");
        } catch (RulesRepositoryException e) {
            // that is OK!
            assertNotNull(e.getMessage());
        }   

        //now test concurrent session access...
        
        AssetItem asset1 = repo.loadDefaultPackage().addAsset( "testMultiSession", "description" );
        asset1.updateContent( "yeah" );
        asset1.checkin( "boo" );
        uuid = asset1.getUUID();
        
        Session s2 = repo.getSession().getRepository().login(new SimpleCredentials("fdd", "password".toCharArray()));
        
        RulesRepository repo2 = new RulesRepository(s2);
        
        AssetItem asset2 = repo2.loadAssetByUUID( uuid );
        asset2.updateContent( "yeah 42" );
        asset2.checkin( "yeah" );
        
        asset1 = repo.loadAssetByUUID( uuid );
        assertEquals("yeah 42", asset1.getContent());
        asset1.updateContent( "yeah 43" );
        asset1.checkin( "la" );
        
        asset2 = repo2.loadAssetByUUID( uuid );
        assertEquals( "yeah 43", asset2.getContent() );
    }
    
    public void testAddRuleCalendarWithDates() {
        RulesRepository rulesRepository = RepositorySessionUtil.getRepository();

                        
            Calendar effectiveDate = Calendar.getInstance();
            Calendar expiredDate = Calendar.getInstance();
            expiredDate.setTimeInMillis(effectiveDate.getTimeInMillis() + (1000 * 60 * 60 * 24));
            AssetItem ruleItem1 = rulesRepository.loadDefaultPackage().addAsset("testAddRuleCalendarCalendar", "desc");
            ruleItem1.updateDateEffective( effectiveDate );
            ruleItem1.updateDateExpired( expiredDate );
     
            assertNotNull(ruleItem1);
            assertNotNull(ruleItem1.getNode());
            assertEquals(effectiveDate, ruleItem1.getDateEffective());
            assertEquals(expiredDate, ruleItem1.getDateExpired());
            
            ruleItem1.checkin( "ho " );            
    }

    public void testGetState() {
        RulesRepository rulesRepository = RepositorySessionUtil.getRepository();
            
            StateItem state0 = rulesRepository.createState( "testGetState" );
            assertNotNull(state0);
            assertEquals("testGetState", state0.getName());
            StateItem stateItem1 = rulesRepository.getState("testGetState");
            assertNotNull(stateItem1);
            assertEquals("testGetState", stateItem1.getName());
            
            StateItem stateItem2 = rulesRepository.getState("testGetState");
            assertNotNull(stateItem2);
            assertEquals("testGetState", stateItem2.getName());
            assertEquals(stateItem1, stateItem2);
    }

    public void testGetTag() {
            RulesRepository rulesRepository = RepositorySessionUtil.getRepository();
            
            CategoryItem root = rulesRepository.loadCategory( "/" );
            CategoryItem tagItem1 = root.addCategory( "testGetTag", "ho");
            assertNotNull(tagItem1);
            assertEquals("testGetTag", tagItem1.getName());
            assertEquals("testGetTag", tagItem1.getFullPath());
            
            CategoryItem tagItem2 = rulesRepository.loadCategory("testGetTag");
            assertNotNull(tagItem2);
            assertEquals("testGetTag", tagItem2.getName());
            assertEquals(tagItem1, tagItem2);
            
            //now test getting a tag down in the tag hierarchy
            CategoryItem tagItem3 = tagItem2.addCategory( "TestChildTag1", "ka");
            assertNotNull(tagItem3);
            assertEquals("TestChildTag1", tagItem3.getName());
            assertEquals("testGetTag/TestChildTag1", tagItem3.getFullPath());                                   
    }
    

    
    public void testListPackages() {
        RulesRepository rulesRepository = RepositorySessionUtil.getRepository();
        
        
            PackageItem rulePackageItem1 = rulesRepository.createPackage("testListPackages", "desc");
            
            assertTrue(rulesRepository.containsPackage( "testListPackages" ));
            assertFalse(rulesRepository.containsPackage( "XXXXXXX" ));
            
            Iterator it = rulesRepository.listPackages();
            assertTrue(it.hasNext());
            
            boolean found = false;
            while ( it.hasNext() ) {
                PackageItem element = (PackageItem) it.next();
                if (element.getName().equals( "testListPackages" ))
                {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
            
    }
    
    public void testMoveRulePackage() throws Exception {
        RulesRepository repo = RepositorySessionUtil.getRepository();
        PackageItem pkg = repo.createPackage( "testMove", "description" );
        AssetItem r = pkg.addAsset( "testMove", "description" );
        r.checkin( "version0" );
        
        assertEquals("testMove", r.getPackageName());
        
        repo.save();
        
        assertEquals(1, iteratorToList( pkg.getAssets()).size());
        
        repo.createPackage( "testMove2", "description" );
        repo.moveRuleItemPackage( "testMove2", r.node.getUUID(), "explanation" );
        
        pkg = repo.loadPackage( "testMove" );
        assertEquals(0, iteratorToList( pkg.getAssets() ).size());
        
        pkg = repo.loadPackage( "testMove2" );
        assertEquals(1, iteratorToList( pkg.getAssets() ).size());
        
        r = (AssetItem) pkg.getAssets().next();
        assertEquals("testMove", r.getName());
        assertEquals("testMove2", r.getPackageName());
        assertEquals("explanation", r.getCheckinComment());
        
        AssetItem p = (AssetItem) r.getPrecedingVersion();
        assertEquals("testMove", p.getPackageName());
        assertEquals("version0", p.getCheckinComment());
        
    }
    
    public void testListStates()  {
        RulesRepository repo = RepositorySessionUtil.getRepository();
        StateItem[] items = repo.listStates();
        assertTrue(items.length > 0);
        
        repo.createState( "testListStates" );
        
        StateItem[] items2 = repo.listStates();
        assertEquals(items.length + 1, items2.length);
    }
    
    List iteratorToList(Iterator it) {
        List list = new ArrayList();
        while(it.hasNext()) {
            list.add( it.next() );
        }
        return list;
    }
}
