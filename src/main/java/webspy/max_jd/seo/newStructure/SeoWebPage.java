package webspy.max_jd.seo.newStructure;

public class SeoWebPage extends SeoEntity {


    public SeoWebPage(String url) {
        super(url);
    }


    @Override
    public void analyzeUrl() {
        if((countH1 != 0) || (response != 200)) {
            haveSeoProblem = true;
        } else {
            haveSeoProblem = false;
        }
    }


}
