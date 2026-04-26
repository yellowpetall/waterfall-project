package com.stc.timeManagement;

import java.util.ArrayList;
import java.util.List;

public class MonthlyTimeSheet {

    private int month;
    private int year;
    private boolean isSubmitted;
    private boolean isReadOnly;
    private List<TimeEntry> entries;
    private ApprovalWorkflow workflow;
    private int userId; //After Sena's part this will come from UserManagement

    public MonthlyTimeSheet(int month, int year, int userId) {
        this.month = month;
        this.year = year;
        this.userId = userId;
        this.isSubmitted = false;
        this.isReadOnly = false;
        this.entries = new ArrayList<>();
        this.workflow = new ApprovalWorkflow();
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isSubmitted() {
        return isSubmitted;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public List<TimeEntry> getEntries() {
        return entries;
    }

    public ApprovalWorkflow getWorkflow() {
        return workflow;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setSubmitted(boolean submitted) {
        isSubmitted = submitted;
    }

    public void setReadOnly(boolean readOnly) {
        isReadOnly = readOnly;
    }

    public void setEntries(List<TimeEntry> entries) {
        this.entries = entries;
    }

    public void setWorkflow(ApprovalWorkflow workflow) {
        this.workflow = workflow;
    }

    public void submit() {
        if (workflow.getStatus().equals("Pending") || workflow.getStatus().equals("Rejected")) {
            this.isSubmitted = true;
            this.isReadOnly = true;
        }
    }

    public void addTimeEntry(TimeEntry entry) {
        if (!isReadOnly) {
            entries.add(entry);
        }
    }

}
