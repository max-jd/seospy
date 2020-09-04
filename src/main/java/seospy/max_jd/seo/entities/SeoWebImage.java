package seospy.max_jd.seo.entities;

public class SeoWebImage extends SeoEntity {


    public SeoWebImage(String url, boolean image) {

        super(url, image);

    }


    @Override
    public void analyzeUrl() {

        if (this.response != 200) haveSeoProblem = true;
        else haveSeoProblem = false;

    }


    @Override
    public boolean equals(Object another) {

        if (super.equals(another)) {
            return another instanceof SeoWebImage;
        }

        return false;

    }


}
