package webspy.max_jd.utils.interfaces;


import webspy.max_jd.seo.entities.SeoEntity;

import java.io.File;
import java.util.Deque;
import java.util.Set;

public interface Loader {
    void loadFrom(Deque<SeoEntity> dequeUrls, Set<SeoEntity> imagesSet, File fileFrom) throws Exception;
}
