package webspy.max_jd.start;

import webspy.max_jd.seo.WebSpy;

public class Main {
    public static void main(String[] args){
        Thread thread = new Thread(() -> new WebSpy());
        thread.start();
        try{
            thread.join();
        }catch(InterruptedException ex){
            System.out.println(ex.getClass().getName() + ex.getStackTrace());
            ex.printStackTrace();
            WebSpy.logToFile.error(ex);
        }
    }
}

