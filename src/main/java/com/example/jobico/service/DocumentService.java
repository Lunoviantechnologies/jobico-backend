package com.example.jobico.service;

import com.example.jobico.dto.ExperienceLetterRequest;
import com.example.jobico.dto.ExperienceLetterResponse;
import com.example.jobico.dto.OfferLetterRequest;
import com.example.jobico.dto.OfferLetterResponse;
import com.example.jobico.entity.*;
import com.example.jobico.exception.ResourceNotFoundException;
import com.example.jobico.repository.CandidateRepository;
import com.example.jobico.repository.EmployeeRepository;
import com.example.jobico.repository.ExperienceLetterRepository;
import com.example.jobico.repository.OfferLetterRepository;
import com.lowagie.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    @Autowired private CandidateRepository         candidateRepository;
    @Autowired private EmployeeRepository           employeeRepository;
    @Autowired private OfferLetterRepository        offerLetterRepository;
    @Autowired private ExperienceLetterRepository   experienceLetterRepository;
    @Autowired private EmailService                 emailService;
    @Autowired private StorageService               storageService;

    @Value("${app.company.name:Jobico}")
    private String companyName;

    private static final String COMPANY_ADDRESS = "3rd Floor, Cyber Towers, Hitech City, Hyderabad – 500081";
    private static final String COMPANY_EMAIL   = "hr@jobico.in";
    private static final String COMPANY_WEBSITE = "www.jobico.in";
    private static final String COMPANY_CIN     = "U74999TG2024PTC123456";

    private static final String OFFER_FOLDER      = "offer-letters";
    private static final String EXPERIENCE_FOLDER = "experience-letters";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    // ══════════════════════════════════════════════════════════════════════════
    //  OFFER LETTER
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Generate PDF → store on disk → persist DB record.
     * Returns metadata (no PDF bytes in response).
     */
    @Transactional
    public OfferLetterResponse generateAndSaveOfferLetter(OfferLetterRequest request) {
        Candidate c = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + request.getCandidateId()));

        if (c.getStatus() != CandidateStatus.SELECTED)
            throw new IllegalStateException("Offer letter can only be generated for SELECTED candidates.");

        try {
            byte[] pdf      = buildOfferLetterPdf(c, request);
            String filename = "OfferLetter_" + safe(c.getFirstName()) + "_" + safe(c.getSurname())
                            + "_" + uuid() + ".pdf";
            String pdfUrl   = storageService.store(pdf, OFFER_FOLDER, filename);

            OfferLetter ol  = new OfferLetter();
            ol.setCandidate(c);
            ol.setSalary(request.getSalary());
            ol.setJoiningDate(request.getJoiningDate());
            ol.setPdfUrl(pdfUrl);
            ol.setReferenceNumber(offerRef(c.getId()));
            ol.setGeneratedBy(currentUser());
            offerLetterRepository.save(ol);

            log.info("Offer letter generated: ref={} candidate={}", ol.getReferenceNumber(), c.getId());
            return OfferLetterResponse.from(ol);

        } catch (DocumentException e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Load the PDF bytes from disk and return them for streaming.
     * Throws 404 if the record or file is missing.
     */
    @Transactional(readOnly = true)
    public byte[] downloadOfferLetter(Long offerLetterId) {
        OfferLetter ol = offerLetterRepository.findById(offerLetterId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer letter not found: " + offerLetterId));
        return storageService.load(ol.getPdfUrl());
    }

    /**
     * Email the stored PDF to the candidate. Marks emailSent = true.
     * Safe to call multiple times (admin can re-send).
     */
    @Transactional
    public void sendOfferLetterByEmail(Long offerLetterId) {
        OfferLetter ol = offerLetterRepository.findById(offerLetterId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer letter not found: " + offerLetterId));

        Candidate c  = ol.getCandidate();
        String email = c.getEmail();
        if (email == null || email.isBlank())
            throw new IllegalStateException("Candidate has no email address on file.");

        byte[] pdf      = storageService.load(ol.getPdfUrl());
        String filename = "OfferLetter_" + c.getFirstName() + "_" + c.getSurname() + ".pdf";
        String subject  = "Offer Letter – " + c.getRole() + " at " + companyName;
        String body     = emailService.buildOfferLetterEmailBody(
                c.getFirstName() + " " + c.getSurname(), c.getRole(),
                ol.getSalary(), ol.getJoiningDate(), companyName);

        emailService.sendEmailWithPdfAttachment(email, subject, body, pdf, filename);

        ol.setEmailSent(true);
        ol.setEmailSentAt(LocalDateTime.now());
        offerLetterRepository.save(ol);

        log.info("Offer letter emailed: ref={} to={}", ol.getReferenceNumber(), email);
    }

    /** One-shot convenience: generate + save + email. */
    @Transactional
    public OfferLetterResponse generateSaveAndSendOfferLetter(OfferLetterRequest request) {
        OfferLetterResponse saved = generateAndSaveOfferLetter(request);
        sendOfferLetterByEmail(saved.getId());
        return saved;
    }

    /** Paginated list for admin dashboard. */
    @Transactional(readOnly = true)
    public Page<OfferLetterResponse> listOfferLetters(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String q = (search == null || search.isBlank()) ? null : search.trim();
        return offerLetterRepository.search(q, pageable).map(OfferLetterResponse::from);
    }

    /** All offer letters for a single candidate (history view). */
    @Transactional(readOnly = true)
    public List<OfferLetterResponse> getOfferLettersForCandidate(Long candidateId) {
        return offerLetterRepository.findByCandidateIdOrderByGeneratedAtDesc(candidateId)
                .stream().map(OfferLetterResponse::from).toList();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EXPERIENCE LETTER
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public ExperienceLetterResponse generateAndSaveExperienceLetter(ExperienceLetterRequest request) {
        Employee emp = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));

        try {
            byte[] pdf      = buildExperienceLetterPdf(emp, request.getRemarks());
            Candidate c     = emp.getCandidate();
            String filename = "ExperienceLetter_" + safe(c.getFirstName()) + "_" + safe(c.getSurname())
                            + "_" + uuid() + ".pdf";
            String pdfUrl   = storageService.store(pdf, EXPERIENCE_FOLDER, filename);

            ExperienceLetter el = new ExperienceLetter();
            el.setEmployee(emp);
            el.setRemarks(request.getRemarks());
            el.setPdfUrl(pdfUrl);
            el.setReferenceNumber(experienceRef(emp.getId()));
            el.setLastWorkingDay(LocalDate.now());
            el.setGeneratedBy(currentUser());
            experienceLetterRepository.save(el);

            log.info("Experience letter generated: ref={} employee={}", el.getReferenceNumber(), emp.getId());
            return ExperienceLetterResponse.from(el);

        } catch (DocumentException e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] downloadExperienceLetter(Long experienceLetterId) {
        ExperienceLetter el = experienceLetterRepository.findById(experienceLetterId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience letter not found: " + experienceLetterId));
        return storageService.load(el.getPdfUrl());
    }

    /**
     * FIX: New method used by the employee self-service endpoint.
     *
     * Loads the most recently admin-generated experience letter for the given
     * employee and returns the raw PDF bytes ready to stream back.
     *
     * Throws ResourceNotFoundException (→ 404) when no letter has been
     * generated yet — the employee should contact HR.
     */
    @Transactional(readOnly = true)
    public byte[] downloadLatestExperienceLetterForEmployee(Long employeeId) {
        ExperienceLetter el = experienceLetterRepository
                .findTopByEmployeeIdOrderByGeneratedAtDesc(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No experience letter found for employee: " + employeeId
                        + ". Please contact HR."));
        return storageService.load(el.getPdfUrl());
    }

    @Transactional
    public void sendExperienceLetterByEmail(Long experienceLetterId) {
        ExperienceLetter el = experienceLetterRepository.findById(experienceLetterId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience letter not found: " + experienceLetterId));

        Candidate c  = el.getEmployee().getCandidate();
        String email = c.getEmail();
        if (email == null || email.isBlank())
            throw new IllegalStateException("Employee has no email address on file.");

        byte[] pdf      = storageService.load(el.getPdfUrl());
        String filename = "ExperienceLetter_" + c.getFirstName() + "_" + c.getSurname() + ".pdf";
        String subject  = "Experience Letter – " + companyName;
        String body     = emailService.buildExperienceLetterEmailBody(
                c.getFirstName() + " " + c.getSurname(), c.getRole(),
                el.getEmployee().getDepartment(), el.getEmployee().getJoiningDate(), companyName);

        emailService.sendEmailWithPdfAttachment(email, subject, body, pdf, filename);

        el.setEmailSent(true);
        el.setEmailSentAt(LocalDateTime.now());
        experienceLetterRepository.save(el);

        log.info("Experience letter emailed: ref={} to={}", el.getReferenceNumber(), email);
    }

    @Transactional
    public ExperienceLetterResponse generateSaveAndSendExperienceLetter(ExperienceLetterRequest request) {
        ExperienceLetterResponse saved = generateAndSaveExperienceLetter(request);
        sendExperienceLetterByEmail(saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<ExperienceLetterResponse> listExperienceLetters(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String q = (search == null || search.isBlank()) ? null : search.trim();
        return experienceLetterRepository.search(q, pageable).map(ExperienceLetterResponse::from);
    }

    @Transactional(readOnly = true)
    public List<ExperienceLetterResponse> getExperienceLettersForEmployee(Long employeeId) {
        return experienceLetterRepository.findByEmployeeIdOrderByGeneratedAtDesc(employeeId)
                .stream().map(ExperienceLetterResponse::from).toList();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PDF BUILDERS
    // ══════════════════════════════════════════════════════════════════════════

    private byte[] buildOfferLetterPdf(Candidate c, OfferLetterRequest request) throws DocumentException {
        String today      = DATE_FMT.format(LocalDate.now());
        String joining    = DATE_FMT.format(request.getJoiningDate());
        String name       = c.getFirstName() + " " + c.getSurname();
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
                body { font-family:Arial,Helvetica,sans-serif; font-size:10pt; color:#2c2c2c; background:#f0f4f8; }
                .page { width:720px; margin:20px auto; background:#fff; border:1px solid #d0d7de; border-radius:8px; overflow:hidden; }
                .header { background:#0f2557; padding:22px 36px; }
                .header-row { display:table; width:100%%; }
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
                .body { padding:30px 36px; }
                .ref-line { font-size:8.5pt; color:#6b7280; margin-bottom:18px; }
                .salutation { font-size:11pt; font-weight:bold; color:#0f2557; margin-bottom:14px; }
                .para { font-size:9.5pt; color:#374151; line-height:1.7; margin-bottom:14px; }
                .section-title { font-size:9pt; font-weight:bold; color:#0f2557; letter-spacing:1px;
                                 text-transform:uppercase; background:#e8f0fe; padding:7px 12px;
                                 border-left:4px solid #1a73e8; margin:20px 0 10px 0; }
                .detail-table { width:100%%; border-collapse:collapse; }
                .detail-table tr:nth-child(even) { background:#f8fafc; }
                .detail-table td { padding:8px 12px; font-size:9.5pt; border-bottom:1px solid #edf2f7; }
                .detail-table td:first-child { font-weight:bold; color:#374151; width:42%%; }
                .accept-box { background:#f0fdf4; border:1px solid #bbf7d0; border-radius:6px;
                              padding:14px 18px; margin:22px 0; }
                .accept-box p { font-size:9pt; color:#166534; line-height:1.6; }
                .sign-area { display:table; width:100%%; margin-top:30px; }
                .sign-left { display:table-cell; width:50%%; vertical-align:bottom; }
                .sign-right { display:table-cell; width:50%%; text-align:right; vertical-align:bottom; }
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
              <div class="body">
                <div class="ref-line">Ref: %s</div>
                <div class="salutation">Dear %s,</div>
                <p class="para">
                  We are delighted to extend an offer of employment for the position of
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
                  <p>&#10003; To accept this offer, please sign and return a copy of this letter. By accepting,
                  you confirm your joining date of <b>%s</b>.</p>
                </div>
                <p class="para">
                  We look forward to welcoming you to the <b>%s</b> family. Please reach out to us at
                  <b>%s</b> for any questions.
                </p>
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
              <div class="footer">
                <div class="footer-text">Computer-generated document. Queries: %s</div>
                <div class="cin">CIN: %s &#160;|&#160; %s</div>
              </div>
            </div>
            </body></html>
            """.formatted(
                companyName, COMPANY_ADDRESS, COMPANY_EMAIL, COMPANY_WEBSITE, today,
                offerRef(c.getId()), name,
                c.getRole(), companyName,
                c.getRole(), c.getCategory(), joining,
                String.format("%,.2f", annualCtc), String.format("%,.2f", monthlyCtc),
                joining, companyName, COMPANY_EMAIL, companyName,
                COMPANY_EMAIL, COMPANY_CIN, COMPANY_WEBSITE
        );
        return renderPdf(html);
    }

    private byte[] buildExperienceLetterPdf(Employee emp, String remarks) throws DocumentException {
        Candidate c     = emp.getCandidate();
        String today    = DATE_FMT.format(LocalDate.now());
        String fromDate = DATE_FMT.format(emp.getJoiningDate());
        String name     = c.getFirstName() + " " + c.getSurname();

        LocalDate from = emp.getJoiningDate();
        LocalDate to   = LocalDate.now();
        long months    = java.time.temporal.ChronoUnit.MONTHS.between(from, to);
        long years     = months / 12;
        long remMonths = months % 12;
        String tenure  = (years > 0 ? years + " year" + (years > 1 ? "s" : "") + " " : "")
                       + (remMonths > 0 ? remMonths + " month" + (remMonths > 1 ? "s" : "") : "").trim();
        if (tenure.isBlank()) tenure = "less than a month";

        String finalRemarks = (remarks != null && !remarks.isBlank())
                ? remarks : "We wish " + c.getFirstName() + " the very best in all future endeavors.";

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
                body { font-family:Arial,Helvetica,sans-serif; font-size:10pt; color:#2c2c2c; }
                .page { width:720px; margin:20px auto; background:#fff; border:1px solid #d0d7de; border-radius:8px; overflow:hidden; }
                .header { background:#0f2557; padding:22px 36px; }
                .header-row { display:table; width:100%%; }
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
                .cert-band { background:#1a73e8; padding:10px 36px; }
                .cert-text { font-size:9pt; color:#cfe2ff; display:table; width:100%%; }
                .cert-cell { display:table-cell; padding-right:24px; }
                .cert-cell span { color:#fff; font-weight:bold; }
                .body { padding:30px 36px; }
                .ref-line { font-size:8.5pt; color:#6b7280; margin-bottom:18px; }
                .whom { font-size:10pt; font-weight:bold; color:#374151; margin-bottom:18px;
                        border-left:4px solid #1a73e8; padding-left:10px; }
                .para { font-size:9.5pt; color:#374151; line-height:1.8; margin-bottom:16px; }
                .info-card { background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px;
                             padding:18px 22px; margin:20px 0; }
                .info-grid { display:table; width:100%%; }
                .info-col { display:table-cell; width:50%%; vertical-align:top; padding-right:20px; }
                .info-item { margin-bottom:12px; }
                .info-label { font-size:7.5pt; color:#6b7280; text-transform:uppercase; letter-spacing:0.5px; }
                .info-value { font-size:10pt; font-weight:bold; color:#1e293b; }
                .remarks-box { background:#eff6ff; border:1px solid #bfdbfe; border-radius:6px;
                               padding:14px 18px; margin:18px 0; }
                .remarks-box p { font-size:9.5pt; color:#1e40af; line-height:1.6; font-style:italic; }
                .sign-area { display:table; width:100%%; margin-top:36px; }
                .sign-left { display:table-cell; width:50%%; vertical-align:bottom; }
                .sign-right { display:table-cell; width:50%%; text-align:right; vertical-align:bottom; }
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
              <div class="cert-band">
                <div class="cert-text">
                  <div class="cert-cell"><span>Employee ID: </span>%s</div>
                  <div class="cert-cell"><span>Department: </span>%s</div>
                  <div class="cert-cell"><span>Total Tenure: </span>%s</div>
                </div>
              </div>
              <div class="body">
                <div class="ref-line">Ref: %s</div>
                <div class="whom">To Whom It May Concern</div>
                <p class="para">
                  This is to certify that <span class="highlight">%s</span> was employed with
                  <span class="highlight">%s</span> as <span class="highlight">%s</span>
                  in the <span class="highlight">%s</span> department.
                  Tenure: <b>%s</b> to <b>%s</b> — a period of <b>%s</b>.
                </p>
                <p class="para">
                  During employment, %s demonstrated high professionalism, dedication and
                  technical competence in the role of <b>%s</b>.
                </p>
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
                <div class="remarks-box"><p>&#8220; %s &#8221;</p></div>
                <p class="para">We wish %s the very best in all future professional endeavors.</p>
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
              <div class="footer">
                <div class="footer-text">Computer-generated document. Verification: %s</div>
                <div class="cin">CIN: %s &#160;|&#160; %s</div>
              </div>
            </div>
            </body></html>
            """.formatted(
                companyName, COMPANY_ADDRESS, COMPANY_EMAIL, COMPANY_WEBSITE, today,
                emp.getEmployeeId(), emp.getDepartment(), tenure,
                experienceRef(emp.getId()),
                name, companyName, c.getRole(), emp.getDepartment(), fromDate, today, tenure,
                c.getFirstName(), c.getRole(),
                name, emp.getEmployeeId(), c.getRole(),
                emp.getDepartment(), fromDate, today,
                finalRemarks,
                c.getFirstName(),
                today, companyName,
                COMPANY_EMAIL, COMPANY_CIN, COMPANY_WEBSITE
        );
        return renderPdf(html);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private byte[] renderPdf(String html) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(baos);
        return baos.toByteArray();
    }

    private String offerRef(Long candidateId) {
        return "JBC/OL/" + LocalDate.now().getYear() + "/" + candidateId;
    }

    private String experienceRef(Long employeeId) {
        return "JBC/EL/" + LocalDate.now().getYear() + "/" + employeeId;
    }

    private static String uuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String safe(String s) {
        return s == null ? "unknown" : s.replaceAll("[^a-zA-Z0-9]", "");
    }

    private String currentUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }
}