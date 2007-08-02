<%@ page isErrorPage="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ include file="header.jsp" %>

    <c:set var="isException" value="${param.Exception}" />
    <c:set var="errMsg" value="${param.ErrorMessage}" />
    
    <h1 class="title">@EJBCA@ Certificate Enrollment Error</h1>
    
    <c:if test="${isException != null && isException == 'true'}">
        <h2>An Exception occured!</h2>
    </c:if>
    <c:choose> 
        <c:when test="${errMsg == null}"> 
            <h2>Unknown error, or you came to this page directly without beeing redirected.</h2> 
        </c:when>
        <c:otherwise> 
            <p><c:out value="${errMsg}" /></p>
        </c:otherwise> 
    </c:choose> 

    <p><a href="javascript:history.back()">Go back</a></p>
    
<%@ include file="footer.inc" %>
