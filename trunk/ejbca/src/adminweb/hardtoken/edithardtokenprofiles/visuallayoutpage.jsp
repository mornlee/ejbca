<%               
  int[] visualtypes = {IVisualLayoutSettings.VISUALLAYOUTTYPE_NONE, IVisualLayoutSettings.VISUALLAYOUTTYPE_GENERALLABEL, IVisualLayoutSettings.VISUALLAYOUTTYPE_GENERALCARDPRINTER};
  String[] visualtypetexts = {"NONE","GENERALLABEL", "GENERALCARDPRINTER"};

  IVisualLayoutSettings visprofile = (IVisualLayoutSettings) helper.profiledata;
%>
   <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="right"> 
         &nbsp;
        </div>
      </td>
      <td width="50%" valign="top"> 
         &nbsp;
      </td>
   </tr>
   <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="right"> 
          <%= ejbcawebbean.getText("VISUALLAYOUTSETTINGS") %>:
        </div>
      </td>
      <td width="50%" valign="top"> 
         &nbsp;
      </td>
    </tr>
    <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="right"> 
          <%= ejbcawebbean.getText("TYPE") %>
        </div>
      </td>
      <td width="50%" valign="top">   
        <select name="<%=EditHardTokenProfileJSPHelper.SELECT_VISUALLAYOUTTYPE%>" size="1"  >       
            <% int currentvistype = visprofile.getVisualLayoutType();
               for(int i=0; i < visualtypes.length ; i ++){%>
              <option value="<%=visualtypes[i]%>" <% if(visualtypes[i] == currentvistype) out.write(" selected "); %>> 
                  <%= ejbcawebbean.getText(visualtypetexts[i]) %>
               </option>
            <%}%>
          </select>         
      </td>
    </tr>
    <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="right"> 
          <%= ejbcawebbean.getText("CURRENTTEMPLATE") %>
        </div>
      </td>
      <td width="50%" valign="top">          
         <% if(visprofile.getVisualLayoutTemplateFilename() == null || visprofile.getVisualLayoutTemplateFilename().equals("")){
              out.write("NONE");
            }else{
              out.write(visprofile.getVisualLayoutTemplateFilename());
            }
         %> 
      </td>
    </tr>
    <tr id="Row<%=row++%2%>"> 
      <td width="50%" valign="top"> 
        <div align="right"> 
          <%= ejbcawebbean.getText("UPLOADTEMPLATE") %>
        </div>
      </td>
      <td width="50%" valign="top">          
        <input type="submit" name="<%= EditHardTokenProfileJSPHelper.BUTTON_UPLOADVISUALTEMP %>" onClick='return checkallfields()' value="<%= ejbcawebbean.getText("UPLOADTEMPLATE") %>">
      </td>
    </tr>    

