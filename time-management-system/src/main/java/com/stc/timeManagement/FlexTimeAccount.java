package com.stc.timeManagement;

public class FlexTimeAccount {

    private double balance;
    private double targetHours;
    private double actualHours;

    public FlexTimeAccount(double balance, double targetHours, double actualHours) {
        this.balance = balance;
        this.targetHours = targetHours;
        this.actualHours = actualHours;
    }

    public double getBalance() {
        return balance;
    }

    public double getTargetHours() {
        return targetHours;
    }

    public double getActualHours() {
        return actualHours;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setTargetHours(double targetHours) {
        this.targetHours = targetHours;
    }

    public void setActualHours(double actualHours) {
        this.actualHours = actualHours;
    }

    public void updateBalance() {
        this.balance = actualHours - targetHours;
    }

}
