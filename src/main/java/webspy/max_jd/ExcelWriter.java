package webspy.max_jd;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.Arrays;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import webspy.max_jd.ExcelWriter;

public class ExcelWriter {
    private static String[] columns = {"#", "URL", "Canonical", "Response", "Title", "Description", "KeyWords", "H1", "Content-Type",
            "Meta-Robots", "Ex. links", "In links", "Out links", "Problem"};

    public static void writeToFile(java.nio.file.Path path, SeoUrl[] arraySeoUrls){

        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet();

        Row headerRow = sheet.createRow(0);

        for(int i = 0; i < columns.length; i++){
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
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
                String columnExternalLinks = "";
                for(String exLink : SeoUrl.externalLinks.get(seoUrl.getURL()))
                    columnExternalLinks += exLink + System.lineSeparator();
                newRow.createCell(10).setCellValue(columnExternalLinks);
                }
            }else
                newRow.createCell(10).setCellValue("");

            String tempStr1 = Arrays.toString(seoUrl.statisticLinksOn.get(seoUrl.getURL()).toArray());
            //  newRow.createCell(11).setCellValue(tempStr1);
            newRow.createCell(11).setCellValue(tempStr1.substring(1, tempStr1.length()-1)); //delete square braces []

            if(seoUrl.statisticLinksOut.get(seoUrl.getURL())!= null) {
                String tempStr2 = Arrays.toString(seoUrl.statisticLinksOut.get(seoUrl.getURL()).toArray());
                // newRow.createCell(12).setCellValue(tempStr2);
                newRow.createCell(12).setCellValue(tempStr2.substring(1, tempStr2.length() - 1)); //delete square braces []
            }else newRow.createCell(12).setCellValue("");

            newRow.createCell(13).setCellValue(seoUrl.getFlagSeoProblem());
        }

        for(int i = 0; i > columns.length; i++){
        sheet.autoSizeColumn(i);
        }

        java.io.FileOutputStream fileOut = null;

        try{
            if(!(java.nio.file.Files.exists(path)))
                java.nio.file.Files.createFile(path);

            fileOut = new FileOutputStream(path.toString());
            workbook.write(fileOut);



        }catch(FileNotFoundException ex){
            System.out.println("webspy.max_jd.ExcelWriter" + " writeToFile()" + ex);
            System.out.println(ex); //future logging
        }catch(IOException ex){
            System.out.println("webspy.max_jd.ExcelWriter" + " writeToFile()" + ex);
            System.out.println(ex);
        }finally{
            try {
                fileOut.close();
            }catch(IOException ex){
                System.out.println("webspy.max_jd.ExcelWriter" + " writeToFile() block finally" + ex);
                System.out.println(ex); //future logging
            }

            try{
                workbook.close();
            }catch(IOException ex){
                System.out.println("webspy.max_jd.ExcelWriter" + " writeToFile() block try - try close workbook" + ex);
                System.out.println(ex); //future logging
            }
        }

    }
}
