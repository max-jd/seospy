package webspy.max_jd;

import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.Serializable;
import java.util.*;

//Builder
public class SeoUrl implements Comparable<SeoUrl>, Serializable{
    private final String  url;
    private String  canonical;
    private int     response;
    private String  title;
    private String  description;
    private String  keywords;
    private int     countH1;
    private String  metaRobots;

    private String  contentType;

    private boolean haveSeoProblem;
    private final boolean isImage;

    public static Map<String, HashSet<String>> statisticLinksOn;
    public static Map<String, HashSet<String>> statisticLinksOut;
    public static Map<String, HashSet<String>> externalLinks;
    public static Map<String, String> cacheContentTypePages;
    public static Map<String, HashSet<String>> imagesReferredByPages;//delete?

    static{
        statisticLinksOn = Collections.synchronizedMap(new HashMap<String, HashSet<String>>());
        statisticLinksOut = Collections.synchronizedMap(new HashMap<String, HashSet<String>>());
        externalLinks = new HashMap<String, HashSet<String>>();
        cacheContentTypePages = Collections.synchronizedMap(new HashMap<String, String>());
        imagesReferredByPages = new HashMap<String, HashSet<String>>();
    }

    SeoUrl(String url){
        this.url = url;
        isImage = false;
    }
    SeoUrl(String url, boolean isImage){
        this.url = url;
        this.isImage = isImage;
    }
    @Override
    public int compareTo(SeoUrl s2) {
        return this.url.compareTo(s2.url);
    }

    @Override
    public int hashCode(){
        return url.hashCode();
    }

    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if(other == this) return true;
        if(!(other instanceof SeoUrl))return false;
        SeoUrl otherSeoUrl = (SeoUrl) other;
        return otherSeoUrl.url.equals(this.url);
    }

    @Override
    public String toString(){
        return url;
    }

    public void analyzeURL(){
        if(isImage){
            if(this.response != 200) haveSeoProblem = true;
            else  haveSeoProblem = false;
            return;
        }

        if((countH1 > 1 | countH1 == 0) || (response != 200) )
            haveSeoProblem = true;
        else haveSeoProblem = false;
    }


    /*only setters*/
    public void setCanonical(String canonical){this.canonical = canonical;}
    public void setResponse(int response){this.response = response;}
    public void setTitle(String title){this.title = title;}
    public void setDescription(String description){this.description = description;}
    public void setKeywords(String keywords){this.keywords = keywords;}
    public void setCountH1(int countH1){this.countH1 = countH1;}
    public void setMetaRobots(String metaRobots){this.metaRobots = metaRobots;}
    public void setContentType(String contentType){ this.contentType = contentType; }
    public void setHaveSeoProblem(boolean seoProblem){this.haveSeoProblem = seoProblem;}

    /*Only getters*/
    public String getURL(){return url;}
    public String getCanonical(){return canonical;}
    public int getResponse(){ return response;}
    public String getTitle(){return title;}
    public String getDescription(){return description;}
    public String getKeywords(){return keywords;}
    public int getCountH1(){return countH1;}
    public String getMetarobots(){return metaRobots;}
    public String getContentType(){return contentType;}
    public Boolean getFlagSeoProblem(){
        return haveSeoProblem;
    }


    public boolean isImage(){
        return isImage;
    }

}

//Singleton and Director for set up SeOUrls
class TunnerSeoURL {
    private static TunnerSeoURL tunner;

    private TunnerSeoURL() {
    }

    static TunnerSeoURL getTunner() {
        if (tunner == null)
            return tunner = new TunnerSeoURL();
        return tunner;
    }

    SeoUrl tunne(SeoUrl url, HtmlPage page) {
        if(url.isImage()){
            setResponse(url, page);
            setContentType(url);
        } else {
        setResponse(url, page);
        setCanonical(url, page);
        setTitle(url, page);
        setDescription(url, page);
        setKeyword(url, page);
        countH1(url, page);
        setMetarobots(url, page);
        setContentType(url);
        }
        return url;
    }

    private void setResponse(SeoUrl seoUrl, HtmlPage page) {
        seoUrl.setResponse(page.getWebResponse().getStatusCode());
    }

    private void setCanonical(SeoUrl seoUrl, HtmlPage page) {
        DomNodeList<HtmlElement> domElements = page.getHead().getElementsByTagName("link");

        for (HtmlElement el : domElements) {
            if (el.getAttribute("rel").equalsIgnoreCase("canonical")) {
                String href = el.getAttribute("href");
                seoUrl.setCanonical(href);
                break;
            }
        }
    }

    private void setTitle(SeoUrl seoUrl, HtmlPage page) {
        seoUrl.setTitle(page.getTitleText());
    }

    private void setDescription(SeoUrl seoUrl, HtmlPage page) {
        DomNodeList<HtmlElement> listMeta = page.getHead().getElementsByTagName("meta");
        for (HtmlElement el : listMeta) {
            if (el.getAttribute("name").equalsIgnoreCase("description")) {
                seoUrl.setDescription(el.getAttribute("content"));
                break;
            }
        }
    }

    private void setKeyword(SeoUrl seoUrl, HtmlPage page) {
        DomNodeList<HtmlElement> listMeta = page.getHead().getElementsByTagName("meta");

        for (HtmlElement el : listMeta) {
            if (el.getAttribute("name").equalsIgnoreCase("keywords")) {
                seoUrl.setKeywords(el.getAttribute("content"));
                break;
            }
        }
    }

    private void countH1(SeoUrl seoUrl, HtmlPage page) {
        DomNodeList<HtmlElement> listH1 = page.getBody().getElementsByTagName("h1");
        seoUrl.setCountH1(listH1.size());
    }

    private void setMetarobots(SeoUrl seoUrl, HtmlPage page) {
        DomNodeList<HtmlElement> listMeta = page.getHead().getElementsByTagName("meta");

        for (HtmlElement el : listMeta) {
            if (el.getAttribute("name").equalsIgnoreCase("robots")) {
                seoUrl.setMetaRobots(el.getAttribute("content"));
                break;
            }
        }
    }

    private void setContentType(SeoUrl seoUrl) {
        seoUrl.setContentType(SeoUrl.cacheContentTypePages.get(seoUrl.toString()));
    }
}

