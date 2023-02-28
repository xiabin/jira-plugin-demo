package com.example.demo.servlet;

import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Gantte extends HttpServlet{
    private static final Logger log = LoggerFactory.getLogger(Gantte.class);

    @JiraImport
    private TemplateRenderer templateRenderer;

    @JiraImport
    private PageBuilderService pageBuilderService;

    public Gantte(TemplateRenderer templateRenderer, PageBuilderService pageBuilderService) {
        this.templateRenderer = templateRenderer;
        this.pageBuilderService = pageBuilderService;
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.setContentType("text/html;charset=utf-8");
        Map<String, Object> context = new HashMap<>();
        pageBuilderService.assembler().resources().requireWebResource("com.example.demo.demo:gantt-resources");

        String templatePath = "/templates/gantte.vm";
        templateRenderer.render(templatePath, context, response.getWriter());
    }

}
