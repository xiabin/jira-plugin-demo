package com.example.demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class GanttIssue {
    private String key;
    private String summary;
    private String assignee;
    private String startDate;
    private String endDate;

}
