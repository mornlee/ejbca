/*
 * SortBy.java
 *
 * Created on den 18 april 2002, 15:47
 */

package se.anatom.ejbca.webdist.rainterface;

/**
 * A class specifying which field to sort the userdata by. 
 *
 * @author  Philip Vendil
 */
public class SortBy {
    // Public constants
    public static final int USERNAME         = 0;
    public static final int PASSWORD         = 1;
    public static final int COMMONNAME       = 2;
    public static final int ORGANIZATIONUNIT = 3;
    public static final int ORGANIZATION     = 4;
    public static final int LOCALE           = 5;
    public static final int STATE            = 6;
    public static final int COUNTRY          = 7;
    public static final int EMAIL            = 8;
    public static final int STATUS           = 9; 
    
    public static final int ACCENDING        = 0;
    public static final int DECENDING        = 1;

    
    /** Creates a new instance of SortBy */
    public SortBy() {
      this.sortby=USERNAME;
      this.sortorder=ACCENDING;
    }
    
    public SortBy(int sortby, int sortorder){
      this.sortby=sortby;   
      this.sortorder=sortorder;
    }
    
    int getSortBy() {
      return sortby;
    }
    
    int getSortOrder() {
      return sortorder;
    }
    
    void setSortBy(int sortby) {
       this.sortby=sortby;      
    }

    void setSortOrder(int sortorder){
      this.sortorder=sortorder;        
    }
    // Private fields.
    private int sortby;
    private int sortorder;
}
