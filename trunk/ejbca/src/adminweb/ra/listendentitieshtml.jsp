
<head>
  <title><%= globalconfiguration.getEjbcaTitle() %></title>
  <base href="<%= ejbcawebbean.getBaseUrl() %>">
  <link rel=STYLESHEET href="<%= ejbcawebbean.getCssFile() %>">
  <script language=javascript src="<%= globalconfiguration.getAdminWebPath() %>ejbcajslib.js"></script>
  <script language=javascript>
<!--
function viewuser(row){
    var hiddenusernamefield = eval("document.form.<%= HIDDEN_USERNAME %>" + row);
    var username = hiddenusernamefield.value;
    var link = "<%= VIEWUSER_LINK %>?<%= USER_PARAMETER %>="+username;
    link = encodeURI(link);
    window.open(link, 'view_user',config='height=600,width=500,scrollbars=yes,toolbar=no,resizable=1');
}

function edituser(row){
    var hiddenusernamefield = eval("document.form.<%= HIDDEN_USERNAME %>" + row);
    var username = hiddenusernamefield.value;
    var link = "<%= EDITUSER_LINK %>?<%= USER_PARAMETER %>="+username;
    link = encodeURI(link);
    window.open(link, 'edit_user',config='height=600,width=550,scrollbars=yes,toolbar=no,resizable=1');
}

function viewhistory(row){
    var hiddenusernamefield = eval("document.form.<%= HIDDEN_USERNAME %>" + row);
    var username = hiddenusernamefield.value;
    var link = "<%= VIEWHISTORY_LINK %>?<%= USER_PARAMETER %>="+username;
    link = encodeURI(link);
    window.open(link, 'view_history',config='height=600,width=800,scrollbars=yes,toolbar=no,resizable=1');
}

function viewcert(row){
    var hiddenusernamefield = eval("document.form.<%= HIDDEN_USERNAME %>" + row);
    var username = hiddenusernamefield.value;
    var link = "<%= VIEWCERT_LINK %>?<%= USER_PARAMETER %>="+username;
    link = encodeURI(link);
    window.open(link, 'view_cert',config='height=600,width=500,scrollbars=yes,toolbar=no,resizable=1');
}

function viewtoken(row){
    var hiddenusernamefield = eval("document.form.<%= HIDDEN_USERNAME %>" + row);
    var username = hiddenusernamefield.value;
    var link = "<%= VIEWTOKEN_LINK %>?<%= USER_PARAMETER %>="+username;
    link = encodeURI(link);
    window.open(link, 'view_token',config='height=600,width=600,scrollbars=yes,toolbar=no,resizable=1');
}

function confirmdelete(){
  var returnval;
  returnval = confirm("<%= ejbcawebbean.getText("AREYOUSUREDELETE") %>");
  returnval = returnval && confirm("<%= ejbcawebbean.getText("HAVEYOUREVOKEDTHEENDENTITIES") %>");

  return returnval;
}

function confirmrevokation(){
  var returnval = false;
  if(document.form.<%= SELECT_REVOKE_REASON %>.options.selectedIndex == -1){
     alert("<%= ejbcawebbean.getText("AREVOKEATIONREASON") %>"); 
     returnval = false;
  }else{
    returnval = confirm("<%= ejbcawebbean.getText("AREYOUSUREREVOKE") %>");
  } 
  return returnval;
}

-->
</script>
</head>

<body>
<h2 align="center"><%= ejbcawebbean.getText("LISTENDENTITIES") %></h2>
  <form name="changefiltermode" method="post" action="<%=THIS_FILENAME %>">
    <div align="right">
     <% if(filtermode == AdminPreference.FILTERMODE_BASIC){ %>
      <input type="hidden" name='<%= ACTION %>' value='<%=ACTION_CHANGEFILTERMODETO_ADVANCED %>'>
      <A href='javascript:document.changefiltermode.submit();'><u><%= ejbcawebbean.getText("ADVANCEDMODE") %></u></A>
     <% }
        if(filtermode == AdminPreference.FILTERMODE_ADVANCED){ %>
        <input type="hidden" name='<%= ACTION %>' value='<%=ACTION_CHANGEFILTERMODETO_BASIC %>'>
        <A href='javascript:document.changefiltermode.submit();'><u><%= ejbcawebbean.getText("BASICMODE") %></u></A>
     <% } %>
     &nbsp;&nbsp;&nbsp;
     <A  onclick='displayHelpWindow("<%= ejbcawebbean.getHelpfileInfix("ra_help.html") +"#listendentities" %>")'>
     <u><%= ejbcawebbean.getText("HELP") %></u> </A>
  </div>
  </form> 
<form name="form" method="post" action="<%=THIS_FILENAME %>">
  <input type="hidden" name='<%= ACTION %>' value='<%=ACTION_LISTUSERS %>'>
  <input type="hidden" name='<%= OLD_ACTION %>' value='<%=oldaction %>'>
  <input type="hidden" name='<%= OLD_ACTION_VALUE %>' value='<%=oldactionvalue %>'>
  <% if(oldmatchwithrow1 != null){ %>
  <input type="hidden" name='<%= OLD_MATCHWITHROW1 %>' value='<%=oldmatchwithrow1 %>'>
  <% } %>
  <% if(oldmatchwithrow2 != null){ %>
  <input type="hidden" name='<%= OLD_MATCHWITHROW2 %>' value='<%=oldmatchwithrow2 %>'>
  <% } %>
  <% if(oldmatchwithrow3 != null){ %>
  <input type="hidden" name='<%= OLD_MATCHWITHROW3 %>' value='<%=oldmatchwithrow3 %>'>
  <% } %>
  <% if(oldmatchwithrow4 != null){ %>
  <input type="hidden" name='<%= OLD_MATCHWITHROW4 %>' value='<%=oldmatchwithrow4 %>'>
  <% } %>
  <% if(oldmatchtyperow1 != null){ %>
  <input type="hidden" name='<%= OLD_MATCHTYPEROW1 %>' value='<%=oldmatchtyperow1 %>'>
  <% } %>
  <% if(oldmatchtyperow2 != null){ %>
  <input type="hidden" name='<%= OLD_MATCHTYPEROW2 %>' value='<%=oldmatchtyperow2 %>'>
  <% } %>
  <% if(oldmatchtyperow3 != null){ %>
  <input type="hidden" name='<%= OLD_MATCHTYPEROW2 %>' value='<%=oldmatchtyperow3 %>'>
  <% } %>
  <% if(oldmatchvaluerow1 != null){ %>
  <input type="hidden" name='<%= OLD_MATCHVALUEROW1 %>' value='<%=oldmatchvaluerow1%>'>
  <% } %>
  <% if(oldmatchvaluerow2 != null){ %>
  <input type="hidden" name='<%= OLD_MATCHVALUEROW2 %>' value='<%=oldmatchvaluerow2 %>'>
  <% } %>
  <% if(oldmatchvaluerow3 != null){ %>
  <input type="hidden" name='<%= OLD_MATCHVALUEROW3 %>' value='<%=oldmatchvaluerow3%>'>
  <% } %>
  <% if(oldconnectorrow2 != null){ %>
  <input type="hidden" name='<%= OLD_CONNECTORROW2 %>' value='<%=oldconnectorrow2%>'>
  <% } %>
  <% if(oldconnectorrow3 != null){ %>
  <input type="hidden" name='<%= OLD_CONNECTORROW3 %>' value='<%=oldconnectorrow3%>'>
  <% } %>
  <% if(oldconnectorrow4 != null){ %>
  <input type="hidden" name='<%= OLD_CONNECTORROW4 %>' value='<%=oldconnectorrow4%>'>
  <% } %>
  <% if(olddayrow4 != null){ %>
  <input type="hidden" name='<%= OLD_DAY_ROW4 %>' value='<%=olddayrow4%>'>
  <% } %>
  <% if(olddayrow5 != null){ %>
  <input type="hidden" name='<%= OLD_DAY_ROW5 %>' value='<%=olddayrow5%>'>
  <% } %>
  <% if(oldmonthrow4 != null){ %>
  <input type="hidden" name='<%= OLD_MONTH_ROW4 %>' value='<%=oldmonthrow4%>'>
  <% } %>
  <% if(oldmonthrow5 != null){ %>
  <input type="hidden" name='<%= OLD_MONTH_ROW5 %>' value='<%=oldmonthrow5%>'>
  <% } %>
  <% if(oldyearrow4 != null){ %>
  <input type="hidden" name='<%= OLD_YEAR_ROW4 %>' value='<%=oldyearrow4%>'>
  <% } %>
  <% if(oldyearrow5 != null){ %>
  <input type="hidden" name='<%= OLD_YEAR_ROW5 %>' value='<%=oldyearrow5%>'>
  <% } %>
  <% if(oldtimerow4 != null){ %>
  <input type="hidden" name='<%= OLD_TIME_ROW4 %>' value='<%=oldtimerow4%>'>
  <% } %>
  <% if(oldtimerow5 != null){ %>
  <input type="hidden" name='<%= OLD_TIME_ROW5 %>' value='<%=oldtimerow5%>'>
  <% } %>

  <input type="hidden" name='<%= HIDDEN_RECORDNUMBER %>' value='<%=String.valueOf(record) %>'>
  <input type="hidden" name='<%= HIDDEN_SORTBY  %>' value='<%=sortby %>'>
     <% if(filtermode == AdminPreference.FILTERMODE_BASIC){ %>
        <%@ include file="basicfiltermodehtml.jsp" %>
     <% }
        if(filtermode == AdminPreference.FILTERMODE_ADVANCED){ %>
        <%@ include file="advancedfiltermodehtml.jsp" %>
     <%   } %>

  <% if(illegalquery){ %>
      <H4 id="alert"><div align="center"><%= ejbcawebbean.getText("INVALIDQUERY") %></div></H4>
  <% } %>
  <% if(notauthorizedrevokeall){ %>
      <H4 id="alert"><div align="center"><%= ejbcawebbean.getText("ONLYAUTHORIZEDENDENTITIESDEL") %></div></H4>
  <% } %>
  <% if(notauthorizeddeleteall){ %>
      <H4 id="alert"><div align="center"><%= ejbcawebbean.getText("ONLYAUTHORIZEDENDENTITIESDEL") %></div></H4>
  <% } %>
  <% if(notauthorizedchangeall){ %>
      <H4 id="alert"><div align="center"><%= ejbcawebbean.getText("ONLYAUTHORIZEDENDENTITIESCHANG") %></div></H4>
  <% } %>
  <% if(largeresult){ %>
     <H4 id="alert"><div align="center" ><%= ejbcawebbean.getText("TOLARGERESULT")  + " " + RAInterfaceBean.MAXIMUM_QUERY_ROWCOUNT
                                             + " " + ejbcawebbean.getText("ROWSWILLBEDISPLAYED") %> </div> </H4>  
  <% } 
  if(!blank){ %>
  <p>
    <input type="submit" name="<%=BUTTON_RELOAD %>" value="<%= ejbcawebbean.getText("RELOAD") %>">
  </p>
  <br>
  <table width="100%" border="0" cellspacing="1" cellpadding="0">
    <tr> 
      <td width="14%"> 
        <% if(rabean.previousButton(record,size) ){ %>
          <input type="submit" name="<%=BUTTON_PREVIOUS %>" value="<%= ejbcawebbean.getText("PREVIOUS") %>">
        <% } %>
      </td>
      <td width="76%">&nbsp; </td>
      <td width="10%"> 
        <div align="right">
        <% if(rabean.nextButton(record,size) ){ %>
          <input type="submit" name="<%=BUTTON_NEXT %>" value="<%= ejbcawebbean.getText("NEXT") %>">
        <% } %>
        </div>
      </td>
    </tr>
  </table>
  <table width="100%" border="0" cellspacing="1" cellpadding="0">
  <tr> 
    <td width="5%"><%= ejbcawebbean.getText("SELECT") %>
     </td>
    <td width="11%"><% if(sortby.equals(SORTBY_USERNAME_ACC)){ %>
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("downarrow.gif") %>' border="0" name="<%=SORTBY_USERNAME_DEC %>" value="submit" ><%= ejbcawebbean.getText("USERNAME") %>              
                   <% }else{
                         if(sortby.equals(SORTBY_USERNAME_DEC)){ %>
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("uparrow.gif") %>' border="0" name="<%=SORTBY_USERNAME_ACC %>" value="submit" ><%= ejbcawebbean.getText("USERNAME") %>                     
                   <%    }else{ %> 
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("noarrow.gif") %>' border="0" name="<%=SORTBY_USERNAME_ACC %>" value="submit" ><%= ejbcawebbean.getText("USERNAME") %>
                   <%    }
                       } %>
    </td>
    <td width="19%">
                   <% if(sortby.equals(SORTBY_COMMONNAME_ACC)){ %>
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("downarrow.gif") %>' border="0" name="<%=SORTBY_COMMONNAME_DEC %>" value="submit" ><%= ejbcawebbean.getText("COMMONNAME") %>              
                   <% }else{
                         if(sortby.equals(SORTBY_COMMONNAME_DEC)){ %>
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("uparrow.gif") %>' border="0" name="<%=SORTBY_COMMONNAME_ACC %>" value="submit" ><%= ejbcawebbean.getText("COMMONNAME") %>                     
                   <%    }else{ %> 
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("noarrow.gif") %>' border="0" name="<%=SORTBY_COMMONNAME_ACC %>" value="submit" ><%= ejbcawebbean.getText("COMMONNAME") %>
                   <%    }
                       } %>
    </td>
    <td width="17%">
                   <% if(sortby.equals(SORTBY_ORGANIZATIONUNIT_ACC)){ %>
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("downarrow.gif") %>' border="0" name="<%=SORTBY_ORGANIZATIONUNIT_DEC %>" value="submit" ><%= ejbcawebbean.getText("ORGANIZATIONUNIT") %>              
                   <% }else{
                         if(sortby.equals(SORTBY_ORGANIZATIONUNIT_DEC)){ %>
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("uparrow.gif") %>' border="0" name="<%=SORTBY_ORGANIZATIONUNIT_ACC %>" value="submit" ><%= ejbcawebbean.getText("ORGANIZATIONUNIT") %>                     
                   <%    }else{ %> 
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("noarrow.gif") %>' border="0" name="<%=SORTBY_ORGANIZATIONUNIT_ACC %>" value="submit" ><%= ejbcawebbean.getText("ORGANIZATIONUNIT") %>
                   <%    }
                       } %>
    </td>
    <td width="18%"><% if(sortby.equals(SORTBY_ORGANIZATION_ACC)){ %>
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("downarrow.gif") %>' border="0" name="<%=SORTBY_ORGANIZATION_DEC %>" value="submit" ><%= ejbcawebbean.getText("ORGANIZATION") %>                        
                   <% }else{ 
                         if(sortby.equals(SORTBY_ORGANIZATION_DEC)){ %>
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("uparrow.gif") %>' border="0" name="<%=SORTBY_ORGANIZATION_ACC %>" value="submit" ><%= ejbcawebbean.getText("ORGANIZATION") %>                 
                   <%    }else{ %> 
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("noarrow.gif") %>' border="0" name="<%=SORTBY_ORGANIZATION_ACC %>" value="submit" ><%= ejbcawebbean.getText("ORGANIZATION") %>
                   <%    }
                       } %>
    </td>
    <td width="12%"><% if(sortby.equals(SORTBY_STATUS_ACC)){ %>
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("downarrow.gif") %>' border="0" name="<%=SORTBY_STATUS_DEC %>" value="submit" ><%= ejbcawebbean.getText("STATUS") %>              
                   <% }else{
                         if(sortby.equals(SORTBY_STATUS_DEC)){ %>
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("uparrow.gif") %>' border="0" name="<%=SORTBY_STATUS_ACC %>" value="submit" ><%= ejbcawebbean.getText("STATUS") %>                     
                   <%    }else{ %> 
                          <input type="image" src='<%= ejbcawebbean.getImagefileInfix("noarrow.gif") %>' border="0" name="<%=SORTBY_STATUS_ACC %>" value="submit" ><%= ejbcawebbean.getText("STATUS") %>
                   <%    }
                       } %>
    </td>
    <td width="18%"> &nbsp;
    </td>
  </tr>
  <% if(blank){ %>
 <tr id="Row0"> 
   <td width="5%"> 
   </td>
    <td width="11%">&nbsp;</td>
    <td width="19%">&nbsp;</td>
    <td width="17%">&nbsp;</td>
    <td width="18%">&nbsp;</td>
    <td width="12%">&nbsp;</td>
    <td width="18%">&nbsp;</td>
  </tr> 
  <% }else{
       if(users == null || users.length == 0){     %>
  <tr id="Row0"> 
   <td width="8%"> 
   </td>
    <td width="11%">&nbsp;</td>
    <td width="19%"><%= ejbcawebbean.getText("NOENDENTITIESFOUND") %></td>
    <td width="17%">&nbsp;</td>
    <td width="18%">&nbsp;</td>
    <td width="12%">&nbsp;</td>
    <td width="18%">&nbsp;</td>
  </tr>
  <% } else{
         for(int i=0; i < users.length; i++){%>
  <tr id="Row<%= i%2 %>"> 
      <td width="5%"> 
        <div align="center">
          <input type="checkbox" name="<%= CHECKBOX_SELECT_USER + i %>" value="<%= CHECKBOX_VALUE %>">
        </div>
      </td>
    <td width="11%"><%= users[i].getUsername() %>
       <input type="hidden" name='<%= HIDDEN_USERNAME + i %>' value='<%= users[i].getUsername() %>'>
    </td>
    <td width="19%"><%= users[i].getSubjectDNField(DNFieldExtractor.CN,0) %></td>
    <td width="17%"><%= users[i].getSubjectDNField(DNFieldExtractor.OU,0) %></td>
    <td width="18%"><%= users[i].getSubjectDNField(DNFieldExtractor.O,0) %></td>
    <td width="12%"><%  switch(users[i].getStatus()){
                          case UserDataRemote.STATUS_NEW :
                            out.write(ejbcawebbean.getText("STATUSNEW"));
                            break;
                          case UserDataRemote.STATUS_FAILED :
                            out.write(ejbcawebbean.getText("STATUSFAILED"));
                            break;
                          case UserDataRemote.STATUS_INITIALIZED :
                            out.write(ejbcawebbean.getText("STATUSINITIALIZED"));
                            break;
                          case UserDataRemote.STATUS_INPROCESS :
                            out.write(ejbcawebbean.getText("STATUSINPROCESS"));
                            break;
                          case UserDataRemote.STATUS_GENERATED :
                            out.write(ejbcawebbean.getText("STATUSGENERATED"));
                            break;
                          case UserDataRemote.STATUS_REVOKED :
                            out.write(ejbcawebbean.getText("STATUSREVOKED"));
                            break;
                          case UserDataRemote.STATUS_HISTORICAL :
                            out.write(ejbcawebbean.getText("STATUSHISTORICAL"));
                            break;
                          case UserDataRemote.STATUS_KEYRECOVERY :
                            out.write(ejbcawebbean.getText("STATUSKEYRECOVERY"));
                            break;
                        }%></td>
      <td width="18%">

        <A  onclick='viewuser(<%= i %>)'>
        <u><%= ejbcawebbean.getText("VIEWENDENTITY") %></u> </A> 
      <% try{ 
           if((rabean.authorizedToEditUser(users[i].getEndEntityProfileId()) || !globalconfiguration.getEnableEndEntityProfileLimitations())
               && ejbcawebbean.isAuthorizedNoLog(EjbcaWebBean.AUTHORIZED_RA_EDIT_RIGHTS)){ %>
        <A  onclick='edituser(<%= i %>)'>
        <u><%= ejbcawebbean.getText("EDITENDENTITY") %></u> </A>
        <% } 
         }catch(AuthorizationDeniedException ade){} 
         try{ 
           if(ejbcawebbean.isAuthorizedNoLog(EjbcaWebBean.AUTHORIZED_CA_VIEW_CERT)){ %>
        <A  onclick='viewcert(<%= i %>)'>
        <u><%= ejbcawebbean.getText("VIEWCERTIFICATES") %></u> </A>
        <% }
         }catch(AuthorizationDeniedException ade){}
         try{ 
           if(globalconfiguration.getIssueHardwareTokens() &&
              (rabean.authorizedToViewHardToken(users[i].getEndEntityProfileId()) || !globalconfiguration.getEnableEndEntityProfileLimitations())
               && ejbcawebbean.isAuthorizedNoLog(EjbcaWebBean.AUTHORIZED_HARDTOKEN_VIEW_RIGHTS)){ %>
        <A  onclick='viewtoken(<%= i %>)'>
        <u><%= ejbcawebbean.getText("VIEWHARDTOKENS") %></u> </A>
        <% }
         }catch(AuthorizationDeniedException ade){}
         try{ 
           if((rabean.authorizedToViewHistory(users[i].getEndEntityProfileId()) || !globalconfiguration.getEnableEndEntityProfileLimitations()) 
               && ejbcawebbean.isAuthorizedNoLog(EjbcaWebBean.AUTHORIZED_RA_HISTORY_RIGHTS)){ %>
        <A  onclick='viewhistory(<%= i %>)'>
        <u><%= ejbcawebbean.getText("VIEWHISTORY") %></u> </A>
        <%   } 
           }catch(AuthorizationDeniedException ade){} %>
      </td>
  </tr>
 <%      }
       }
     } %>
</table>
  <table width="100%" border="0" cellspacing="1" cellpadding="0">
    <tr>
      <td width="14%">
        <% if(rabean.previousButton(record,size)){ %>
          <input type="submit" name="<%=BUTTON_PREVIOUS %>" value="<%= ejbcawebbean.getText("PREVIOUS") %>">
        <% } %>
      </td>
      <td width="76%"> 
        <div align="center">
          <input type="button" name="<%=BUTTON_SELECTALL %>" value="<%= ejbcawebbean.getText("SELECTALL") %>"
                onClick='checkAll("document.form.<%= CHECKBOX_SELECT_USER %>", <%= numcheckboxes %>)'>
          <input type="button" name="<%=BUTTON_DESELECTALL %>" value="<%= ejbcawebbean.getText("UNSELECTALL") %>"
                onClick='uncheckAll("document.form.<%= CHECKBOX_SELECT_USER %>", <%= numcheckboxes %>)'>
          <input type="button" name="<%=BUTTON_INVERTSELECTION %>" value="<%= ejbcawebbean.getText("INVERTSELECTION") %>"           
                 onClick='switchAll("document.form.<%= CHECKBOX_SELECT_USER %>", <%= numcheckboxes %>)'>
        </div>
      </td>
      <td width="10%"> 
        <div align="right">
        <% if(rabean.nextButton(record,size)){ %>
          <input type="submit" name="<%=BUTTON_NEXT %>" value="<%= ejbcawebbean.getText("NEXT") %>">
        <% } %>
        </div>
      </td>
    </tr>
  </table>
  <table width="100%" border="0" cellspacing="1" cellpadding="0">
    <tr>
      <td  valign="top">
      <% try{ 
           if(ejbcawebbean.isAuthorizedNoLog(EjbcaWebBean.AUTHORIZED_RA_DELETE_RIGHTS)){ %>
        <input type="submit" name="<%=BUTTON_DELETE_USERS %>" value="<%= ejbcawebbean.getText("DELETESELECTED") %>"
               onClick='return confirmdelete()'>
       <%   } 
          }catch(AuthorizationDeniedException ade){} %>
        &nbsp;&nbsp;&nbsp;
      </td>
      <td  valign="top">
      <% try{ 
           if(ejbcawebbean.isAuthorizedNoLog(EjbcaWebBean.AUTHORIZED_RA_REVOKE_RIGHTS)){ %>
        <input type="submit" name="<%=BUTTON_REVOKE_USERS %>" value="<%= ejbcawebbean.getText("REVOKESELECTED") %>"
               onClick='return confirmrevokation()'><br>
        <select name="<%=SELECT_REVOKE_REASON %>" >
          <% for(int i=0; i < RevokedInfoView.reasontexts.length; i++){ 
               if(i!= 7){%>
               <option value='<%= i%>'><%= ejbcawebbean.getText(RevokedInfoView.reasontexts[i]) %></option>
          <%   } 
             }
            } 
          }catch(AuthorizationDeniedException ade){} %>
        </select>
        &nbsp;&nbsp;&nbsp;
      </td>
      <td  valign="top">
      <%/* try{ 
           if(ejbcawebbean.isAuthorizedNoLog(EjbcaWebBean.AUTHORIZED_RA_EDIT_RIGHTS)){ %>
        <input type="submit" name="<%=BUTTON_CHANGESTATUS %>" value="<%= ejbcawebbean.getText("CHANGESTATUSTO") %>"
               onClick='return confirm("<%= ejbcawebbean.getText("AREYOUSURECHANGE") %>")'><br>
        <select name="<%=SELECT_CHANGE_STATUS %>">
         <option selected value='<%= Integer.toString(UserDataRemote.STATUS_NEW) %>'><%= ejbcawebbean.getText("STATUSNEW") %></option>
     <!--   <option value='<%= Integer.toString(UserDataRemote.STATUS_FAILED) %>'><%= ejbcawebbean.getText("STATUSFAILED") %></option>  -->
       <!--  <option value='<%= Integer.toString(UserDataRemote.STATUS_INITIALIZED) %>'><%= ejbcawebbean.getText("STATUSINITIALIZED") %></option>  -->
       <!--  <option value='<%= Integer.toString(UserDataRemote.STATUS_INPROCESS) %>'><%= ejbcawebbean.getText("STATUSINPROCESS") %></option>  -->
            <option value='<%= Integer.toString(UserDataRemote.STATUS_GENERATED) %>'><%= ejbcawebbean.getText("STATUSGENERATED") %></option>  
        <!--  <option value='<%= Integer.toString(UserDataRemote.STATUS_REVOKED) %>'><%= ejbcawebbean.getText("STATUSREVOKED") %></option>  -->
         <option value='<%= Integer.toString(UserDataRemote.STATUS_HISTORICAL) %>'><%= ejbcawebbean.getText("STATUSHISTORICAL") %></option>
        </select>
       <% }  
        }catch(AuthorizationDeniedException ade){} */%>&nbsp;
      </td>
    </tr>
  </table>
  <% } %>
  </form>
  <%// Include Footer 
   String footurl =   globalconfiguration.getFootBanner(); %>
   
  <jsp:include page="<%= footurl %>" />
</body>
</html>
