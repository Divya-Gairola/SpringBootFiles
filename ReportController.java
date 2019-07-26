package com.ctol.bench.controller;

 


import java.io.IOException;

 

import javax.servlet.http.HttpServletResponse;

 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

 

import com.ctol.bench.reports.EmployeeReport;
import com.ctol.bench.repositories.ExcelRepository;
import com.ctol.bench.repositories.HeaderRepository;
import com.ctol.bench.service.ReportService;

 

import ar.com.fdvs.dj.domain.builders.ColumnBuilderException;
import net.sf.jasperreports.engine.JRException;

 

@Controller
@RequestMapping("/")
public class ReportController {

 

    private final ExcelRepository dataRepository;
    private final ReportService reportService;
    private final HeaderRepository headerRepo;

 

    @Autowired
    public ReportController(final ExcelRepository dataRepository, final ReportService reportService, final HeaderRepository headerRepo){
        this.dataRepository = dataRepository;
        this.reportService = reportService;
        this.headerRepo =headerRepo;
    }

 

    @GetMapping
    public String getHome(){
        return "redirect:/employeeReport.pdf";
    }

 

    @GetMapping(value = "/employeeReport.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @ResponseBody
    public HttpEntity<byte[]> getEmployeeReportPdf(final HttpServletResponse response) throws ColumnBuilderException, Exception {
        final EmployeeReport report = new EmployeeReport(dataRepository.findAll(), headerRepo.findAll());
        final byte[] data = reportService.getReportPdf(report.getReport());

 

        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_PDF);
        header.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=employeeReport.pdf");
        header.setContentLength(data.length);

 

        return new HttpEntity<byte[]>(data, header);
    }

 

    @GetMapping(value = "/employeeReport.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8")
    @ResponseBody
    public HttpEntity<byte[]> getEmployeeReportXlsx(final HttpServletResponse response) throws ColumnBuilderException, Exception {
        final EmployeeReport report = new EmployeeReport(dataRepository.findAll(), headerRepo.findAll());
        final byte[] data = reportService.getReportXlsx(report.getReport());

 

        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8"));
        header.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=employeeReport.xlsx");
        header.setContentLength(data.length);

 

        return new HttpEntity<byte[]>(data, header);
    }
}