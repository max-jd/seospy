package webspy.max_jd.utils.interfaces;


import webspy.max_jd.seo.newStructure.SeoEntity;

import java.io.File;
import java.util.Deque;
import java.util.Set;

public interface Exporter {
    public void export(Deque<SeoEntity> dequeUrls, Set<SeoEntity> imagesSet, File fileToWrite);
}
