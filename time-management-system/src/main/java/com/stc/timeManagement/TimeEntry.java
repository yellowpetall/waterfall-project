package com.stc.timeManagement;

import java.sql.Time;
import java.util.Date;

public class TimeEntry {

    private Date date;
    private Time startTime;
    private Time endTime;
    private double breakDuration;
    private String comment;
    private double workingHours;
    private CustomerProject project;

    public TimeEntry(Date date, Time startTime, Time endTime, double breakDuration, String comment, CustomerProject project) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.breakDuration = breakDuration;
        this.comment = comment;
        this.project = project;
        calculateWorkingHours();
    }

    public Date getDate() {
        return date;
    }

    public Time getStartTime() {
        return startTime;
    }

    public Time getEndTime() {
        return endTime;
    }

    public double getBreakDuration() {
        return breakDuration;
    }

    public String getComment() {
        return comment;
    }

    public double getWorkingHours() {
        return workingHours;
    }

    public CustomerProject getProject() {
        return project;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setStartTime(Time startTime) {
        this.startTime = startTime;
        calculateWorkingHours();
    }

    public void setEndTime(Time endTime) {
        this.endTime = endTime;
        calculateWorkingHours();
    }

    public void setBreakDuration(double breakDuration) {
        this.breakDuration = breakDuration;
        calculateWorkingHours();
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setProject(CustomerProject project) {
        this.project = project;
    }

    private double calculateWorkingHours() {
        long startMillis = startTime.getTime();
        long endMillis = endTime.getTime();
        double totalHours = (endMillis - startMillis) / (1000.0 * 60 * 60);
        this.workingHours = totalHours - breakDuration;
        return this.workingHours;
    }

}
