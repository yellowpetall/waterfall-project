package com.stc.timeManagement;

public class ApprovalWorkflow {

    private String status;
    private String rejectionReason;
    private FlexTimeAccount flexTimeAccount;

    public ApprovalWorkflow() {
        this.status = "Pending";
        this.rejectionReason = "";
        this.flexTimeAccount = new FlexTimeAccount(0, 160, 0);
    }

    public String getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public FlexTimeAccount getFlexTimeAccount() {
        return flexTimeAccount;
    }

    public void approve(MonthlyTimeSheet sheet) {
        this.status = "Approved";
        double totalWorked = sheet.getEntries().stream().mapToDouble(TimeEntry::getWorkingHours).sum();
        this.flexTimeAccount.setActualHours(totalWorked);
        flexTimeAccount.updateBalance();
    }

    public void reject(String reason, MonthlyTimeSheet sheet) {
        this.status = "Rejected";
        this.rejectionReason = reason;
        sheet.setReadOnly(false);
        sheet.setSubmitted(false);
    }

    public void getStatusDetails() {
        System.out.println("Status: " + status);
        if (status.equals("Rejected")) {
            System.out.println("Rejection Reason: " + rejectionReason);
        }
        System.out.println("Flex Time Balance: " + flexTimeAccount.getBalance());
    }

}
