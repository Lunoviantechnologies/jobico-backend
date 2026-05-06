package com.example.jobico.service;

import com.example.jobico.dto.PayrollRequest;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Supports two Excel formats:
 *
 * FORMAT A — month/year IN Excel (old format):
 * A: employeeId | B: month | C: year | D: basicSalary | E: hra | F: allowances | G: deductions
 *
 * FORMAT B — month/year from request params (new format):
 * A: employeeId | B: basicSalary | C: hra | D: allowances | E: deductions
 * Call parsePayrollExcel(file, month, year) for this format.
 */
@Service
public class ExcelParsingService {

    /**
     * FORMAT A — month and year must be present in Excel columns B and C.
     * Used by old /upload endpoint if called without params.
     */
    public List<PayrollRequest> parsePayrollExcel(MultipartFile file) throws IOException {
        return parsePayrollExcel(file, 0, 0);
    }

    /**
     * FORMAT B — month and year injected from request params.
     * If month=0 or year=0, falls back to reading from Excel columns B and C.
     *
     * Excel columns for FORMAT B:
     * A: employeeId | B: basicSalary | C: hra | D: allowances | E: deductions
     *
     * Excel columns for FORMAT A (fallback):
     * A: employeeId | B: month | C: year | D: basicSalary | E: hra | F: allowances | G: deductions
     */
    public List<PayrollRequest> parsePayrollExcel(MultipartFile file, int month, int year) throws IOException {
        List<PayrollRequest> requests = new ArrayList<>();

        boolean injectMonthYear = (month > 0 && year > 0);

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row, injectMonthYear ? 5 : 7)) continue;

                try {
                    PayrollRequest req = new PayrollRequest();
                    Cell idCell = row.getCell(0);
                    if (idCell == null) continue;
                    String empIdStr = idCell.getCellType() == CellType.STRING
                            ? idCell.getStringCellValue().trim()
                            : String.valueOf((long) idCell.getNumericCellValue());
                    req.setEmployeeIdStr(empIdStr);
                    if (injectMonthYear) {
                        // FORMAT B — month/year from params, no month/year columns in Excel
                        req.setMonth(month);
                        req.setYear(year);
                        req.setBasicSalary(getNumericValue(row, 1));
                        req.setHra(getNumericValue(row, 2));
                        req.setAllowances(getNumericValue(row, 3));
                        req.setDeductions(getNumericValue(row, 4));
                    } else {
                        // FORMAT A — month/year read from Excel columns B and C
                        req.setMonth((int) getNumericValue(row, 1));
                        req.setYear((int) getNumericValue(row, 2));
                        req.setBasicSalary(getNumericValue(row, 3));
                        req.setHra(getNumericValue(row, 4));
                        req.setAllowances(getNumericValue(row, 5));
                        req.setDeductions(getNumericValue(row, 6));
                    }

                    requests.add(req);

                } catch (Exception e) {
                    throw new RuntimeException("Error parsing row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        if (requests.isEmpty()) {
            throw new RuntimeException(injectMonthYear
                    ? "Excel file has no data rows. Expected columns: employeeId, basicSalary, hra, allowances, deductions"
                    : "Excel file has no data rows. Expected columns: employeeId, month, year, basicSalary, hra, allowances, deductions");
        }

        return requests;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private double getNumericValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return 0.0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING  -> {
                try { yield Double.parseDouble(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield 0.0; }
            }
            case FORMULA -> cell.getNumericCellValue();
            default      -> 0.0;
        };
    }

    private boolean isRowEmpty(Row row, int columnCount) {
        for (int c = 0; c < columnCount; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}