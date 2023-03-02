package com.example.demo.servlet;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.query.Query;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.example.demo.entity.GanttCustomFiled;
import com.example.demo.entity.GanttIssue;
import com.example.demo.entity.GanttIssueTree;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
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
    private IssueService issueService;

    @JiraImport
    private JiraAuthenticationContext authenticationContext;

    public Gantte(TemplateRenderer templateRenderer, PageBuilderService pageBuilderService, SearchService searchService, IssueService issueService, JiraAuthenticationContext authenticationContext) {
        this.templateRenderer = templateRenderer;
        this.pageBuilderService = pageBuilderService;
        this.searchService = searchService;
        this.authenticationContext = authenticationContext;
        this.issueService = issueService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, RuntimeException {

        Locale userLocale = ComponentAccessor.getJiraAuthenticationContext().getLocale();

        ApplicationUser user = authenticationContext.getLoggedInUser();
        log.info("user is {}", user.toString());
        Map<String, Object> context = new HashMap<>();

        IssueInputParameters issueInputParameters = issueService.newIssueInputParameters();
        String key = req.getParameter("key");
        log.info("Key is {}", key);

        MutableIssue mutableIssue = issueService.getIssue(user, key).getIssue();

        Long startDateNameCustomFiledId = Long.valueOf(req.getParameter("startDateCustomFileId"));
        log.info("startDateNameCustomFiledId is {}", startDateNameCustomFiledId);


        Long endDateNameCustomFiledId = Long.valueOf(req.getParameter("endDateNameCustomFiledId"));
        log.info("endDateNameCustomFiledId is {}", endDateNameCustomFiledId);

        String startDateString = req.getParameter("startDateString");
        // 检查参数是否为空
        if (startDateString == null || startDateString.trim().isEmpty()) {
            // 参数为空，抛出异常
            throw new IllegalArgumentException("startDateString 不能为空");
        }

        log.info("startDateString is {}", startDateString);

        String endDateString = req.getParameter("endDateString");
        // 检查参数是否为空
        if (endDateString == null || endDateString.trim().isEmpty()) {
            // 参数为空，抛出异常
            throw new IllegalArgumentException("endDateString 不能为空");
        }

        log.info("startDateString is {}", endDateString);

        CustomField startCustomField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(startDateNameCustomFiledId);
        CustomField endCustomField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(endDateNameCustomFiledId);
        log.info("startCustomField is {} , endCustomField is {} ", startCustomField, endCustomField);

        if (startCustomField != null && endCustomField != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/MMM/yy", userLocale);
            LocalDate startDate = LocalDate.parse(startDateString.substring(0, 10));
            LocalDate endDate = LocalDate.parse(endDateString.substring(0, 10));

            String startFormattedDate = startDate.format(formatter);
            String endFormattedDate = endDate.format(formatter);

            issueInputParameters.addCustomFieldValue(startCustomField.getId(), startFormattedDate);
            issueInputParameters.addCustomFieldValue(endCustomField.getId(), endFormattedDate);
        } else {
            log.error("startCustomField or endCustomField is null");
        }


        IssueService.UpdateValidationResult result = issueService.validateUpdate(user, mutableIssue.getId(), issueInputParameters);

        resp.setContentType("application/json;charset=UTF-8");

        JSONObject json = new JSONObject();
        if (result.getErrorCollection().hasAnyErrors()) {
            try {
                json.put("status", false);
                json.put("errors", result.getErrorCollection().getErrorMessages().stream().findFirst());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            issueService.update(user, result);

            try {
                json.put("status", true);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        PrintWriter out = resp.getWriter();
        out.print(json.toString());
        out.flush();
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
        log.info("jql:{}", jql);
        pageBuilderService.assembler().resources().requireWebResource("com.example.demo.demo:gantt-resources");
        List<Issue> issues = null;
        try {
            issues = getIssuesByJql(jql);
        } catch (JqlParseException e) {
            e.printStackTrace();
        }

        List<GanttIssue> ganttIssueList = new ArrayList<>();

        Locale userLocale = ComponentAccessor.getJiraAuthenticationContext().getLocale();

        HashMap<String, String> childParentMap = this.getParentChildRelations(issues);
        log.info("issues is {}", issues.toString());
        for (Issue issue : issues) {

            log.info("key is {}", issue.getKey());
            GanttIssue ganttIssue = new GanttIssue();
            ganttIssue.setKey(issue.getKey());
            ganttIssue.setSummary(issue.getSummary());

            ganttIssue.setId(issue.getId());
            ApplicationUser assignee = issue.getAssignee();
            if (assignee != null) {
                ganttIssue.setAssignee(issue.getAssignee().getDisplayName());
            }
            CustomFieldManager customFieldManager = ComponentAccessor.getCustomFieldManager();


            ganttIssue.setStartDate(new GanttCustomFiled());
            ganttIssue.setEndDate(new GanttCustomFiled());
            ganttIssue.getStartDate().setCustomFiledId(10000L);
            ganttIssue.getEndDate().setCustomFiledId(10001L);

            CustomField customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(ganttIssue.getStartDate().getCustomFiledId());
            if (customField != null) {
                Object customFieldValue = issue.getCustomFieldValue(customField);
                ganttIssue.getStartDate().setValue(customFieldValue.toString());
            }

            if (ganttIssue.getStartDate().getValue().equals(null)) {
                throw new NullPointerException(String.format("customField is  undefine %l", ganttIssue.getStartDate().getCustomFiledId()));
            }

            customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObject(ganttIssue.getEndDate().getCustomFiledId());
            if (customField != null) {
                Object customFieldValue = issue.getCustomFieldValue(customField);
                ganttIssue.getEndDate().setValue(customFieldValue.toString());
            }


            if (ganttIssue.getEndDate().getValue().equals(null)) {
                throw new NullPointerException(String.format("customField is  undefine %l", ganttIssue.getEndDate().getCustomFiledId()));
            }

//            String customFieldName = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("StartDate", userLocale);
//            CustomField customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName(customFieldName).stream()
//                .findFirst()
//                .orElse(null);
//            if (customField != null) {
//                Object customFieldValue = issue.getCustomFieldValue(customField);
//                ganttIssue.setStartDate(customFieldValue.toString());
//            } else {
//                throw new NullPointerException(String.format("customFieldName is StartDate userLocale is %s after translation is %s", userLocale.toString(), customFieldName));
//            }
//
//            customFieldName = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("EndDate", userLocale);
//            customField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectsByName(customFieldName).stream()
//                .findFirst()
//                .orElse(null);
//            if (customField != null) {
//                Object customFieldValue = issue.getCustomFieldValue(customField);
//                ganttIssue.setEndDate(customFieldValue.toString());
//            } else {
//                throw new NullPointerException(String.format("customFieldName is EndDate userLocale is %s after translation is %s", userLocale.toString(), customFieldName));
//            }
            if (childParentMap.containsKey(ganttIssue.getKey())) {
                ganttIssue.setDependency(childParentMap.get(ganttIssue.getKey()));
            } else {
                ganttIssue.setDependency("root");
            }
            ganttIssueList.add(ganttIssue);
        }

        //构建父子树
        List<GanttIssue> ganttIssueTree = this.buildTree(ganttIssueList);

        ganttIssueList = this.flattenTree(ganttIssueTree);

        context.put("ganttIssueList", ganttIssueList);
        context.put("ganttIssueListJson", new Gson().toJson(ganttIssueList));


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


    private HashMap<String, String> getParentChildRelations(List<Issue> issues) {

        String linkTypeName = "Parent Child";
        HashMap<String, String> childParentMap = new HashMap<>();
        IssueLinkManager issueLinkManager = ComponentAccessor.getIssueLinkManager();

        for (Issue issue : issues) {
            List<IssueLink> inwardLinks = issueLinkManager.getInwardLinks(issue.getId());
            if (inwardLinks.size() > 0) {
                log.info("issue is {} inwardLinks is {}", issue.toString(), inwardLinks.toString());
            }
            for (IssueLink link : inwardLinks) {
                log.info("{} link type name is {}", issue.toString(), link.getIssueLinkType().getName());
                if (link.getIssueLinkType().getName().equals(linkTypeName)) {
                    Issue linkedIssue = link.getSourceObject();
                    childParentMap.put(issue.getKey(), linkedIssue.getKey());
                }
            }
        }
        log.info("childParentMap is {}", childParentMap.toString());
        return childParentMap;
    }

    private List<GanttIssue> buildTree(List<GanttIssue> ganttIssueList) {
        Map<String, List<GanttIssue>> parentChildMap = new HashMap<>();

        // 将所有节点的父子关系映射成map
        for (GanttIssue ganttIssue : ganttIssueList) {
            List<GanttIssue> children = parentChildMap.computeIfAbsent(ganttIssue.getDependency(), k -> new ArrayList<>());
            children.add(ganttIssue);

            //排序下 children 这里还有优化空间 可以等映射关系构建完了再排序 而不是每次都排序 TreeSet可以解决
            //todo 优化
            Collections.sort(children, Comparator.comparingLong(GanttIssue::getId));

        }
        List<GanttIssue> rootList = parentChildMap.get("root");

        // 递归构建树
        for (GanttIssue node : rootList) {
            this.buildTreeRecursively(node, parentChildMap);
        }
        return rootList;
    }


    private void buildTreeRecursively(GanttIssue parenGanttIssue, Map<String, List<GanttIssue>> parentChildMap) {
        List<GanttIssue> children = parentChildMap.get(parenGanttIssue.getKey());
        if (children != null) {
            parenGanttIssue.setChildren(children);
            for (GanttIssue child : children) {
                buildTreeRecursively(child, parentChildMap);
            }
        }
    }

    public static List<GanttIssue> flattenTree(List<GanttIssue> ganttIssueList) {
        List<GanttIssue> flattened = new ArrayList<>();
        for (GanttIssue node : ganttIssueList) {
            flattened.add(node);
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                flattened.addAll(flattenTree(node.getChildren()));
            }
        }
        return flattened;
    }


}

