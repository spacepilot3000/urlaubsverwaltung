
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="joda" uri="http://www.joda.org/joda/time/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@taglib prefix="uv" tagdir="/WEB-INF/tags" %>


<!DOCTYPE html>
<html>

<head>
    <uv:head />
    <script type="text/javascript">
        $(document).ready(function() {

            $("table.sortable").tablesorter({
                sortList: [[1,0]]
            });
            
        });
        
    </script>
</head>

<body>

<spring:url var="URL_PREFIX" value="/web" />

<uv:menu />

<div class="print-info--only-landscape">
    <h4><spring:message code="print.info.landscape" /></h4>
</div>

<div class="content print--only-landscape">
    <div class="container">

        <div class="row">

            <div class="col-xs-12">

            <div class="header">

                <legend class="is-sticky">

                    <p>
                        <spring:message code="sicknotes" />
                    </p>

                    <uv:print />

                    <div class="btn-group btn-group-legend pull-right hidden-xs hidden-sm">
                        <a class="btn btn-default dropdown-toggle" data-toggle="dropdown" href="#">
                            <i class="fa fa-bar-chart"></i>&nbsp;<spring:message code='sicknotes.statistics.short' />
                            <span class="caret"></span>
                        </a>
                        <ul class="dropdown-menu">
                            <c:forEach begin="0" end="10" varStatus="counter">
                                <li>
                                    <a href="${URL_PREFIX}/sicknote/statistics?year=${today.year - counter.index}">
                                        <c:out value="${today.year - counter.index}" />
                                    </a>
                                </li> 
                            </c:forEach>
                        </ul>
                    </div>
                    <a class="btn btn-default pull-right" href="${URL_PREFIX}/sicknote/new">
                        <i class="fa fa-pencil"></i> <span class="hidden-xs"><spring:message code="action.apply.sicknote" /></span>
                    </a>
                </legend>

            </div>

            <uv:filter-modal id="filterModal" actionUrl="${URL_PREFIX}/sicknote/filter" />

            <div>
                <p class="is-inline-block">
                    <a href="#filterModal" data-toggle="modal">
                        <spring:message code="time"/>:&nbsp;<uv:date date="${from}"/> - <uv:date date="${to}"/>
                    </a>
                </p>
                <p class="pull-right visible-print">
                    <spring:message code="Effective"/> <uv:date date="${today}" />
                </p>
            </div>
                <table class="list-table selectable-table sortable tablesorter" cellspacing="0">
                    <thead class="hidden-xs hidden-sm">
                    <tr>
                        <th class="hidden-print"></th>
                        <th class="sortable-field"><spring:message code="firstname"/></th>
                        <th class="sortable-field"><spring:message code="lastname"/></th>
                        <th class="hidden"><%-- tablesorter placeholder for first name and last name column in xs screen --%></th>
                        <th class="sortable-field"><spring:message code="sicknotes.days.number"/></th>
                        <th class="sortable-field"><spring:message code="sicknotes.child.days.number"/></th>
                        <th class="hidden"><%-- tablesorter placeholder for sick days column in xs screen --%></th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach items="${persons}" var="person">
                    <tr onclick="navigate('${URL_PREFIX}/staff/${person.id}/overview#anchorSickNotes');">
                        <td class="is-centered hidden-print">
                            <img class="img-circle hidden-print" src="<c:out value='${gravatars[person]}?d=mm&s=60'/>"/>
                        </td>
                        <td class="hidden-xs">
                            <c:out value="${person.firstName}"/>
                        </td>
                        <td class="hidden-xs">
                            <c:out value="${person.lastName}"/>
                        </td>
                        <td class="visible-xs">
                            <c:out value="${person.firstName}"/> <c:out value="${person.lastName}"/>
                        </td>
                        <td class="hidden-xs">
                            <i class="fa fa-medkit hidden-print"></i>
                            <span class="sortable"><uv:number number="${sickDays[person]}"/></span>
                            <spring:message code="sicknotes.days"/>
                            <c:if test="${sickDaysWithAUB[person] > 0}">
                                <p class="list-table--second-row">
                                    <i class="fa fa-check check"></i> <spring:message
                                        code="overview.sicknotes.sickdays.aub" arguments="${sickDaysWithAUB[person]}"/>
                                </p>
                            </c:if>
                        </td>
                        <td class="hidden-xs">
                            <i class="fa fa-child hidden-print"></i> <uv:number number="${childSickDays[person]}"/>
                                <spring:message code="sicknotes.child.days"/>
                            <c:if test="${childSickDaysWithAUB[person] > 0}">
                                <p class="list-table--second-row">
                                    <i class="fa fa-check check"></i> <spring:message
                                            code="overview.sicknotes.sickdays.aub"
                                            arguments="${childSickDaysWithAUB[person]}"/>
                                </p>
                            </c:if>
                        </td>
                        <td class="visible-xs">
                            <i class="fa fa-medkit hidden-print"></i> <uv:number number="${sickDays[person]}"/>
                            <i class="fa fa-child hidden-print"></i> <uv:number number="${childSickDays[person]}"/>
                        </td>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

</body>

</html>
