package com.example.jobico.service;

import com.example.jobico.dto.PayrollRequest;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Expected Excel columns (row 1 = header, data from row 2):
 * A: employeeId | B: month | C: year | D: basicSalary | E: hra | F: allowances | G: deductions
 */
@Service
public class ExcelParsingService {

    public List<PayrollRequest> parsePayrollExcel(MultipartFile file) throws IOException {
        List<PayrollRequest> requests = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    PayrollRequest req = new PayrollRequest();
                    req.setEmployeeId((long) getNumericValue(row, 0));
                    req.setMonth((int) getNumericValue(row, 1));
                    req.setYear((int) getNumericValue(row, 2));
                    req.setBasicSalary(getNumericValue(row, 3));
                    req.setHra(getNumericValue(row, 4));
                    req.setAllowances(getNumericValue(row, 5));
                    req.setDeductions(getNumericValue(row, 6));
                    requests.add(req);
                } catch (Exception e) {
                    throw new RuntimeException("Error parsing row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        if (requests.isEmpty()) {
            throw new RuntimeException("Excel file has no data rows. Check format: employeeId, month, year, basicSalary, hra, allowances, deductions");
        }

        return requests;
    }

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
            default -> 0.0;
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int c = 0; c < 7; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}