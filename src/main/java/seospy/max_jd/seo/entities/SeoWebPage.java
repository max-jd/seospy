package seospy.max_jd.seo.entities;

public class SeoWebPage extends SeoEntity {


    public SeoWebPage(String url) {

        super(url);

    }


    @Override
    public void analyzeUrl() {

        if((countH1 != 1) || (response != 200)) {
            haveSeoProblem = true;
        } else {
            haveSeoProblem = false;
        }

    }


    @Override
    public boolean equals(Object another) {

        if(super.equals(another)) {
            return another instanceof SeoWebPage;
        }

        return false;

    }


}
