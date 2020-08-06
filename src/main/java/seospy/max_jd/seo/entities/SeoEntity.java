package seospy.max_jd.seo.entities;


import lombok.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

//Builder
@Getter
@Setter
@RequiredArgsConstructor
abstract public class SeoEntity implements Comparable<SeoEntity>, Serializable {


    @NonNull
    protected final String  url;
    protected String        canonical;
    protected int           response;
    protected String        title;
    protected String        description;
    protected String        keywords;
    protected int           countH1;
    protected String        metaRobots;
    protected String        contentType;

    protected Boolean       haveSeoProblem;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    @NonNull
    private final boolean isImage;


    //collecting information
    public static Map<String, HashSet<String>>  statisticLinksOn;
    public static Map<String, HashSet<String>>  statisticLinksOut;
    public static Map<String, HashSet<String>>  imagesReferredByPages;
    public static Map<String, HashSet<String>>  externalLinks;
    public static Map<String, String>           cacheContentTypePages;


    public SeoEntity(@NonNull String url) {
        this(url, false);
    }


    static {
        setNewStatistics();
    }


    public static void setNewStatistics() {
        statisticLinksOn = new HashMap<String, HashSet<String>>();
        statisticLinksOut = new HashMap<String, HashSet<String>>();
        externalLinks = new HashMap<String, HashSet<String>>();
        cacheContentTypePages = new HashMap<String, String>();
        imagesReferredByPages = new HashMap<String, HashSet<String>>();
    }


    public static Boolean isImage(SeoEntity seoEntity) {
        return seoEntity.isImage;
    }

    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if(other == this) return true;
        if(!(other instanceof SeoEntity))return false;
        SeoEntity otherSeoUrl = (SeoEntity) other;
        return otherSeoUrl.url.equals(this.url);
    }


    @Override
    public int compareTo(SeoEntity secondSeoEntity) {
        return this.url.compareTo(secondSeoEntity.url);
    }


    @Override
    public int hashCode() {
        return url.hashCode();
    }


    @Override
    public String toString() {
        return url.toString();
    }


    abstract public void analyzeUrl();
      /*  if(isImage) {
            analysisAsImage();
            return;
        }
        analysisAsSeoUrl();
    }*/


    private void analysisAsImage() {
        if(this.response != 200) haveSeoProblem = true;
        else  haveSeoProblem = false;
    }


    public Boolean isHaveSeoProblem(){
        return haveSeoProblem;
    }


    private void analysisAsSeoUrl() {
        if((countH1 > 1 | countH1 == 0) || (response != 200)) {
            haveSeoProblem = true;
        } else {
            haveSeoProblem = false;
        }
    }


}
