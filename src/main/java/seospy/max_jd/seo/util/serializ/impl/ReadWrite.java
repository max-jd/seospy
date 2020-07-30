package seospy.max_jd.seo.util.serializ.impl;

import org.apache.commons.lang3.ArrayUtils;
import seospy.max_jd.seo.SeoUrl;
import seospy.max_jd.seo.entities.SeoEntity;
import seospy.max_jd.seo.util.serializ.interfaces.Exporter;
import seospy.max_jd.seo.util.serializ.interfaces.Loader;
import seospy.max_jd.seo.util.serializ.interfaces.Saver;

import java.io.*;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReadWrite implements Exporter, Loader, Saver {
    private String extensionExport = ".xlsx";
    private String extensionLoad = ".ser";
    private String extensionSave = ".ser";

    public void export(Deque<SeoEntity> dequeUrls, Set<SeoEntity> imagesSet, File fileToWrite) {
        SeoUrl[] arraySeoUrls = new SeoUrl[dequeUrls.size()];
        dequeUrls.toArray(arraySeoUrls);
        SeoUrl[] arraySeoImages = new SeoUrl[imagesSet.size()];
        imagesSet.toArray(arraySeoImages);

        SeoUrl[] joinedSeoArray = ArrayUtils.addAll(arraySeoUrls,arraySeoImages);
        ExcelWriter.writeToFile(fileToWrite.toPath(), joinedSeoArray);
    }

    public void loadFrom(Deque<SeoEntity> dequeUrls, Set<SeoEntity> imagesSet, File fileFrom) throws IOException, ClassNotFoundException {
        try(FileInputStream fileInputStream = new FileInputStream(fileFrom);
            ObjectInputStream inStreamOb = new ObjectInputStream(fileInputStream)) {

            Deque<SeoEntity> tempCopyDeque = (Deque)inStreamOb.readObject();
            dequeUrls.addAll(tempCopyDeque);
            Set<SeoEntity> tempCopySet = (Set<SeoEntity>) inStreamOb.readObject();
            imagesSet.addAll(tempCopySet);
            SeoEntity.statisticLinksOn = (Map<String, HashSet<String>>) inStreamOb.readObject();
            SeoEntity.statisticLinksOut = (Map<String, HashSet<String>>)inStreamOb.readObject();
            SeoEntity.externalLinks = (Map<String, HashSet<String>>) inStreamOb.readObject();
            SeoEntity.cacheContentTypePages = (Map<String,String>) inStreamOb.readObject();
        }
    }

    public void saveTo(Deque<SeoEntity> dequeUrls, Set<SeoEntity> imagesSet, File fileTo) throws IOException, ClassNotFoundException {

       /* if(!fileTo.exists()){
            fileTo.createNewFile();
        }*/
        try(FileOutputStream fileOut = new FileOutputStream(fileTo, false);
            ObjectOutputStream objectsOutput = new ObjectOutputStream(fileOut)) {
                objectsOutput.writeObject(dequeUrls);
                objectsOutput.writeObject(imagesSet);
                objectsOutput.writeObject(SeoEntity.statisticLinksOn);
                objectsOutput.writeObject(SeoEntity.statisticLinksOut);
                objectsOutput.writeObject(SeoEntity.externalLinks);
                objectsOutput.writeObject(SeoEntity.cacheContentTypePages);
            }
        }

    public String getExtensionExport() {
        return extensionExport;
    }

    public String getExtensionForLoad() {
        return extensionLoad;
    }

    public String getExtensionToSave() {
        return extensionSave;
    }
}


