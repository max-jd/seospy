package seospy.max_jd.seo.util;

import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import seospy.max_jd.seo.SeoUrl;
import seospy.max_jd.seo.entities.SeoEntity;

//Singleton and Director for set up SeOUrls
public class TunnerSeoUrl {
        private static volatile TunnerSeoUrl tunner;
        private TunnerSeoUrl() {
        }

        public static TunnerSeoUrl getTunner() {
            if (tunner == null)
                return tunner = new TunnerSeoUrl();
            return tunner;
        }

        public SeoEntity tunne(SeoEntity url, HtmlPage page) {
            if(SeoEntity.isImage(url)) {
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


        private void setResponse(SeoEntity seoUrl, HtmlPage page) {
            seoUrl.setResponse(page.getWebResponse().getStatusCode());
        }


        private void setCanonical(SeoEntity seoUrl, HtmlPage page) {
            DomNodeList<HtmlElement> domElements = page.getHead().getElementsByTagName("link");

            for (HtmlElement el : domElements) {
           /* //if elements's parent equals script or noscript element - then we don't need it
            if(el.getParentNode().toString().contains("script")){
                continue;
            }*/
                if (el.getAttribute("rel").equalsIgnoreCase("canonical")) {
                    String href = el.getAttribute("href");
                    seoUrl.setCanonical(href);
                    break;
                }
            }
        }


        private void setTitle(SeoEntity seoUrl, HtmlPage page) {
            seoUrl.setTitle(page.getTitleText());
        }


        private void setDescription(SeoEntity seoUrl, HtmlPage page) {
            DomNodeList<HtmlElement> listMeta = page.getHead().getElementsByTagName("meta");
            for (HtmlElement el : listMeta) {
                if (el.getAttribute("name").equalsIgnoreCase("description")) {
                    seoUrl.setDescription(el.getAttribute("content"));
                    break;
                }
            }
        }


        private void setKeyword(SeoEntity seoUrl, HtmlPage page) {
            DomNodeList<HtmlElement> listMeta = page.getHead().getElementsByTagName("meta");

            for (HtmlElement el : listMeta) {
                if (el.getAttribute("name").equalsIgnoreCase("keywords")) {
                    seoUrl.setKeywords(el.getAttribute("content"));
                    break;
                }
            }
        }


        private void countH1(SeoEntity seoUrl, HtmlPage page) {
            DomNodeList<HtmlElement> listH1 = page.getBody().getElementsByTagName("h1");
            seoUrl.setCountH1(listH1.size());
        }


        private void setMetarobots(SeoEntity seoUrl, HtmlPage page) {
            DomNodeList<HtmlElement> listMeta = page.getHead().getElementsByTagName("meta");

            for (HtmlElement el : listMeta) {
                if (el.getAttribute("name").equalsIgnoreCase("robots")) {
                    seoUrl.setMetaRobots(el.getAttribute("content"));
                    break;
                }
            }
        }


        private void setContentType(SeoEntity seoUrl) {
            seoUrl.setContentType(SeoUrl.cacheContentTypePages.get(seoUrl.toString()));
        }

}



