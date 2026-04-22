package com.example.jobico.dto;

public class DashboardStatsResponse {

    private long totalCandidates;
    private long totalSelected;
    private long totalOnboarded;
    private long totalActiveEmployees;
    private double totalPayrollThisMonth;

    // Pipeline counts
    private long applied;
    private long shortlisted;
    private long selected;
    private long rejected;

    public long getTotalCandidates() { return totalCandidates; }
    public void setTotalCandidates(long totalCandidates) { this.totalCandidates = totalCandidates; }

    public long getTotalSelected() { return totalSelected; }
    public void setTotalSelected(long totalSelected) { this.totalSelected = totalSelected; }

    public long getTotalOnboarded() { return totalOnboarded; }
    public void setTotalOnboarded(long totalOnboarded) { this.totalOnboarded = totalOnboarded; }

    public long getTotalActiveEmployees() { return totalActiveEmployees; }
    public void setTotalActiveEmployees(long totalActiveEmployees) { this.totalActiveEmployees = totalActiveEmployees; }

    public double getTotalPayrollThisMonth() { return totalPayrollThisMonth; }
    public void setTotalPayrollThisMonth(double totalPayrollThisMonth) { this.totalPayrollThisMonth = totalPayrollThisMonth; }

    public long getApplied() { return applied; }
    public void setApplied(long applied) { this.applied = applied; }

    public long getShortlisted() { return shortlisted; }
    public void setShortlisted(long shortlisted) { this.shortlisted = shortlisted; }

    public long getSelected() { return selected; }
    public void setSelected(long selected) { this.selected = selected; }

    public long getRejected() { return rejected; }
    public void setRejected(long rejected) { this.rejected = rejected; }
}
