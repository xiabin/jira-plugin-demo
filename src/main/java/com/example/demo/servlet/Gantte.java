package com.example.demo.servlet;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.query.Query;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.example.demo.entity.GanttIssue;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Gantte extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(Gantte.class);

    @JiraImport
    private TemplateRenderer templateRenderer;

    @JiraImport
    private PageBuilderService pageBuilderService;

    @JiraImport
    private SearchService searchService;
    @JiraImport
    private JiraAuthenticationContext authenticationContext;

    public Gantte(TemplateRenderer templateRenderer, PageBuilderService pageBuilderService, SearchService searchService, JiraAuthenticationContext authenticationContext) {
        this.templateRenderer = templateRenderer;
        this.pageBuilderService = pageBuilderService;
        this.searchService = searchService;
        this.authenticationContext = authenticationContext;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8");
        Map<String, Object> context = new HashMap<>();

        String jql = request.getParameter("jql");
        // 检查参数是否为空
        if (jql == null || jql.trim().isEmpty()) {
            // 参数为空，抛出异常
            throw new IllegalArgumentException("jql不能为空");
        }
        log.error("jql:{}", jql);
        pageBuilderService.assembler().resources().requireWebResource("com.example.demo.demo:gantt-resources");
        List<Issue> issues = null;
        try {
            issues = getIssuesByJql(jql);
        } catch (JqlParseException e) {
            e.printStackTrace();
        }

        List<GanttIssue> ganttIssueList = new ArrayList<>();

        for (Issue issue : issues) {
            GanttIssue ganttIssue = new GanttIssue();
            ganttIssue.setKey(issue.getKey());
            ganttIssue.setSummary(issue.getSummary());
            ganttIssue.setAssignee(issue.getAssignee().getDisplayName());
            CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();

            CustomField customField = customFieldManager.getCustomFieldObjectByName("开始时间");
            log.error("debug 开始时间 {}",customField);
            Object customFieldValue = issue.getCustomFieldValue(customField);
            log.error("debug  开始时间{}",customFieldValue);
            ganttIssue.setStartDate(customFieldValue.toString());

             customField = customFieldManager.getCustomFieldObjectByName("结束时间");
            log.error("debug 结束时间 {}",customField);
            customFieldValue = issue.getCustomFieldValue(customField);
            log.error("debug  结束时间{}",customFieldValue);
            ganttIssue.setEndDate(customFieldValue.toString());
            ganttIssueList.add(ganttIssue);
        }
        String issuesJson = issues.toString();
        log.error("issues:{}", issues.toString());
        log.error("issuesJson:{}", issues.toString());
        context.put("issues", issues);
        context.put("issuesJson", issuesJson);
        context.put("ganttIssueList", ganttIssueList);


        String templatePath = "/templates/gantte.vm";
        templateRenderer.render(templatePath, context, response.getWriter());
    }



    /**
     * Retrieve issues using simple JQL query project="TUTORIAL"
     * Pagination is set to unlimited
     *
     * @return List of issues
     */
    private List<Issue> getIssuesByJql(String jqlString) throws JqlParseException {

        ApplicationUser user = authenticationContext.getLoggedInUser();
        JqlClauseBuilder jqlClauseBuilder = JqlQueryBuilder.newClauseBuilder();
        JqlQueryParser jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
        Query query = jqlQueryParser.parseQuery(jqlString);
        PagerFilter pagerFilter = PagerFilter.getUnlimitedFilter();

        SearchResults searchResults = null;
        try {
            searchResults = searchService.search(user, query, pagerFilter);
        } catch (SearchException e) {
            e.printStackTrace();
        }
        List<Issue> issueList = (searchResults != null) ? searchResults.getResults() : null;
        return issueList;
    }
}
