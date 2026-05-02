package com.example.jobico.dto;

import java.util.List;

public class BulkUploadResponse {

    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<String> errors;
    private List<PayrollResponse> processedPayrolls;

    public BulkUploadResponse() {}

    public BulkUploadResponse(int totalRows, int successCount, int failureCount,
                               List<String> errors, List<PayrollResponse> processedPayrolls) {
        this.totalRows = totalRows;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.errors = errors;
        this.processedPayrolls = processedPayrolls;
    }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
    public List<PayrollResponse> getProcessedPayrolls() { return processedPayrolls; }
    public void setProcessedPayrolls(List<PayrollResponse> processedPayrolls) { this.processedPayrolls = processedPayrolls; }
}