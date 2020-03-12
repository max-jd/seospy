package webspy.max_jd;

public class Main {
    public static void main(String[] args){
        Thread thread = new Thread(() -> new WebSpy());
        thread.start();
        try{
            thread.join();
        }catch(InterruptedException ex){
            System.out.println(ex);
            WebSpy.logToFile.error(ex);
        }
    }
}

