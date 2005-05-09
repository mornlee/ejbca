/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
 
    package se.anatom.ejbca.webdist.loginterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;
import java.rmi.RemoteException;
import se.anatom.ejbca.webdist.rainterface.SortBy;
import se.anatom.ejbca.log.LogEntry;

/**
 * A class representing a set of log entry view representations.
 * @author  TomSelleck
 * @version $Id: LogEntriesView.java,v 1.9 2005-05-09 15:43:11 anatom Exp $
 */
public class LogEntriesView implements java.io.Serializable {
 
    /** Creates a new instance of LogEntriesView  */
    public LogEntriesView(SubjectDNProxy dnproxy , String[] localinfoeventnames, String[] localerroreventnames, String[] localmodulenames, HashMap caidtonamemap) {
      logentryviews = new ArrayList();
      sortby = new SortBy(SortBy.TIME, SortBy.DECENDING);
      this.dnproxy = dnproxy;
      this.localinfoeventnames=localinfoeventnames;
      this.localerroreventnames=localerroreventnames;
      this.localmodulenames=localmodulenames;
      this.caidtonamemap=caidtonamemap;
    }
    
    /** Creates a new instance of LogEntriesView  containing one LogEntryView object.
     * @param logentry The log entry of LogEntry class to import.
     */
    public LogEntriesView(LogEntry logentry, SubjectDNProxy dnproxy, String[] localinfoeventnames, String[] localerroreventnames, String[] localmodulenames, HashMap caidtonamemap) throws RemoteException {
      logentryviews = new ArrayList();
      sortby = new SortBy(SortBy.TIME, SortBy.DECENDING);     
      this.dnproxy = dnproxy;      
      logentryviews.add(new LogEntryView(logentry, dnproxy, localinfoeventnames, localerroreventnames, localmodulenames, caidtonamemap)); 
      this.localinfoeventnames=localinfoeventnames;
      this.localerroreventnames=localerroreventnames;   
      this.localmodulenames=localmodulenames;      
      this. caidtonamemap=caidtonamemap;
    }

    /** Creates a new instance of LogEntriesView  containing a collection of LogEntryView objects.
     * @param logentries a collection of log entries of LogEntry class to import.
     */    
    public LogEntriesView(Collection logentries, SubjectDNProxy dnproxy , String[] localinfoeventnames, String[] localerroreventnames, String[] localmodulenames, HashMap caidtonamemap) throws RemoteException { 
      logentryviews = new ArrayList();
      sortby = new SortBy(SortBy.TIME, SortBy.DECENDING);
      this.dnproxy = dnproxy;
      this.localinfoeventnames=localinfoeventnames;
      this.localerroreventnames=localerroreventnames;
      this.localmodulenames=localmodulenames;
      this.caidtonamemap=caidtonamemap;
      setEntries(logentries);
    }
    // Public methods.
    
    /**
     * Methods that sets the sorting preference and sortorder.
     *
     * @param sortby should be one of the constants defined in se.anatom.ejbca.webdist.rainterface.Sortby.
     * @param sortorder should be one of the constants defined in se.anatom.ejbca.webdist.rainterface.Sortby.
     */  
    public void sortBy(int sortby, int sortorder) {
      this.sortby.setSortBy(sortby);
      this.sortby.setSortOrder(sortorder);
      
      Collections.sort(logentryviews);
    }
    
    /**
     * Method that returns a given number of sorted logentryviews
     *   
     * @param index the startingpoint of the data.
     * @param size the number of logentryviews to return
     * @return an array of LogEntryView.
     **/
    public LogEntryView[] getEntries(int index, int size) {
      int endindex;  
      LogEntryView[] returnval;
   
      if(index > logentryviews.size()) index = logentryviews.size()-1;
      if(index < 0) index =0;
      
      endindex = index + size;
      if(endindex > logentryviews.size()) endindex = logentryviews.size();
      
      returnval = new LogEntryView[endindex-index];  
      
      int end = endindex - index;
      for(int i = 0; i < end; i++){
        returnval[i] = (LogEntryView) logentryviews.get(index+i);   
      }
      
      return returnval;
    }
    
    /*
     * Methods that clears the internal data and adds an array of logentries.
     */
    public void setEntries(LogEntry[] logentries) throws RemoteException {
      LogEntryView logentryview;  
      this.logentryviews.clear();
      if(logentries !=null && logentries.length > 0){ 
        for(int i=0; i< logentries.length; i++){
          logentryview = new LogEntryView(logentries[i], dnproxy, localinfoeventnames, localerroreventnames, localmodulenames, caidtonamemap); 
          logentryview.setSortBy(this.sortby);
          this.logentryviews.add(logentryview);
        }
        Collections.sort(this.logentryviews);
      }
    }

    /*
     * Method that clears the internal data and adds a collection of logentries.
     */ 
    public void setEntries(Collection logentries) throws RemoteException{ 
      LogEntryView logentryview;   
      Iterator i;  
      this.logentryviews.clear();
      if(logentries!=null && logentries.size() > 0){
        i=logentries.iterator();
        while(i.hasNext()){
          LogEntry nextentry = (LogEntry) i.next();  
          logentryview = new LogEntryView(nextentry, dnproxy, localinfoeventnames, localerroreventnames, localmodulenames, caidtonamemap); 
          logentryview.setSortBy(this.sortby);
          logentryviews.add(logentryview);
        }
        Collections.sort(logentryviews);
      }
    }

    /*
     * Method that returns the number of available logentryviews. 
     */   
    public int size(){
      return logentryviews.size();   
    }
    // Private fields
    private ArrayList logentryviews;
    private SortBy sortby;
    private SubjectDNProxy dnproxy;
    private String[] localinfoeventnames;
    private String[] localerroreventnames;
    private String[] localmodulenames;
    private HashMap  caidtonamemap;
}
