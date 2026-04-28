package com.stc.timeManagement;

import java.util.ArrayList;
import java.util.List;

import com.stc.usermanagement.Employee;

public class MonthlyTimeSheet {

    private int month;
    private int year;
    private boolean isSubmitted;
    private boolean isReadOnly;
    private List<TimeEntry> entries;
    private ApprovalWorkflow workflow;
    private Employee employee;

    public MonthlyTimeSheet(int month, int year, Employee employee) {
        this.month = month;
        this.year = year;
        this.employee = employee;
        this.isSubmitted = false;
        this.isReadOnly = false;
        this.entries = new ArrayList<>();
        this.workflow = new ApprovalWorkflow(employee);
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public Employee getEmployee() {
        return employee;
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
        if (entries.isEmpty()) {
            System.out.println("Cannot submit an empty time sheet.");
            return;
        }

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
