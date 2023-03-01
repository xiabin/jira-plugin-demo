package com.example.demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
public class GanttIssue {
    private Long id;
    private String key;
    private String summary;
    private String assignee;
    private String startDate;
    private String endDate;

    private String dependency;


    private List<GanttIssue> children;


    public void addChild(GanttIssue ganttIssue) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(ganttIssue);
    }

}
