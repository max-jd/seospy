package webspy.max_jd.utils.interfaces;


import webspy.max_jd.seo.SeoUrl;

import java.io.File;
import java.util.Deque;
import java.util.Set;

public interface Loader {
    void loadFrom(Deque<SeoUrl> dequeUrls, Set<SeoUrl> imagesSet, File fileFrom) throws Exception;
}
