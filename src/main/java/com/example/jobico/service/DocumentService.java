package com.example.jobico.service;

import com.example.jobico.dto.ExperienceLetterRequest;
import com.example.jobico.dto.OfferLetterRequest;
import com.example.jobico.entity.Candidate;
import com.example.jobico.entity.CandidateStatus;
import com.example.jobico.entity.Employee;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.repository.CandidateRepository;
import com.example.jobico.repository.EmployeeRepository;
import com.lowagie.text.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

@Service
public class DocumentService {

    @Autowired private CandidateRepository candidateRepository;
    @Autowired private EmployeeRepository  employeeRepository;
    @Autowired private EmailService        emailService;
    

    @Value("${app.company.name:Jobico}")
    private String companyName;

    private static final String COMPANY_ADDRESS = "3rd Floor, Cyber Towers, Hitech City, Hyderabad – 500081";
    private static final String COMPANY_EMAIL   = "hr@jobico.in";
    private static final String COMPANY_WEBSITE = "www.jobico.in";
    private static final String COMPANY_CIN     = "U74999TG2024PTC123456";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    
    //  OFFER LETTER   
    /**
     * Candidate must be in SELECTED status.
     */
    public byte[] generateOfferLetter(OfferLetterRequest request) {
        Candidate c = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        if (c.getStatus() != CandidateStatus.SELECTED)
            throw new RuntimeException("Offer letter can only be generated for SELECTED candidates.");

        try {
            return buildOfferLetterPdf(c, request);
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate Offer Letter PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generate Offer Letter PDF and send it to candidate's email.
     */
    public void sendOfferLetterByEmail(OfferLetterRequest request) {
        Candidate c = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        if (c.getStatus() != CandidateStatus.SELECTED)
            throw new RuntimeException("Offer letter can only be sent for SELECTED candidates.");

        String email = c.getEmail();
        if (email == null || email.isBlank())
            throw new RuntimeException("Candidate does not have an email address on file.");

        try {
            byte[] pdf = buildOfferLetterPdf(c, request);

            String filename = "OfferLetter_" + c.getFirstName() + "_" + c.getSurname() + ".pdf";
            String subject  = "Offer Letter – " + c.getRole() + " at " + companyName;
            String htmlBody = emailService.buildOfferLetterEmailBody(
                    c.getFirstName() + " " + c.getSurname(),
                    c.getRole(),
                    request.getSalary(),
                    request.getJoiningDate(),
                    companyName);

            emailService.sendEmailWithPdfAttachment(email, subject, htmlBody, pdf, filename);

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate Offer Letter PDF: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  EXPERIENCE LETTER
    // ══════════════════════════════════════════════════════════════════

    /**
     * Generate Experience Letter PDF bytes.
     */
    public byte[] generateExperienceLetter(ExperienceLetterRequest request) {
        Employee emp = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        try {
            return buildExperienceLetterPdf(emp, request.getRemarks());
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate Experience Letter PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generate Experience Letter PDF and send it to candidate's email.
     */
    public void sendExperienceLetterByEmail(ExperienceLetterRequest request) {
        Employee emp = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Candidate c   = emp.getCandidate();
        String email  = c.getEmail();
        if (email == null || email.isBlank())
            throw new RuntimeException("Employee does not have an email address on file.");

        try {
            byte[] pdf = buildExperienceLetterPdf(emp, request.getRemarks());

            String filename = "ExperienceLetter_" + c.getFirstName() + "_" + c.getSurname() + ".pdf";
            String subject  = "Experience Letter – " + companyName;
            String htmlBody = emailService.buildExperienceLetterEmailBody(
                    c.getFirstName() + " " + c.getSurname(),
                    c.getRole(),
                    emp.getDepartment(),
                    emp.getJoiningDate(),
                    companyName);

            emailService.sendEmailWithPdfAttachment(email, subject, htmlBody, pdf, filename);

        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate Experience Letter PDF: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  PDF BUILDERS
    // ══════════════════════════════════════════════════════════════════

    private byte[] buildOfferLetterPdf(Candidate c, OfferLetterRequest request) throws DocumentException {
        String today   = DATE_FMT.format(LocalDate.now());
        String joining = DATE_FMT.format(request.getJoiningDate());
        String name    = c.getFirstName() + " " + c.getSurname();
        double annualCtc  = request.getSalary();
        double monthlyCtc = annualCtc / 12;

        String html = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
              <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
              <title>Offer Letter</title>
              <style type="text/css">
                * { margin:0; padding:0; box-sizing:border-box; }
                body { font-family: Arial, Helvetica, sans-serif; font-size:10pt; color:#2c2c2c; background:#f0f4f8; }
                .page { width:720px; margin:20px auto; background:#fff; border:1px solid #d0d7de; border-radius:8px; overflow:hidden; }

                /* Header */
                .header { background:#0f2557; padding:22px 36px; }
                .header-row { display:table; width:100%; }
                .header-left { display:table-cell; vertical-align:middle; }
                .header-right { display:table-cell; vertical-align:middle; text-align:right; }
                .logo-box { width:44px; height:44px; background:#1a73e8; border-radius:8px;
                            display:inline-block; text-align:center; line-height:44px;
                            font-size:22px; font-weight:bold; color:#fff; margin-right:10px; vertical-align:middle; }
                .company-name { font-size:17pt; font-weight:bold; color:#fff; vertical-align:middle; }
                .company-meta { font-size:8pt; color:#a8c7fa; margin-top:4px; }
                .doc-badge { background:#1a73e8; color:#fff; padding:6px 16px; border-radius:20px;
                             font-size:9pt; font-weight:bold; letter-spacing:1px; }
                .doc-date { font-size:9pt; color:#a8c7fa; margin-top:6px; }

                /* Body */
                .body { padding:30px 36px; }
                .ref-line { font-size:8.5pt; color:#6b7280; margin-bottom:18px; }
                .salutation { font-size:11pt; font-weight:bold; color:#0f2557; margin-bottom:14px; }
                .para { font-size:9.5pt; color:#374151; line-height:1.7; margin-bottom:14px; }

                /* Details table */
                .section-title { font-size:9pt; font-weight:bold; color:#0f2557; letter-spacing:1px;
                                 text-transform:uppercase; background:#e8f0fe; padding:7px 12px;
                                 border-left:4px solid #1a73e8; margin:20px 0 10px 0; }
                .detail-table { width:100%; border-collapse:collapse; }
                .detail-table tr:nth-child(even) { background:#f8fafc; }
                .detail-table td { padding:8px 12px; font-size:9.5pt; border-bottom:1px solid #edf2f7; }
                .detail-table td:first-child { font-weight:bold; color:#374151; width:42%; }
                .detail-table td:last-child { color:#1e293b; }

                /* Acceptance box */
                .accept-box { background:#f0fdf4; border:1px solid #bbf7d0; border-radius:6px;
                              padding:14px 18px; margin:22px 0; }
                .accept-box p { font-size:9pt; color:#166534; line-height:1.6; }

                /* Signature */
                .sign-area { display:table; width:100%; margin-top:30px; }
                .sign-left { display:table-cell; width:50%; vertical-align:bottom; }
                .sign-right { display:table-cell; width:50%; text-align:right; vertical-align:bottom; }
                .sign-line { border-top:1px solid #94a3b8; width:160px; margin-bottom:4px; }
                .sign-line-right { border-top:1px solid #94a3b8; width:160px; margin-bottom:4px; margin-left:auto; }
                .sign-label { font-size:8pt; color:#6b7280; }

                /* Footer */
                .footer { background:#f8fafc; border-top:1px solid #e2e8f0; padding:12px 36px; }
                .footer-text { font-size:7.5pt; color:#94a3b8; text-align:center; }
                .cin { font-size:7pt; color:#cbd5e1; text-align:center; margin-top:3px; }

                .highlight { color:#1a73e8; font-weight:bold; }
                .stamp { display:inline-block; border:2px solid #1a73e8; color:#1a73e8;
                         padding:3px 12px; border-radius:4px; font-size:8.5pt;
                         font-weight:bold; letter-spacing:2px; }
              </style>
            </head>
            <body>
            <div class="page">

              <!-- HEADER -->
              <div class="header">
                <div class="header-row">
                  <div class="header-left">
                    <span class="logo-box">J</span>
                    <span class="company-name">%s</span>
                    <div class="company-meta">%s &#160;|&#160; %s &#160;|&#160; %s</div>
                  </div>
                  <div class="header-right">
                    <div class="doc-badge">OFFER LETTER</div>
                    <div class="doc-date">%s</div>
                  </div>
                </div>
              </div>

              <!-- BODY -->
              <div class="body">
                <div class="ref-line">Ref: JBC/OL/%s/%s</div>
                <div class="salutation">Dear %s,</div>

                <p class="para">
                  We are delighted to extend an offer of employment to you for the position of
                  <span class="highlight">%s</span> at <span class="highlight">%s</span>.
                  This offer is contingent upon successful completion of our pre-employment
                  verification process and your formal acceptance of the terms outlined herein.
                </p>

                <p class="para">
                  We were impressed with your background and are confident that your skills and
                  experience will be a valuable addition to our team.
                </p>

                <div class="section-title">Appointment Details</div>
                <table class="detail-table">
                  <tr><td>Designation</td><td>%s</td></tr>
                  <tr><td>Department</td><td>%s</td></tr>
                  <tr><td>Date of Joining</td><td>%s</td></tr>
                  <tr><td>CTC (Annual)</td><td>&#8377; %s</td></tr>
                  <tr><td>Monthly Take-Home (Approx.)</td><td>&#8377; %s</td></tr>
                  <tr><td>Employment Type</td><td>Full-Time, Permanent</td></tr>
                  <tr><td>Work Location</td><td>Hyderabad, Telangana</td></tr>
                  <tr><td>Probation Period</td><td>3 Months</td></tr>
                </table>

                <div class="section-title">Terms &amp; Conditions</div>
                <p class="para">
                  1. This offer is valid for <b>7 days</b> from the date of this letter.<br/>
                  2. You will be required to submit original educational and experience certificates upon joining.<br/>
                  3. The salary details mentioned are subject to applicable statutory deductions (PF, PT, TDS).<br/>
                  4. This letter does not constitute a contract of employment.
                </p>

                <div class="accept-box">
                  <p>
                    &#10003; To accept this offer, please sign and return a copy of this letter along with
                    your documents. By accepting, you confirm your joining date of <b>%s</b>.
                  </p>
                </div>

                <p class="para">
                  We look forward to welcoming you to the <b>%s</b> family. Please feel free to reach
                  out to us at <b>%s</b> should you have any questions.
                </p>

                <!-- Signature -->
                <div class="sign-area">
                  <div class="sign-left">
                    <div class="sign-line">&#160;</div>
                    <div class="sign-label">Candidate Signature &amp; Date</div>
                  </div>
                  <div class="sign-right">
                    <div class="sign-line-right">&#160;</div>
                    <div class="sign-label">Authorised Signatory<br/>HR Department, %s</div>
                  </div>
                </div>
              </div>

              <!-- FOOTER -->
              <div class="footer">
                <div class="footer-text">This is a computer-generated document. For queries write to %s</div>
                <div class="cin">CIN: %s &#160;|&#160; %s</div>
              </div>

            </div>
            </body>
            </html>
            """.formatted(
                companyName, COMPANY_ADDRESS, COMPANY_EMAIL, COMPANY_WEBSITE,
                today,
                // ref
                LocalDate.now().getYear(), c.getId(),
                // salutation
                name,
                // opening para
                c.getRole(), companyName,
                // details table
                c.getRole(),
                c.getCategory(),
                joining,
                String.format("%,.2f", annualCtc),
                String.format("%,.2f", monthlyCtc),
                // acceptance box
                joining,
                // closing
                companyName, COMPANY_EMAIL,
                // sign
                companyName,
                // footer
                COMPANY_EMAIL, COMPANY_CIN, COMPANY_WEBSITE
        );

        return renderPdf(html);
    }

    private byte[] buildExperienceLetterPdf(Employee emp, String remarks) throws DocumentException {
        Candidate c     = emp.getCandidate();
        String today    = DATE_FMT.format(LocalDate.now());
        String fromDate = DATE_FMT.format(emp.getJoiningDate());
        String name     = c.getFirstName() + " " + c.getSurname();

        // Calculate tenure
        LocalDate from   = emp.getJoiningDate();
        LocalDate to     = LocalDate.now();
        long months      = java.time.temporal.ChronoUnit.MONTHS.between(from, to);
        long years       = months / 12;
        long remMonths   = months % 12;
        String tenure    = (years > 0 ? years + " year" + (years > 1 ? "s" : "") + " " : "")
                         + (remMonths > 0 ? remMonths + " month" + (remMonths > 1 ? "s" : "") : "").trim();
        if (tenure.isBlank()) tenure = "less than a month";

        String finalRemarks = (remarks != null && !remarks.isBlank())
                ? remarks
                : "We wish " + c.getFirstName() + " the very best in all future endeavors.";

        String html = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
              <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
              <title>Experience Letter</title>
              <style type="text/css">
                * { margin:0; padding:0; box-sizing:border-box; }
                body { font-family: Arial, Helvetica, sans-serif; font-size:10pt; color:#2c2c2c; background:#f0f4f8; }
                .page { width:720px; margin:20px auto; background:#fff; border:1px solid #d0d7de; border-radius:8px; overflow:hidden; }

                .header { background:#0f2557; padding:22px 36px; }
                .header-row { display:table; width:100%; }
                .header-left { display:table-cell; vertical-align:middle; }
                .header-right { display:table-cell; vertical-align:middle; text-align:right; }
                .logo-box { width:44px; height:44px; background:#1a73e8; border-radius:8px;
                            display:inline-block; text-align:center; line-height:44px;
                            font-size:22px; font-weight:bold; color:#fff; margin-right:10px; vertical-align:middle; }
                .company-name { font-size:17pt; font-weight:bold; color:#fff; vertical-align:middle; }
                .company-meta { font-size:8pt; color:#a8c7fa; margin-top:4px; }
                .doc-badge { background:#1a73e8; color:#fff; padding:6px 16px; border-radius:20px;
                             font-size:9pt; font-weight:bold; letter-spacing:1px; }
                .doc-date { font-size:9pt; color:#a8c7fa; margin-top:6px; }

                /* Certification band */
                .cert-band { background:#1a73e8; padding:10px 36px; }
                .cert-text { font-size:9pt; color:#cfe2ff; display:table; width:100%; }
                .cert-cell { display:table-cell; padding-right:24px; }
                .cert-cell span { color:#ffffff; font-weight:bold; }

                .body { padding:30px 36px; }
                .ref-line { font-size:8.5pt; color:#6b7280; margin-bottom:18px; }
                .whom { font-size:10pt; font-weight:bold; color:#374151; margin-bottom:18px;
                        border-left:4px solid #1a73e8; padding-left:10px; }
                .para { font-size:9.5pt; color:#374151; line-height:1.8; margin-bottom:16px; }

                /* Highlight card */
                .info-card { background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px;
                             padding:18px 22px; margin:20px 0; }
                .info-grid { display:table; width:100%; }
                .info-col { display:table-cell; width:50%; vertical-align:top; padding-right:20px; }
                .info-col:last-child { padding-right:0; }
                .info-item { margin-bottom:12px; }
                .info-label { font-size:7.5pt; color:#6b7280; text-transform:uppercase; letter-spacing:0.5px; }
                .info-value { font-size:10pt; font-weight:bold; color:#1e293b; }

                .remarks-box { background:#eff6ff; border:1px solid #bfdbfe; border-radius:6px;
                               padding:14px 18px; margin:18px 0; }
                .remarks-box p { font-size:9.5pt; color:#1e40af; line-height:1.6; font-style:italic; }

                .sign-area { display:table; width:100%; margin-top:36px; }
                .sign-left { display:table-cell; width:50%; vertical-align:bottom; }
                .sign-right { display:table-cell; width:50%; text-align:right; vertical-align:bottom; }
                .sign-line { border-top:1px solid #94a3b8; width:160px; margin-bottom:4px; }
                .sign-line-right { border-top:1px solid #94a3b8; width:160px; margin-bottom:4px; margin-left:auto; }
                .sign-label { font-size:8pt; color:#6b7280; }

                .footer { background:#f8fafc; border-top:1px solid #e2e8f0; padding:12px 36px; }
                .footer-text { font-size:7.5pt; color:#94a3b8; text-align:center; }
                .cin { font-size:7pt; color:#cbd5e1; text-align:center; margin-top:3px; }

                .highlight { color:#1a73e8; font-weight:bold; }
              </style>
            </head>
            <body>
            <div class="page">

              <!-- HEADER -->
              <div class="header">
                <div class="header-row">
                  <div class="header-left">
                    <span class="logo-box">J</span>
                    <span class="company-name">%s</span>
                    <div class="company-meta">%s &#160;|&#160; %s &#160;|&#160; %s</div>
                  </div>
                  <div class="header-right">
                    <div class="doc-badge">EXPERIENCE LETTER</div>
                    <div class="doc-date">%s</div>
                  </div>
                </div>
              </div>

              <!-- CERT BAND -->
              <div class="cert-band">
                <div class="cert-text">
                  <div class="cert-cell"><span>Employee ID: </span>%s</div>
                  <div class="cert-cell"><span>Department: </span>%s</div>
                  <div class="cert-cell"><span>Total Tenure: </span>%s</div>
                </div>
              </div>

              <!-- BODY -->
              <div class="body">
                <div class="ref-line">Ref: JBC/EL/%s/%s</div>
                <div class="whom">To Whom It May Concern</div>

                <p class="para">
                  This is to certify that <span class="highlight">%s</span> was employed with
                  <span class="highlight">%s</span> as <span class="highlight">%s</span>
                  in the <span class="highlight">%s</span> department.
                  The tenure of employment was from <b>%s</b> to <b>%s</b>, a period of <b>%s</b>.
                </p>

                <p class="para">
                  During the course of employment, %s demonstrated a high level of professionalism,
                  dedication, and technical competence. The responsibilities included but were not
                  limited to performing duties associated with the role of <b>%s</b> and contributing
                  to team and organisational objectives.
                </p>

                <!-- Employee Details Card -->
                <div class="info-card">
                  <div class="info-grid">
                    <div class="info-col">
                      <div class="info-item">
                        <div class="info-label">Full Name</div>
                        <div class="info-value">%s</div>
                      </div>
                      <div class="info-item">
                        <div class="info-label">Employee ID</div>
                        <div class="info-value">%s</div>
                      </div>
                      <div class="info-item">
                        <div class="info-label">Designation</div>
                        <div class="info-value">%s</div>
                      </div>
                    </div>
                    <div class="info-col">
                      <div class="info-item">
                        <div class="info-label">Department</div>
                        <div class="info-value">%s</div>
                      </div>
                      <div class="info-item">
                        <div class="info-label">Date of Joining</div>
                        <div class="info-value">%s</div>
                      </div>
                      <div class="info-item">
                        <div class="info-label">Last Working Day</div>
                        <div class="info-value">%s</div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Remarks -->
                <div class="remarks-box">
                  <p>&#8220; %s &#8221;</p>
                </div>

                <p class="para">
                  We wish %s the very best in all future professional endeavors and hope that the
                  experience gained at <b>%s</b> will be of great benefit.
                </p>

                <!-- Signature -->
                <div class="sign-area">
                  <div class="sign-left">
                    <div class="sign-label" style="margin-bottom:30px;">Issued on: %s</div>
                    <div class="sign-line">&#160;</div>
                    <div class="sign-label">Employee Acknowledgement (Optional)</div>
                  </div>
                  <div class="sign-right">
                    <div class="sign-line-right">&#160;</div>
                    <div class="sign-label">Authorised Signatory<br/>HR Department, %s</div>
                  </div>
                </div>
              </div>

              <!-- FOOTER -->
              <div class="footer">
                <div class="footer-text">This is a computer-generated document. For verification write to %s</div>
                <div class="cin">CIN: %s &#160;|&#160; %s</div>
              </div>

            </div>
            </body>
            </html>
            """.formatted(
                // Header
                companyName, COMPANY_ADDRESS, COMPANY_EMAIL, COMPANY_WEBSITE,
                today,
                // Cert band
                emp.getEmployeeId(), emp.getDepartment(), tenure,
                // Ref
                LocalDate.now().getYear(), emp.getId(),
                // Para 1
                name, companyName, c.getRole(), emp.getDepartment(), fromDate, today, tenure,
                // Para 2
                c.getFirstName(), c.getRole(),
                // Info card
                name, emp.getEmployeeId(), c.getRole(),
                emp.getDepartment(), fromDate, today,
                // Remarks
                finalRemarks,
                // Closing
                c.getFirstName(), companyName,
                // Footer sign
                today, companyName,
                COMPANY_EMAIL, COMPANY_CIN, COMPANY_WEBSITE
        );

        return renderPdf(html);
    }

    // ── PDF render helper 
    private byte[] renderPdf(String html) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(baos);
        return baos.toByteArray();
    }
}