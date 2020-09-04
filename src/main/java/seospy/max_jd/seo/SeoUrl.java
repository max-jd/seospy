package seospy.max_jd.seo;

import lombok.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Deprecated
//Builder
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode
public class SeoUrl implements Comparable<SeoUrl>, Serializable {
    @NonNull
    private final String url;
    private String canonical;
    private int response;
    private String title;
    private String description;
    private String keywords;
    private int countH1;
    private String metaRobots;
    private String contentType;

    private Boolean haveSeoProblem;
    @NonNull
    private final boolean isImage;

    //collecting information
    public static Map<String, HashSet<String>> statisticLinksOn;
    public static Map<String, HashSet<String>> statisticLinksOut;
    public static Map<String, HashSet<String>> imagesReferredByPages;
    public static Map<String, HashSet<String>> externalLinks;
    public static Map<String, String> cacheContentTypePages;

    static {
        setNewStatistics();
    }

    SeoUrl(String url) {
        this.url = url;
        //by default this's not an image
        isImage = false;
    }

    public static void setNewStatistics() {
        statisticLinksOn = new HashMap<String, HashSet<String>>();
        statisticLinksOut = new HashMap<String, HashSet<String>>();
        externalLinks = new HashMap<String, HashSet<String>>();
        cacheContentTypePages = new HashMap<String, String>();
        imagesReferredByPages = new HashMap<String, HashSet<String>>();
    }

    @Override
    public int compareTo(SeoUrl s2) {
        return this.url.compareTo(s2.url);
    }

    @Override
    public String toString() {
        return url.toString();
    }

    public void analyzeURL() {
        if (isImage) {
            analysisAsImage();
            return;
        }
        analysisAsSeoUrl();
    }

    private void analysisAsImage() {
        if (this.response != 200) haveSeoProblem = true;
        else haveSeoProblem = false;
    }

    private void analysisAsSeoUrl() {
        if ((countH1 > 1 | countH1 == 0) || (response != 200)) {
            haveSeoProblem = true;
        } else {
            haveSeoProblem = false;
        }
    }

    public boolean isImage() {
        return isImage;
    }

    public Boolean isHaveSeoProblem() {
        return haveSeoProblem;
    }

}

/*

//Singleton and Director for set up SeOUrls
class TunnerSeoURL {
    private static volatile TunnerSeoURL tunner;
    private TunnerSeoURL() {
    }

    static TunnerSeoURL getTunner() {
        if (tunner == null)
            return tunner = new TunnerSeoURL();
        return tunner;
    }

    SeoUrl tunne(SeoUrl url, HtmlPage page) {
        if(url.isImage()) {
            setResponse(url, page);
            setContentType(url);
        } else { //it's a SeoUrl
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
           */
/* //if elements's parent equals script or noscript element - then we don't need it
            if(el.getParentNode().toString().contains("script")){
                continue;
            }*//*

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

*/
