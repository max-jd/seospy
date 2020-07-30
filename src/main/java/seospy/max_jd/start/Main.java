package seospy.max_jd.start;

import seospy.max_jd.seo.SeoSpy;

public class Main {
    public static void main(String[] args){
        Thread thread = new Thread(() -> new SeoSpy());
        thread.start();
        try{
            thread.join();
        }catch(InterruptedException ex){
            System.out.println(ex.getClass().getName() + ex.getStackTrace());
            ex.printStackTrace();
            SeoSpy.logToFile.error(ex);
        }
    }
}

