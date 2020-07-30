package seospy.max_jd.seo.util.serializ.interfaces;


import seospy.max_jd.seo.entities.SeoEntity;

import java.io.File;
import java.util.Deque;
import java.util.Set;

public interface Loader {
    void loadFrom(Deque<SeoEntity> dequeUrls, Set<SeoEntity> imagesSet, File fileFrom) throws Exception;
}
