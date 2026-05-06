package com.example.jobico.service;

import com.example.jobico.dto.PayrollResponse;
import com.lowagie.text.DocumentException;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@Service
public class PayslipPdfService {

    private static final String COMPANY_NAME    = "Jobico Technologies Pvt. Ltd.";
    private static final String COMPANY_ADDRESS = "3rd Floor, Cyber Towers, Hitech City, Hyderabad \u2013 500081";
    private static final String COMPANY_EMAIL   = "hr@jobico.in";
    private static final String COMPANY_WEBSITE = "www.jobico.in";
    private static final String COMPANY_CIN     = "U74999TG2024PTC123456";

    public byte[] generatePayslipPdf(PayrollResponse p) throws DocumentException {
        String html = buildPayslipHtml(p);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(baos);
        return baos.toByteArray();
    }

    private String buildPayslipHtml(PayrollResponse p) {

        String monthName = (p.getMonth() >= 1 && p.getMonth() <= 12)
                ? Month.of(p.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                : "N/A";

        double basic      = p.getBasicSalary();
        double hra        = p.getHra();
        double allowances = p.getAllowances();
        double deductions = p.getDeductions();
        double gross      = basic + hra + allowances;
        double net        = p.getNetSalary();
        double pf         = deductions * 0.60;
        double otherDed   = deductions * 0.40;
        double totalDed   = pf + 200 + otherDed;
        String netWords   = numberToWords((long) net);

        String employeeName = p.getEmployeeName()  != null ? p.getEmployeeName()          : "N/A";
        String employeeId   = p.getEmployeeId()    != null ? p.getEmployeeId()             : "N/A";
        String department   = p.getDepartment()    != null ? p.getDepartment()             : "N/A";
        String paymentDate  = p.getPaymentDate()   != null ? p.getPaymentDate().toString() : "N/A";
        String payStatus    = p.getPaymentStatus() != null ? p.getPaymentStatus().name()   : "N/A";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n");
        sb.append("    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        sb.append("<head>\n");
        sb.append("  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n");
        sb.append("  <title>Payslip</title>\n");
        sb.append("  <style type=\"text/css\">\n");
        sb.append("    * { margin:0; padding:0; box-sizing:border-box; }\n");
        sb.append("    body { font-family:Arial,Helvetica,sans-serif; font-size:10pt; color:#2c2c2c; }\n");
        sb.append("    .page-wrapper { width:720px; margin:0 auto; background:#ffffff; border:1px solid #d0d7de; }\n");
        // Header
        sb.append("    .header { background-color:#0f2557; padding:20px 28px; color:#ffffff; }\n");
        sb.append("    .header-table { width:100%; display:table; }\n");
        sb.append("    .header-left  { display:table-cell; vertical-align:middle; }\n");
        sb.append("    .header-right { display:table-cell; vertical-align:middle; text-align:right; }\n");
        sb.append("    .company-name { font-size:16pt; font-weight:bold; color:#ffffff; }\n");
        sb.append("    .company-meta { font-size:8pt; color:#a8c7fa; margin-top:4px; }\n");
        sb.append("    .payslip-badge { background-color:#1a73e8; color:#ffffff; padding:5px 14px; font-size:9pt; font-weight:bold; letter-spacing:1px; }\n");
        sb.append("    .pay-period { font-size:11pt; color:#a8c7fa; margin-top:6px; font-weight:bold; }\n");
        // Subheader
        sb.append("    .subheader { background-color:#1a73e8; padding:8px 28px; color:#ffffff; font-size:8.5pt; display:table; width:100%; }\n");
        sb.append("    .sub-cell  { display:table-cell; padding-right:28px; }\n");
        sb.append("    .sub-label { color:#bdd7ff; }\n");
        // Section title
        sb.append("    .section-title { background-color:#e8f0fe; color:#0f2557; font-size:8.5pt; font-weight:bold; letter-spacing:1px; padding:6px 28px; border-top:2px solid #1a73e8; border-bottom:1px solid #c5d5f5; text-transform:uppercase; }\n");
        // Employee grid
        sb.append("    .emp-grid { display:table; width:100%; padding:14px 28px; border-bottom:1px solid #e2e8f0; }\n");
        sb.append("    .emp-col  { display:table-cell; width:33%; vertical-align:top; padding-right:10px; }\n");
        sb.append("    .info-label { font-size:7.5pt; color:#6b7280; text-transform:uppercase; letter-spacing:0.5px; margin-bottom:2px; }\n");
        sb.append("    .info-value { font-size:9.5pt; font-weight:bold; color:#1e293b; }\n");
        sb.append("    .info-item  { margin-bottom:10px; }\n");
        // Salary table
        sb.append("    .sal-table { width:100%; border-collapse:collapse; }\n");
        sb.append("    .sal-table th { background-color:#f1f5fb; color:#374151; font-size:8.5pt; font-weight:bold; padding:8px 28px; text-align:left; border-bottom:1px solid #dde3ee; }\n");
        sb.append("    .sal-table th.right { text-align:right; }\n");
        sb.append("    .sal-table td { padding:7px 28px; font-size:9pt; border-bottom:1px solid #f0f0f0; }\n");
        sb.append("    .sal-table td.right { text-align:right; }\n");
        sb.append("    .sal-table tr.total-row td { font-weight:bold; background-color:#f8faff; border-top:2px solid #1a73e8; }\n");
        sb.append("    .col-earn { width:50%; border-right:1px solid #e0e7f0; }\n");
        // Net salary box
        sb.append("    .net-box   { background-color:#0f2557; color:#ffffff; padding:14px 28px; display:table; width:100%; }\n");
        sb.append("    .net-left  { display:table-cell; vertical-align:middle; }\n");
        sb.append("    .net-right { display:table-cell; vertical-align:middle; text-align:right; }\n");
        sb.append("    .net-label  { font-size:8pt; color:#a8c7fa; }\n");
        sb.append("    .net-amount { font-size:18pt; font-weight:bold; color:#ffffff; }\n");
        sb.append("    .net-words  { font-size:8pt; color:#a8c7fa; margin-top:3px; }\n");
        sb.append("    .paid-badge { background-color:#22c55e; color:#ffffff; padding:5px 14px; font-size:9pt; font-weight:bold; }\n");
        // Footer
        sb.append("    .footer { background-color:#f8faff; padding:10px 28px; text-align:center; font-size:7.5pt; color:#9ca3af; border-top:1px solid #e2e8f0; }\n");
        sb.append("  </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("<div class=\"page-wrapper\">\n");

        // ── HEADER ──────────────────────────────────────────────────────
        sb.append("  <div class=\"header\">\n");
        sb.append("    <div class=\"header-table\">\n");
        sb.append("      <div class=\"header-left\">\n");
        sb.append("        <div class=\"company-name\">").append(escapeXml(COMPANY_NAME)).append("</div>\n");
        sb.append("        <div class=\"company-meta\">")
          .append(escapeXml(COMPANY_ADDRESS)).append(" &#160;|&#160; ")
          .append(escapeXml(COMPANY_EMAIL)).append(" &#160;|&#160; ")
          .append(escapeXml(COMPANY_WEBSITE)).append("</div>\n");
        sb.append("      </div>\n");
        sb.append("      <div class=\"header-right\">\n");
        sb.append("        <div class=\"payslip-badge\">SALARY SLIP</div>\n");
        sb.append("        <div class=\"pay-period\">").append(monthName).append(" ").append(p.getYear()).append("</div>\n");
        sb.append("      </div>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");

        // ── SUBHEADER ────────────────────────────────────────────────────
        sb.append("  <div class=\"subheader\">\n");
        sb.append("    <div class=\"sub-cell\"><span class=\"sub-label\">Employee ID: </span>").append(escapeXml(employeeId)).append("</div>\n");
        sb.append("    <div class=\"sub-cell\"><span class=\"sub-label\">Name: </span>").append(escapeXml(employeeName)).append("</div>\n");
        sb.append("    <div class=\"sub-cell\"><span class=\"sub-label\">Department: </span>").append(escapeXml(department)).append("</div>\n");
        sb.append("    <div class=\"sub-cell\"><span class=\"sub-label\">Pay Date: </span>").append(escapeXml(paymentDate)).append("</div>\n");
        sb.append("  </div>\n");

        // ── EMPLOYEE DETAILS ─────────────────────────────────────────────
        sb.append("  <div class=\"section-title\">Employee Details</div>\n");
        sb.append("  <div class=\"emp-grid\">\n");
        sb.append("    <div class=\"emp-col\">\n");
        sb.append("      <div class=\"info-item\"><div class=\"info-label\">Employee Name</div><div class=\"info-value\">").append(escapeXml(employeeName)).append("</div></div>\n");
        sb.append("      <div class=\"info-item\"><div class=\"info-label\">Employee ID</div><div class=\"info-value\">").append(escapeXml(employeeId)).append("</div></div>\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"emp-col\">\n");
        sb.append("      <div class=\"info-item\"><div class=\"info-label\">Department</div><div class=\"info-value\">").append(escapeXml(department)).append("</div></div>\n");
        sb.append("      <div class=\"info-item\"><div class=\"info-label\">Pay Period</div><div class=\"info-value\">").append(monthName).append(" ").append(p.getYear()).append("</div></div>\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"emp-col\">\n");
        sb.append("      <div class=\"info-item\"><div class=\"info-label\">Payment Date</div><div class=\"info-value\">").append(escapeXml(paymentDate)).append("</div></div>\n");
        sb.append("      <div class=\"info-item\"><div class=\"info-label\">Payment Status</div><div class=\"info-value\">").append(escapeXml(payStatus)).append("</div></div>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");

        // ── EARNINGS & DEDUCTIONS TABLE ──────────────────────────────────
        sb.append("  <div class=\"section-title\">Earnings &amp; Deductions</div>\n");
        sb.append("  <table class=\"sal-table\">\n");
        sb.append("    <thead><tr>\n");
        sb.append("      <th class=\"col-earn\">Earnings</th>\n");
        sb.append("      <th class=\"right col-earn\">Amount (&#8377;)</th>\n");
        sb.append("      <th>Deductions</th>\n");
        sb.append("      <th class=\"right\">Amount (&#8377;)</th>\n");
        sb.append("    </tr></thead>\n");
        sb.append("    <tbody>\n");
        sb.append("      <tr><td class=\"col-earn\">Basic Salary</td><td class=\"right col-earn\">").append(formatAmount(basic)).append("</td><td>Provident Fund (PF)</td><td class=\"right\">").append(formatAmount(pf)).append("</td></tr>\n");
        sb.append("      <tr><td class=\"col-earn\">House Rent Allowance (HRA)</td><td class=\"right col-earn\">").append(formatAmount(hra)).append("</td><td>Professional Tax</td><td class=\"right\">200.00</td></tr>\n");
        sb.append("      <tr><td class=\"col-earn\">Other Allowances</td><td class=\"right col-earn\">").append(formatAmount(allowances)).append("</td><td>Other Deductions</td><td class=\"right\">").append(formatAmount(otherDed)).append("</td></tr>\n");
        sb.append("      <tr class=\"total-row\"><td class=\"col-earn\">Gross Earnings</td><td class=\"right col-earn\">").append(formatAmount(gross)).append("</td><td>Total Deductions</td><td class=\"right\">").append(formatAmount(totalDed)).append("</td></tr>\n");
        sb.append("    </tbody>\n");
        sb.append("  </table>\n");

        // ── NET SALARY BOX ───────────────────────────────────────────────
        sb.append("  <div class=\"net-box\">\n");
        sb.append("    <div class=\"net-left\">\n");
        sb.append("      <div class=\"net-label\">NET SALARY PAYABLE</div>\n");
        sb.append("      <div class=\"net-amount\">&#8377; ").append(formatAmount(net)).append("</div>\n");
        sb.append("      <div class=\"net-words\">").append(escapeXml(netWords)).append(" Only</div>\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"net-right\"><div class=\"paid-badge\">").append(escapeXml(payStatus)).append("</div></div>\n");
        sb.append("  </div>\n");

        // ── FOOTER ───────────────────────────────────────────────────────
        sb.append("  <div class=\"footer\">This is a computer-generated payslip and does not require a signature. &#160;|&#160; CIN: ").append(COMPANY_CIN).append("</div>\n");

        sb.append("</div>\n</body>\n</html>\n");
        return sb.toString();
    }

    private String formatAmount(double amount) {
        return String.format("%,.2f", amount);
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&apos;");
    }

    private String numberToWords(long number) {
        if (number == 0) return "Zero Rupees";
        String[] ones = { "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
                "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
                "Sixteen", "Seventeen", "Eighteen", "Nineteen" };
        String[] tens = { "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety" };
        StringBuilder sb = new StringBuilder();
        long crore    = number / 10000000; number %= 10000000;
        long lakh     = number / 100000;   number %= 100000;
        long thousand = number / 1000;     number %= 1000;
        long hundred  = number / 100;      number %= 100;
        if (crore    > 0) sb.append(twoDigits(crore,    ones, tens)).append(" Crore ");
        if (lakh     > 0) sb.append(twoDigits(lakh,     ones, tens)).append(" Lakh ");
        if (thousand > 0) sb.append(twoDigits(thousand, ones, tens)).append(" Thousand ");
        if (hundred  > 0) sb.append(ones[(int) hundred]).append(" Hundred ");
        if (number   > 0) sb.append(twoDigits(number,   ones, tens));
        return sb.toString().trim() + " Rupees";
    }

    private String twoDigits(long n, String[] ones, String[] tens) {
        if (n < 20) return ones[(int) n];
        return tens[(int) (n / 10)] + (n % 10 != 0 ? " " + ones[(int) (n % 10)] : "");
    }
}