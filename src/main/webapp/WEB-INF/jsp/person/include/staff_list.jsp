<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@taglib prefix="uv" tagdir="/WEB-INF/tags" %>

<spring:url var="URL_PREFIX" value="/web" />

<div class="print-info--only-landscape">
    <h4><spring:message code="print.info.landscape" /></h4>
</div>

<p class="pull-right visible-print">
    <spring:message code="Effective"/> <uv:date date="${now}" />
</p>

<script type="text/javascript">
    $(document).ready(function() {

        $("table.sortable").tablesorter({
            sortList: [[1,0]],
            headers: {
                3: { sorter: 'commaNumber' },
                4: { sorter: 'commaNumber' },
                5: { sorter: 'commaNumber' },
                6: { sorter: 'commaNumber' }
            }
        });

    });
</script>

<table cellspacing="0" class="list-table selectable-table sortable tablesorter print--only-landscape">
    <thead class="hidden-xs hidden-sm">
    <tr>
        <th><%-- placeholder to ensure correct number of th --%></th>
        <th class="sortable-field"><spring:message code="firstname" /></th>
        <th class="sortable-field"><spring:message code="lastname" /></th>
        <th class="sortable-field is-centered"><spring:message code='overview.entitlement.per.year' /></th>
        <th class="sortable-field is-centered"><spring:message code='overview.actual.entitlement' /></th>
        <th class="sortable-field is-centered"><spring:message code='overview.remaining.days.last.year' /></th>
        <th class="sortable-field is-centered"><spring:message code="left"/></th>
        <sec:authorize access="hasRole('OFFICE')">
            <th><%-- placeholder to ensure correct number of th --%></th>
        </sec:authorize>    
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${persons}" var="person" varStatus="loopStatus">
        <tr onclick="navigate('${URL_PREFIX}/staff/${person.id}/overview');">
            <td class="is-centered">
                <img class="img-circle hidden-print" src="<c:out value='${gravatarUrls[person]}?d=mm&s=60'/>"/>
            </td>
            <td><c:out value="${person.firstName}"/></td>
            <td><c:out value="${person.lastName}"/></td>
            <td class="is-centered hidden-xs hidden-sm">
                <c:choose>
                    <c:when test="${accounts[person] != null}">
                        <uv:number number="${accounts[person].annualVacationDays}"/>
                    </c:when>
                    <c:otherwise>
                        <spring:message code='not.specified'/>
                    </c:otherwise>
                </c:choose>
            </td>
            <td class="is-centered hidden-xs hidden-sm">
                <c:choose>
                    <c:when test="${accounts[person] != null}">
                        <uv:number number="${accounts[person].vacationDays}"/>
                    </c:when>
                    <c:otherwise>
                        <spring:message code='not.specified'/>
                    </c:otherwise>
                </c:choose>
            </td>
            <td class="is-centered hidden-xs hidden-sm">
                <c:choose>
                    <c:when test="${accounts[person] != null}">
                        <uv:number number="${accounts[person].remainingVacationDays}"/>
                    </c:when>
                    <c:otherwise>
                        <spring:message code='not.specified'/>
                    </c:otherwise>
                </c:choose>
            </td>
            <td class="is-centered hidden-xs hidden-sm">
                <c:choose>
                    <c:when test="${leftDays[person] != null && remLeftDays[person] != null}">
                        <uv:number number="${leftDays[person]}"/>
                        <c:if test="${beforeApril || !accounts[person].remainingVacationDaysExpire}">
                            +
                            <uv:number number="${remLeftDays[person]}"/>
                        </c:if>
                    </c:when>
                    <c:otherwise>
                        <spring:message code='not.specified'/>
                    </c:otherwise>
                </c:choose>
            </td>
            <sec:authorize access="hasRole('OFFICE')">
            <td class="is-centered hidden-print"><a href="${URL_PREFIX}/staff/${person.id}/edit"><i class="fa fa-pencil fa-action" /></a></td>
            </sec:authorize>
        </tr>    
    </c:forEach>
    </tbody>
</table>

