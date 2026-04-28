package com.stc.timeManagement;

import com.stc.usermanagement.Employee;
import com.stc.usermanagement.Supervisor;
import com.stc.usermanagement.User;

public class ApprovalWorkflow {

    private String status;
    private String rejectionReason;
    private Employee owner;
    private FlexTimeAccount flexTimeAccount;

    public ApprovalWorkflow(Employee owner) {
        this.status = "Pending";
        this.rejectionReason = "";
        this.owner = owner;
        this.flexTimeAccount = new FlexTimeAccount(0, 160, 0);
    }

    public String getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Employee getOwner() {
        return owner;
    }

    public FlexTimeAccount getFlexTimeAccount() {
        return flexTimeAccount;
    }

    public void approve(User reviewer, MonthlyTimeSheet sheet) {
        if (!(reviewer instanceof Supervisor)) {
            System.out.println("Only supervisors can approve time sheets.");
            return;
        }

        if (!sheet.isSubmitted() || !this.status.equals("Pending")) {
            System.out.println("Time sheet must be submitted and pending for approval.");
            return;
        }

        this.status = "Approved";
        this.rejectionReason = "";
        sheet.setReadOnly(true);

        double totalWorked = sheet.getEntries().stream().mapToDouble(TimeEntry::getWorkingHours).sum();

        this.flexTimeAccount.setActualHours(totalWorked);
        this.flexTimeAccount.updateBalance();

        System.out.println("Time sheet approved. Flex time balance updated.");
    }

    public void reject(User reviewer, String reason, MonthlyTimeSheet sheet) {
        if (!(reviewer instanceof Supervisor)) {
            System.out.println("Only supervisors can reject time sheets.");
            return;
        }

        this.status = "Rejected";
        this.rejectionReason = reason;

        sheet.setSubmitted(false);
        sheet.setReadOnly(false);

        System.out.println("Time sheet rejected. Reason: " + reason);
    }

    public void getStatusDetails() {
        System.out.println("Status: " + status);
        if (status.equals("Rejected")) {
            System.out.println("Rejection Reason: " + rejectionReason);
        }
        System.out.println("Flex Time Balance: " + flexTimeAccount.getBalance());
    }

}
