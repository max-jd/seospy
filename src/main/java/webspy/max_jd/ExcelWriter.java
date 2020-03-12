package webspy.max_jd;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import java.io.IOException;
import java.io.FileOutputStream;

public class ExcelWriter {
    private static String[] nameColumns = {"#", "URL", "Canonical", "Response", "Title", "Description", "KeyWords", "H1", "Content-Type",
            "Meta-Robots", "Ex. links", "In links", "Out links", "Problem"};

    public static void writeToFile(Path path, SeoUrl[] arraySeoUrls){
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        Row headerRow = sheet.createRow(0);

        for(int i = 0; i < nameColumns.length; i++){
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(nameColumns[i]);
        }

        int rowNum = 1;

        for(SeoUrl seoUrl : arraySeoUrls){
            Row newRow = sheet.createRow(rowNum);
            newRow.createCell(0).setCellValue(rowNum++);
            newRow.createCell(1).setCellValue(seoUrl.getURL());
            newRow.createCell(2).setCellValue(seoUrl.getCanonical());
            newRow.createCell(3).setCellValue(seoUrl.getResponse());
            newRow.createCell(4).setCellValue(seoUrl.getTitle());
            newRow.createCell(5).setCellValue(seoUrl.getDescription());
            newRow.createCell(6).setCellValue(seoUrl.getKeywords());
            newRow.createCell(7).setCellValue(seoUrl.getCountH1());
            newRow.createCell(8).setCellValue(seoUrl.getContentType());
            newRow.createCell(9).setCellValue(seoUrl.getMetarobots());

            if(SeoUrl.externalLinks.get(seoUrl.getURL()) != null){
                if(SeoUrl.externalLinks.get(seoUrl.getURL()).size() != 0){
                    String columnAllExternalLinks = "";
                    for(String exLink : SeoUrl.externalLinks.get(seoUrl.getURL())) {
                        columnAllExternalLinks += exLink + System.lineSeparator();
                    }
                    newRow.createCell(10).setCellValue(columnAllExternalLinks);
                }
            }else
                newRow.createCell(10).setCellValue("");

            String allLinksOn = Arrays.toString(seoUrl.statisticLinksOn.get(seoUrl.getURL()).toArray());
            //delete square braces []
            newRow.createCell(11).setCellValue(allLinksOn.substring(1, allLinksOn.length()-1));

            if(seoUrl.statisticLinksOut.get(seoUrl.getURL())!= null) {
                String allLinksOut = Arrays.toString(seoUrl.statisticLinksOut.get(seoUrl.getURL()).toArray());
                //delete square braces []
                newRow.createCell(12).setCellValue(allLinksOut.substring(1, allLinksOut.length() - 1));
            }else {
                newRow.createCell(12).setCellValue("");
            }
            newRow.createCell(13).setCellValue(seoUrl.getFlagSeoProblem());
        }

        for(int i = 0; i > nameColumns.length; i++){
            sheet.autoSizeColumn(i);
        }

        if(!(Files.exists(path))) {
            try{
                Files.createFile(path);
            }catch(IOException ex){
                System.out.println(ex.toString());
                WebSpy.logToFile.error(ex.toString());
            }
        }

        try(FileOutputStream fileOut = new FileOutputStream(path.toString())){
            workbook.write(fileOut);
        }catch(IOException ex) {
            System.out.println(ex.toString());
            WebSpy.logToFile.error(ex.toString());
        }

        try{
            workbook.close();
        }catch(IOException ex) {
            System.out.println(ex);
            WebSpy.logToFile.error(ex);
        }
    }
}
