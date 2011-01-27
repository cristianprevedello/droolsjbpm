package org.drools.guvnor.client.widgets.decoratedgrid;

/**
 * Column operations for consumers of DecoratedGridWidget
 * 
 * @param <T>
 *            The type of domain columns represented
 * 
 * @author manstis
 */
public interface HasColumns<T> {

    public abstract void addColumn(T modelColumn);

    public abstract void setColumnVisibility(T modelColumn,
                                             boolean isVisible);

    public abstract void deleteColumn(T modelColumn);

}