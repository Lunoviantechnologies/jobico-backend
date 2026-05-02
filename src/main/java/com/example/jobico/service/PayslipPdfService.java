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
    private static final String COMPANY_ADDRESS = "3rd Floor, Cyber Towers, Hitech City, Hyderabad – 500081";
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
        String monthName = Month.of(p.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        double gross     = p.getBasicSalary() + p.getHra() + p.getAllowances();
        double net       = p.getNetSalary();
        String netWords  = numberToWords((long) net);

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                <title>Payslip</title>
                <style type="text/css">
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: Arial, Helvetica, sans-serif;
                        font-size: 10pt;
                        color: #2c2c2c;
                        background: #f0f4f8;
                    }
                    .page-wrapper {
                        width: 720px;
                        margin: 20px auto;
                        background: #ffffff;
                        border: 1px solid #d0d7de;
                        border-radius: 8px;
                        overflow: hidden;
                    }

                    /* ─── HEADER ─────────────────────────────── */
                    .header {
                        background-color: #0f2557;
                        padding: 22px 30px;
                        color: #ffffff;
                    }
                    .header-top {
                        display: table;
                        width: 100%;
                    }
                    .header-left  { display: table-cell; vertical-align: middle; }
                    .header-right {
                        display: table-cell;
                        vertical-align: middle;
                        text-align: right;
                    }
                    .company-logo {
                        width: 44px; height: 44px;
                        background-color: #1a73e8;
                        border-radius: 8px;
                        display: inline-block;
                        text-align: center;
                        line-height: 44px;
                        font-size: 22px;
                        font-weight: bold;
                        color: #ffffff;
                        margin-right: 12px;
                        vertical-align: middle;
                    }
                    .company-name {
                        font-size: 17pt;
                        font-weight: bold;
                        color: #ffffff;
                        vertical-align: middle;
                    }
                    .company-meta {
                        font-size: 8pt;
                        color: #a8c7fa;
                        margin-top: 4px;
                    }
                    .payslip-badge {
                        background-color: #1a73e8;
                        color: #ffffff;
                        padding: 6px 16px;
                        border-radius: 20px;
                        font-size: 9pt;
                        font-weight: bold;
                        letter-spacing: 1px;
                    }
                    .pay-period {
                        font-size: 11pt;
                        color: #a8c7fa;
                        margin-top: 6px;
                        font-weight: bold;
                    }

                    /* ─── SUBHEADER BAR ──────────────────────── */
                    .subheader {
                        background-color: #1a73e8;
                        padding: 8px 30px;
                        color: #ffffff;
                        font-size: 8.5pt;
                        display: table;
                        width: 100%;
                    }
                    .subheader-cell {
                        display: table-cell;
                        padding-right: 30px;
                    }
                    .subheader-cell span { color: #bdd7ff; }

                    /* ─── SECTION TITLE ─────────────────────── */
                    .section-title {
                        background-color: #e8f0fe;
                        color: #0f2557;
                        font-size: 8.5pt;
                        font-weight: bold;
                        letter-spacing: 1.2px;
                        padding: 7px 30px;
                        border-top: 2px solid #1a73e8;
                        border-bottom: 1px solid #c5d5f5;
                        text-transform: uppercase;
                    }

                    /* ─── EMPLOYEE INFO GRID ─────────────────── */
                    .emp-grid {
                        display: table;
                        width: 100%;
                        padding: 16px 30px;
                        border-bottom: 1px solid #e2e8f0;
                    }
                    .emp-col {
                        display: table-cell;
                        width: 33%;
                        vertical-align: top;
                        padding-right: 10px;
                    }
                    .info-label {
                        font-size: 7.5pt;
                        color: #6b7280;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 2px;
                    }
                    .info-value {
                        font-size: 9.5pt;
                        font-weight: bold;
                        color: #1e293b;
                    }
                    .info-item { margin-bottom: 12px; }

                    /* ─── SALARY TABLE ──────────────────────── */
                    .salary-wrap {
                        display: table;
                        width: 100%;
                        padding: 0 30px 20px 30px;
                    }
                    .sal-col {
                        display: table-cell;
                        width: 50%;
                        vertical-align: top;
                        padding-right: 15px;
                    }
                    .sal-col:last-child { padding-right: 0; padding-left: 15px; }

                    .sal-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 12px;
                    }
                    .sal-table thead tr {
                        background-color: #0f2557;
                        color: #ffffff;
                    }
                    .sal-table thead th {
                        padding: 8px 10px;
                        font-size: 8pt;
                        text-align: left;
                        letter-spacing: 0.5px;
                    }
                    .sal-table thead th:last-child { text-align: right; }
                    .sal-table tbody tr:nth-child(even) { background-color: #f8fafc; }
                    .sal-table tbody tr:nth-child(odd)  { background-color: #ffffff; }
                    .sal-table tbody td {
                        padding: 7px 10px;
                        font-size: 9pt;
                        border-bottom: 1px solid #edf2f7;
                    }
                    .sal-table tbody td:last-child { text-align: right; }
                    .sal-table tfoot td {
                        padding: 8px 10px;
                        font-size: 9pt;
                        font-weight: bold;
                        border-top: 2px solid #0f2557;
                    }
                    .sal-table tfoot td:last-child { text-align: right; }
                    .earn-total { background-color: #e8f5e9; color: #1b5e20; }
                    .ded-total  { background-color: #fce4ec; color: #880e4f; }
                    .earn-amt   { color: #2e7d32; font-weight: bold; }
                    .ded-amt    { color: #c62828; font-weight: bold; }

                    /* ─── NET PAY BOX ───────────────────────── */
                    .net-pay-box {
                        margin: 0 30px 20px 30px;
                        background-color: #0f2557;
                        border-radius: 8px;
                        padding: 16px 24px;
                        display: table;
                        width: calc(100% - 60px);
                    }
                    .net-left  {
                        display: table-cell;
                        vertical-align: middle;
                        color: #a8c7fa;
                        font-size: 9pt;
                    }
                    .net-left .net-label { font-size: 8pt; text-transform: uppercase; letter-spacing: 1px; }
                    .net-left .net-words { font-style: italic; margin-top: 4px; font-size: 8.5pt; color: #cfe2ff; }
                    .net-right {
                        display: table-cell;
                        vertical-align: middle;
                        text-align: right;
                    }
                    .net-amount {
                        font-size: 22pt;
                        font-weight: bold;
                        color: #ffffff;
                    }
                    .net-currency { font-size: 14pt; color: #a8c7fa; }
                    .paid-stamp {
                        display: inline-block;
                        border: 2px solid #4ade80;
                        color: #4ade80;
                        font-size: 8.5pt;
                        font-weight: bold;
                        letter-spacing: 2px;
                        padding: 2px 10px;
                        border-radius: 4px;
                        margin-top: 6px;
                    }

                    /* ─── FOOTER ────────────────────────────── */
                    .footer {
                        background-color: #f8fafc;
                        border-top: 1px solid #e2e8f0;
                        padding: 14px 30px;
                        display: table;
                        width: 100%;
                    }
                    .footer-left  { display: table-cell; vertical-align: bottom; }
                    .footer-right { display: table-cell; text-align: right; vertical-align: bottom; }
                    .footer-note  { font-size: 7.5pt; color: #6b7280; line-height: 1.6; }
                    .sig-line {
                        border-top: 1px solid #94a3b8;
                        width: 140px;
                        margin-bottom: 4px;
                        margin-left: auto;
                    }
                    .sig-label { font-size: 7.5pt; color: #6b7280; text-align: right; }
                    .cin-line { font-size: 7pt; color: #94a3b8; margin-top: 6px; }
                </style>
            </head>
            <body>
            <div class="page-wrapper">

              <!-- ── HEADER ── -->
              <div class="header">
                <div class="header-top">
                  <div class="header-left">
                    <span class="company-logo">J</span>
                    <span class="company-name">%s</span>
                    <div class="company-meta">%s &#160;|&#160; %s &#160;|&#160; %s</div>
                  </div>
                  <div class="header-right">
                    <div class="payslip-badge">SALARY SLIP</div>
                    <div class="pay-period">%s %d</div>
                  </div>
                </div>
              </div>

              <!-- ── SUBHEADER ── -->
              <div class="subheader">
                <div class="subheader-cell"><span>Employee ID: </span>%s</div>
                <div class="subheader-cell"><span>Department: </span>%s</div>
                <div class="subheader-cell"><span>Payment Date: </span>%s</div>
                <div class="subheader-cell"><span>Payment Mode: </span>Bank Transfer</div>
              </div>

              <!-- ── EMPLOYEE DETAILS ── -->
              <div class="section-title">Employee Information</div>
              <div class="emp-grid">
                <div class="emp-col">
                  <div class="info-item">
                    <div class="info-label">Full Name</div>
                    <div class="info-value">%s</div>
                  </div>
                  <div class="info-item">
                    <div class="info-label">Department</div>
                    <div class="info-value">%s</div>
                  </div>
                </div>
                <div class="emp-col">
                  <div class="info-item">
                    <div class="info-label">Pay Period</div>
                    <div class="info-value">%s %d</div>
                  </div>
                  <div class="info-item">
                    <div class="info-label">Working Days</div>
                    <div class="info-value">30 / 30</div>
                  </div>
                </div>
                <div class="emp-col">
                  <div class="info-item">
                    <div class="info-label">Payment Status</div>
                    <div class="info-value">%s</div>
                  </div>
                  <div class="info-item">
                    <div class="info-label">Payment Date</div>
                    <div class="info-value">%s</div>
                  </div>
                </div>
              </div>

              <!-- ── SALARY BREAKDOWN ── -->
              <div class="section-title">Salary Breakdown</div>
              <div class="salary-wrap">
                <!-- EARNINGS -->
                <div class="sal-col">
                  <table class="sal-table">
                    <thead>
                      <tr>
                        <th>Earnings</th>
                        <th>Amount (&#8377;)</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td>Basic Salary</td>
                        <td class="earn-amt">%s</td>
                      </tr>
                      <tr>
                        <td>House Rent Allowance (HRA)</td>
                        <td class="earn-amt">%s</td>
                      </tr>
                      <tr>
                        <td>Special Allowances</td>
                        <td class="earn-amt">%s</td>
                      </tr>
                    </tbody>
                    <tfoot>
                      <tr class="earn-total">
                        <td>Gross Earnings</td>
                        <td>%s</td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
                <!-- DEDUCTIONS -->
                <div class="sal-col">
                  <table class="sal-table">
                    <thead>
                      <tr>
                        <th>Deductions</th>
                        <th>Amount (&#8377;)</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td>Provident Fund (PF)</td>
                        <td class="ded-amt">%s</td>
                      </tr>
                      <tr>
                        <td>Professional Tax</td>
                        <td class="ded-amt">200</td>
                      </tr>
                      <tr>
                        <td>Other Deductions</td>
                        <td class="ded-amt">%s</td>
                      </tr>
                    </tbody>
                    <tfoot>
                      <tr class="ded-total">
                        <td>Total Deductions</td>
                        <td>%s</td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              </div>

              <!-- ── NET PAY ── -->
              <div class="net-pay-box">
                <div class="net-left">
                  <div class="net-label">Net Take-Home Pay</div>
                  <div class="net-words">%s Only</div>
                  <div class="paid-stamp">&#10003; PAID</div>
                </div>
                <div class="net-right">
                  <div class="net-amount"><span class="net-currency">&#8377; </span>%s</div>
                </div>
              </div>

              <!-- ── FOOTER ── -->
              <div class="footer">
                <div class="footer-left">
                  <div class="footer-note">
                    * This is a computer-generated payslip and does not require a physical signature.<br/>
                    * For queries, write to <b>%s</b><br/>
                    * Amounts are in Indian Rupees (INR).
                  </div>
                  <div class="cin-line">CIN: %s</div>
                </div>
                <div class="footer-right">
                  <div class="sig-line">&#160;</div>
                  <div class="sig-label">Authorised Signatory<br/>HR Department</div>
                </div>
              </div>

            </div>
            </body>
            </html>
            """.formatted(
                // Header
                COMPANY_NAME, COMPANY_ADDRESS, COMPANY_EMAIL, COMPANY_WEBSITE,
                monthName, p.getYear(),
                // Subheader
                p.getEmployeeId(), p.getDepartment(),
                p.getPaymentDate() != null ? p.getPaymentDate().toString() : "-",
                // Emp Info
                p.getEmployeeName(), p.getDepartment(),
                monthName, p.getYear(),
                p.getPaymentStatus().name(),
                p.getPaymentDate() != null ? p.getPaymentDate().toString() : "-",
                // Earnings
                formatAmount(p.getBasicSalary()),
                formatAmount(p.getHra()),
                formatAmount(p.getAllowances()),
                formatAmount(gross),
                // Deductions
                formatAmount(p.getDeductions() * 0.60),   // PF portion
                formatAmount(p.getDeductions() * 0.40),   // Other portion
                formatAmount(p.getDeductions()),
                // Net
                netWords, formatAmount(net),
                // Footer
                COMPANY_EMAIL, COMPANY_CIN
            );
    }

    private String formatAmount(double amount) {
        return String.format("%,.2f", amount);
    }

    // ── Simple number-to-words (INR style) ────────────────────────────────────
    private String numberToWords(long number) {
        if (number == 0) return "Zero Rupees";
        String[] ones = {"", "One","Two","Three","Four","Five","Six","Seven","Eight","Nine",
                         "Ten","Eleven","Twelve","Thirteen","Fourteen","Fifteen","Sixteen",
                         "Seventeen","Eighteen","Nineteen"};
        String[] tens = {"","","Twenty","Thirty","Forty","Fifty","Sixty","Seventy","Eighty","Ninety"};

        StringBuilder sb = new StringBuilder();
        long crore = number / 10_000_000; number %= 10_000_000;
        long lakh  = number / 100_000;    number %= 100_000;
        long thou  = number / 1_000;      number %= 1_000;
        long hund  = number / 100;        number %= 100;

        if (crore > 0) sb.append(twoDigits(crore, ones, tens)).append(" Crore ");
        if (lakh  > 0) sb.append(twoDigits(lakh,  ones, tens)).append(" Lakh ");
        if (thou  > 0) sb.append(twoDigits(thou,  ones, tens)).append(" Thousand ");
        if (hund  > 0) sb.append(ones[(int) hund]).append(" Hundred ");
        if (number > 0) sb.append(twoDigits(number, ones, tens));

        return sb.toString().trim() + " Rupees";
    }

    private String twoDigits(long n, String[] ones, String[] tens) {
        if (n < 20) return ones[(int) n];
        return tens[(int) (n / 10)] + (n % 10 != 0 ? " " + ones[(int) (n % 10)] : "");
    }
}