package webspy.max_jd.utils;

import org.apache.commons.lang3.ArrayUtils;
import webspy.max_jd.seo.SeoUrl;
import webspy.max_jd.seo.newStructure.SeoEntity;
import webspy.max_jd.utils.interfaces.Exporter;
import webspy.max_jd.utils.interfaces.Loader;
import webspy.max_jd.utils.interfaces.Saver;

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


