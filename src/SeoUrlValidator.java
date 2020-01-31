import org.apache.commons.validator.routines.UrlValidator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class SeoUrlValidator extends UrlValidator {
    private String host;

    public SeoUrlValidator(String host, String[] schemes, long options){
        super(schemes, options);
        this.host = host;
    }

    public boolean isSameHost(URL url){
        URL urlHost = null;
        try{
            urlHost = new URL(host);
        }catch(MalformedURLException ex){
            System.out.println(this.getClass().getName() + " isSameHost()" + ex);
            System.out.println(ex);
        }
      //  System.out.println("host " + host);
       // System.out.println("url " + url.getHost());
       // System.out.println("urlHost " + urlHost.getHost());
       // System.out.println("urlHost equals url: " + url.getHost().equals(urlHost.getHost()));

        return url.getHost().equals(urlHost.getHost());
    }

    public boolean havePoundSign(URL url){
        return url.toString().contains("#");
    }

    public boolean isImage(String url){
        String lowerCase = url.toLowerCase();
        return  lowerCase.endsWith(".png") || lowerCase.endsWith(".jpg") || lowerCase.endsWith(".gif") ||
                lowerCase.endsWith((".jpeg"));
        //return page.getContentType().equalsIgnoreCase("image/png");
    }

    public boolean isSchemeHttpOrHttps(String url){
        return url.startsWith("http");
    }


    public String getContentType(String url){

        String contentType = "";
        try {
            URL realUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestProperty("User-Agent", "Mazilla");
            connection.setRequestMethod("HEAD");
            connection.connect();
            contentType = connection.getContentType();
         //   System.out.println(url + ": " + contentType);
        }catch(MalformedURLException ex){
            WebSpy.logToFile.error(ex.toString());
            System.out.println(this.getClass().getName() + " getContentType()" + ex);
        }catch(IOException ex){
            System.out.println(this.getClass().getName() + " getContentType()" + ex);
            System.out.println(ex);
        }
         return contentType.toLowerCase();
    }

    public boolean isUrlValid(URL url){
        return super.isValid(url.toString());
    }

}
