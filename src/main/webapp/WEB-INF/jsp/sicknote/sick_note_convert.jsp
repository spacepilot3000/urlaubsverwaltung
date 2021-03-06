<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@taglib prefix="uv" tagdir="/WEB-INF/tags" %>

<html>
<head>
    <uv:head />
</head>
<body>

<spring:url var="URL_PREFIX" value="/web"/>

<uv:menu />

<div class="content">

    <c:set var="METHOD" value="POST" />
    <c:set var="ACTION" value="${URL_PREFIX}/sicknote/${sickNote.id}/convert" />

    <form:form method="${METHOD}" action="${ACTION}" modelAttribute="appForm" class="form-horizontal">

    <div class="container">

        <div class="row">

            <div class="col-xs-12 col-sm-12 col-md-6">
                <div class="header">
                    <legend>
                        <p>
                            <spring:message code="sicknotes.convert.vacation" />
                        </p>
                    </legend>
                </div>
    
                    <div class="form-group">
                        <label class="control-label col-sm-12 col-md-4"><spring:message code='staff'/></label>
    
                        <div class="col-md-7">
                            <c:out value="${sickNote.person.firstName}" />&nbsp;<c:out value="${sickNote.person.lastName}" />
                        </div>
                    </div>

                    <div class="form-group">
                        <label class="control-label col-md-4">
                            <spring:message code="vac.type" />
                            <br />
                            <span class="help-inline"><form:errors path="vacationType" cssClass="error"/></span>
                        </label>

                        <div class="col-md-7">
                            <form:select path="vacationType" size="1" cssClass="form-control" cssErrorClass="form-control error">
                                <c:forEach items="${vacTypes}" var="vacType">
                                    <option value="${vacType}">
                                        <spring:message code='${vacType}' />
                                    </option>
                                </c:forEach>
                            </form:select>
                        </div>
                    </div>
    
                    <div class="form-group">
                        <label class="control-label col-md-4"><spring:message code="time" /></label>
    
                        <div class="col-md-7">
                            <uv:date date="${sickNote.startDate}" /> - <uv:date date="${sickNote.endDate}" />
                        </div>
                    </div>
    
                    <div class="form-group">
                        <label class="control-label col-md-4">
                            <spring:message code="reason" />
                        </label>

                        <div class="col-md-7">
                            <span id="count-chars"></span><spring:message code="max.chars" />
                            <br />
                            <form:textarea id="reason" path="reason" cssClass="form-control" cssErrorClass="form-control error" rows="2" onkeyup="count(this.value, 'count-chars');" onkeydown="maxChars(this,200); count(this.value, 'count-chars');" />
                            <span class="help-inline"><form:errors path="reason" cssClass="error"/></span>
                        </div>

                    </div>

            </div>

            <div class="col-xs-12 col-sm-12 col-md-6">

                <div class="header">
                    <legend>
                        <p><spring:message code="sicknote" /></p>
                    </legend>
                </div>

                <div class="box">
                    <span class="thirds">
                       <span class="box-icon bg-red">
                            <c:choose>
                                <c:when test="${sickNote.type == 'SICK_NOTE_CHILD'}">
                                    <i class="fa fa-child"></i>
                                </c:when>
                                <c:otherwise>
                                    <i class="fa fa-medkit"></i>
                                </c:otherwise>
                            </c:choose>
                        </span>

                        <h5 class="is-inline-block is-sticky"><c:out value="${sickNote.person.niceName}"/></h5>

                        <c:choose>
                            <c:when test="${sickNote.type == 'SICK_NOTE_CHILD'}">
                                <spring:message code="sicknotes.details.title.child" />
                            </c:when>
                            <c:otherwise>
                                <spring:message code="sicknotes.details.title" />
                            </c:otherwise>
                        </c:choose>

                        <c:choose>
                            <c:when test="${sickNote.startDate == sickNote.endDate}">
                                <spring:message code="at"/> <h5 class="is-inline-block is-sticky"><uv:date date="${sickNote.startDate}"/></h5>
                            </c:when>
                            <c:otherwise>
                                <spring:message code="from"/> <h5 class="is-inline-block is-sticky"><uv:date
                                    date="${sickNote.startDate}"/></h5> <spring:message code="to"/> <h5 class="is-inline-block is-sticky">
                                <uv:date date="${sickNote.endDate}"/></h5>
                            </c:otherwise>
                        </c:choose>
                    </span>
                </div>

                <table class="list-table striped-table bordered-table">
                    <tbody>
                    <tr>
                        <td>
                            <spring:message code="days.time"/>
                        </td>
                        <td>
                            = <uv:number number="${sickNote.workDays}"/> <spring:message
                                code="work.days"/>
                        </td>
                    </tr>
                    <tr>
                        <td><spring:message code="sicknotes.aub.short"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${sickNote.aubPresent}">
                                    <i class="fa fa-check hidden-print"></i>
                                    <uv:date date="${sickNote.aubStartDate}"/> - <uv:date
                                        date="${sickNote.aubEndDate}"/>
                                </c:when>
                                <c:otherwise>
                                    <i class="fa fa-remove hidden-print"></i>
                                    <spring:message code="no"/>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
            
        </div>

        <div class="row">

            <div class="col-xs-12">

                <hr/>

                <button class="btn btn-success col-xs-12 col-sm-5 col-md-2" type="submit"><i class='fa fa-check'></i>&nbsp;<spring:message code="action.save" /></button>
                <a class="btn btn-default col-xs-12 col-sm-5 col-md-2 pull-right" href="${URL_PREFIX}/sicknote/${sickNote.id}"><i class='fa fa-remove'></i>&nbsp;<spring:message code="action.cancel"/></a>


            </div>

        </div>
        
    </div>

</form:form>
</div>    

</body>
</html>