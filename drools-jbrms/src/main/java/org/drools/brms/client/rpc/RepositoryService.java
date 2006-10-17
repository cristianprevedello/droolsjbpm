package org.drools.brms.client.rpc;

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * This is what the remote service will implement, as a servlet.
 * (in hosted/debug mode, you could also use an implementation that was in-process).
 */
public interface RepositoryService extends RemoteService {

    /**
     * @param categoryPath A "/" delimited path to a category. 
     * @param callback
     */
    public String[] loadChildCategories(String categoryPath);
 
    
    /**
     * Return a a 2d array/grid of results for rules.
     * @param A "/" delimited path to a category.
     * @param status The status flag. Leave blank to be all.
     */
    public String[][] loadRuleListForCategories(String categoryPath, String status);
    
    /**
     * This will return a TableConfig of header names.
     * @param listName The name of the list that we are going to render.
     */
    public TableConfig loadTableConfig(String listName);
    
    /**
     * This will create a new category at the specified path.
     */
    public Boolean createCategory(String path, String name, String description);
    
    /**
     * Creates a brand new rule with the initial category.
     */
    public Boolean createNewRule(String ruleName, String description, String initialCategory, String initialPackage);
    
    /**
     * This returns a list of packages where rules may be added.
     */
    public String[] listRulePackages();
}
