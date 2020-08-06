package seospy.max_jd.start;

import seospy.max_jd.seo.SeoSpy;

public class Main {
    public static void main(String[] args){
        Thread thread = new Thread(() -> new SeoSpy());
        thread.start();
        try{
            thread.join();
        }catch(InterruptedException ex){
            SeoSpy.logToFile.error(Main.class + " " + ex.toString());
        }
    }
}

