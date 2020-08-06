package seospy.max_jd.seo.util.serializ.impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import seospy.max_jd.seo.SeoSpy;
import seospy.max_jd.seo.entities.SeoEntity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class ExcelWriter {
    private static String[] nameColumns = {"#", "URL", "Canonical", "Response", "Title", "Description", "KeyWords", "H1", "Content-Type",
            "Meta-Robots", "Ex. links", "In links", "Out links", "Problem"};

    public static void writeToFile(Path path, SeoEntity[] arraySeoUrls) {
        System.out.println(path.toString());
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        Row headerRow = sheet.createRow(0);

        for(int i = 0; i < nameColumns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(nameColumns[i]);
        }

        int rowNum = 1;

        for(SeoEntity seoUrl : arraySeoUrls) {
            Row newRow = sheet.createRow(rowNum);
            newRow.createCell(0).setCellValue(rowNum++);
            newRow.createCell(1).setCellValue(seoUrl.getUrl());
            newRow.createCell(2).setCellValue(seoUrl.getCanonical());
            newRow.createCell(3).setCellValue(seoUrl.getResponse());
            newRow.createCell(4).setCellValue(seoUrl.getTitle());
            newRow.createCell(5).setCellValue(seoUrl.getDescription());
            newRow.createCell(6).setCellValue(seoUrl.getKeywords());
            newRow.createCell(7).setCellValue(seoUrl.getCountH1());
            newRow.createCell(8).setCellValue(seoUrl.getContentType());
            newRow.createCell(9).setCellValue(seoUrl.getMetaRobots());

            if(SeoEntity.externalLinks.get(seoUrl.getUrl()) != null) {
                if(SeoEntity.externalLinks.get(seoUrl.getUrl()).size() != 0) {
                    String columnAllExternalLinks = "";
                    for(String exLink : SeoEntity.externalLinks.get(seoUrl.getUrl())) {
                        columnAllExternalLinks += exLink + System.lineSeparator();
                    }
                    newRow.createCell(10).setCellValue(columnAllExternalLinks);
                }
            } else {
                newRow.createCell(10).setCellValue("");
            }

            //there is no need to add if-operator because if there is a URL, then some URL refers to this one

            String allLinksOn = Arrays.toString(seoUrl.statisticLinksOn.get(seoUrl.getUrl()).toArray());
            //it is maximum length defined by "Excel specifications and limits" https://support.office.com/en-us/article/excel-specifications-and-limits-1672b34d-7043-467e-8e27-269d656771c3
            //delete a right square brace ] or set the maximum length of cell contents (text) is 32,767 characters
            int sizeAllLinksOn = allLinksOn.length() > 32_767 ? 32_676 : allLinksOn.length()-1;
            //at 0 index a left square brace - delete it
            newRow.createCell(11).setCellValue(allLinksOn.substring(1, sizeAllLinksOn));

            if(seoUrl.statisticLinksOut.get(seoUrl.getUrl())!= null) {
                String allLinksOut = Arrays.toString(seoUrl.statisticLinksOut.get(seoUrl.getUrl()).toArray());
                //it is maximum length defined by "Excel specifications and limits" https://support.office.com/en-us/article/excel-specifications-and-limits-1672b34d-7043-467e-8e27-269d656771c3
                //delete right square braces ] or set the maximum length of cell contents (text) is 32,767 characters
                int lenthAllLinksOut = allLinksOut.length() > 32_767 ? 32_676 : allLinksOut.length()-1;
                newRow.createCell(12).setCellValue(allLinksOut.substring(1, lenthAllLinksOut));
            }else {
                newRow.createCell(12).setCellValue("");
            }
            newRow.createCell(13).setCellValue(seoUrl.isHaveSeoProblem());
        }

        for(int i = 0; i > nameColumns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        if(!(Files.exists(path))) {
            try {
                Files.createFile(path);
            } catch(IOException ex) {
                SeoSpy.logToFile.error(ExcelWriter.class + " " + ex.toString());
            }
        }

        try(FileOutputStream fileOut = new FileOutputStream(path.toString())) {
            workbook.write(fileOut);
        } catch(IOException ex) {
            SeoSpy.logToFile.error(ExcelWriter.class + " " + ex.toString());
        }

        try {
            workbook.close();
        } catch(IOException ex) {
            SeoSpy.logToFile.error(ExcelWriter.class + " " + ex.toString());
        }
    }
}
