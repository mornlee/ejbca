/*
 * UserPreference.java
 *
 * Created on den 28 mars 2002, 12:36
 */

package se.anatom.ejbca.webdist.webconfiguration;

/**
 * A class representing a users personal preferenses.
 *
 * @author  Philip Vendil
 */
public class UserPreference implements java.io.Serializable, Cloneable {
        
    /** Creates a new instance of UserPreference */
    public UserPreference() {}
    
    public int getPreferedLanguage() {return preferedlanguage;}
    
   /* Returns the prefered language code. Ex: 'EN' */    
    public String getPreferedLanguageCode(){
      return WebLanguages.getAvailableLanguages()[preferedlanguage];
    }
    
    public int getSecondaryLanguage() {return secondarylanguage;}

    /* Returns the prefered secondary language code. Ex: 'EN' */
    public String getSecondaryLanguageCode(){
      return  WebLanguages.getAvailableLanguages()[secondarylanguage];
    }
    
    public int getEntriesPerPage(){
      return entriesperpage;
    }
        
    public void setEntriesPerPage(int entriesperpage){
      this.entriesperpage= entriesperpage; 
    }
    public String getTheme() {return theme;}
    
    public String getCssFile(){
      return GlobalConfiguration.getRaAdminPath() + GlobalConfiguration.getThemePath() + "/" + theme + ".css";  
    }
    
    
    public void setPreferedLanguage(String languagecode) {
      String[] languages = WebLanguages.getAvailableLanguages();  
      if(languages != null){
        for(int i=0; i < languages.length; i++){
          if(languages[i].toUpperCase().equals(languagecode.toUpperCase()))
            preferedlanguage=i;
        }
      }
    }
    
    public void setPreferedLanguage(int language){
      this.preferedlanguage=language;  
    }
    
    public void setSecondaryLanguage(String languagecode){
      String[] languages = WebLanguages.getAvailableLanguages();   
      if(languages != null){
        for(int i=0; i < languages.length; i++){ 
          if(languages[i].toUpperCase().equals(languagecode.toUpperCase()))
            secondarylanguage=i;
        }
      }
    }
     
    
    public void setSecondaryLanguage(int language) {
      this.secondarylanguage=language;
    }    
    
    public void setTheme(String theme) {
      this.theme=theme;  
    }
    
    public Object clone() throws CloneNotSupportedException {
      return super.clone();   
    }
    
    // Private fields
    private int preferedlanguage; 
    private int secondarylanguage;
    private int entriesperpage= 30;
    private String theme = "default_theme";
    
}
