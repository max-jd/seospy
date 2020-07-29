package webspy.max_jd.seo.newStructure;


public class SeoImage extends SeoEntity {


   public SeoImage(String url, boolean image) {
        super(url, image);
    }


    @Override
    public void analyzeUrl() {
        if(this.response != 200) haveSeoProblem = true;
        else  haveSeoProblem = false;
    }


}
