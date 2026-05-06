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

    private static final String COMPANY_NAME = "Jobico Technologies Pvt. Ltd.";
    private static final String COMPANY_ADDRESS =
            "3rd Floor, Cyber Towers, Hitech City, Hyderabad – 500081";
    private static final String COMPANY_EMAIL = "hr@jobico.in";
    private static final String COMPANY_WEBSITE = "www.jobico.in";
    private static final String COMPANY_CIN = "U74999TG2024PTC123456";

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
    	String monthName =
    	        (p.getMonth() >= 1 && p.getMonth() <= 12)
    	                ? Month.of(p.getMonth())
    	                .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    	                : "N/A";

    	double basic = p.getBasicSalary();

    	double hra = p.getHra();

    	double allowances = p.getAllowances();

    	double deductions = p.getDeductions();

    	double gross = basic + hra + allowances;

    	double net = p.getNetSalary();

    	double pf = deductions * 0.60;

    	double otherDed = deductions * 0.40;

    	double totalDed = pf + 200 + otherDed;

    	String netWords = numberToWords((long) net);
    	

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                <title>Payslip</title>

                <style type="text/css">

                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }

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

                    .header {
                        background-color: #0f2557;
                        padding: 22px 30px;
                        color: #ffffff;
                    }

                    .header-top {
                        display: table;
                        width: 100%;
                    }

                    .header-left {
                        display: table-cell;
                        vertical-align: middle;
                    }

                    .header-right {
                        display: table-cell;
                        vertical-align: middle;
                        text-align: right;
                    }

                    .company-logo {
                        width: 44px;
                        height: 44px;
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

                    .subheader-cell span {
                        color: #bdd7ff;
                    }

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

                    .info-item {
                        margin-bottom: 12px;
                    }

                </style>

            </head>

            <body>

            <div class="page-wrapper">

                <div class="header">

                    <div class="header-top">

                        <div class="header-left">

                            <span class="company-logo">J</span>

                            <span class="company-name">%s</span>

                            <div class="company-meta">
                                %s &#160;|&#160; %s &#160;|&#160; %s
                            </div>

                        </div>

                        <div class="header-right">

                            <div class="payslip-badge">SALARY SLIP</div>

                            <div class="pay-period">%s %d</div>

                        </div>

                    </div>

                </div>

            </div>

            </body>
            </html>
            """.formatted(

                COMPANY_NAME,
                COMPANY_ADDRESS,
                COMPANY_EMAIL,
                COMPANY_WEBSITE,
                monthName,
                p.getYear()
        );
    }

    private String formatAmount(double amount) {
        return String.format("%,.2f", amount);
    }

    private String numberToWords(long number) {

        if (number == 0) {
            return "Zero Rupees";
        }

        String[] ones = {
                "",
                "One",
                "Two",
                "Three",
                "Four",
                "Five",
                "Six",
                "Seven",
                "Eight",
                "Nine",
                "Ten",
                "Eleven",
                "Twelve",
                "Thirteen",
                "Fourteen",
                "Fifteen",
                "Sixteen",
                "Seventeen",
                "Eighteen",
                "Nineteen"
        };

        String[] tens = {
                "",
                "",
                "Twenty",
                "Thirty",
                "Forty",
                "Fifty",
                "Sixty",
                "Seventy",
                "Eighty",
                "Ninety"
        };

        StringBuilder sb = new StringBuilder();

        long crore = number / 10000000;
        number %= 10000000;

        long lakh = number / 100000;
        number %= 100000;

        long thousand = number / 1000;
        number %= 1000;

        long hundred = number / 100;
        number %= 100;

        if (crore > 0) {
            sb.append(twoDigits(crore, ones, tens))
                    .append(" Crore ");
        }

        if (lakh > 0) {
            sb.append(twoDigits(lakh, ones, tens))
                    .append(" Lakh ");
        }

        if (thousand > 0) {
            sb.append(twoDigits(thousand, ones, tens))
                    .append(" Thousand ");
        }

        if (hundred > 0) {
            sb.append(ones[(int) hundred])
                    .append(" Hundred ");
        }

        if (number > 0) {
            sb.append(twoDigits(number, ones, tens));
        }

        return sb.toString().trim() + " Rupees";
    }

    private String twoDigits(long n, String[] ones, String[] tens) {

        if (n < 20) {
            return ones[(int) n];
        }

        return tens[(int) (n / 10)]
                + (n % 10 != 0
                ? " " + ones[(int) (n % 10)]
                : "");
    }
}