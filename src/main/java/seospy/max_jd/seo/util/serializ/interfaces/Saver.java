package seospy.max_jd.seo.util.serializ.interfaces;

import seospy.max_jd.seo.entities.SeoEntity;

import java.io.File;
import java.util.Deque;
import java.util.Set;

public interface Saver {
    public void saveTo(Deque<SeoEntity> dequeUrls, Set<SeoEntity> imagesSet, File fileTo) throws Exception;
}
