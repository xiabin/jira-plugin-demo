package com.example.demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class GanttIssueTree {
    private String Key;
    private String parentId;

    private List<GanttIssue> children;


    public void addChild(GanttIssue ganttIssue) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(ganttIssue);
    }
}
