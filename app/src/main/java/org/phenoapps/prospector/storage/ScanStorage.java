//package consumerphysics.com.myscioapplication.storage;
package org.phenoapps.prospector.storage;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nadavg on 01/08/2016.
 */
public class ScanStorage {

    private final static String SCAN_DIR = "scans";
    private File baseDir;

    private List<String> fileNames = new ArrayList<>();

    public void deleteScan(int position) {
        new File(baseDir, fileNames.get(position)).delete();

        fileNames.remove(position);
    }

    public void initScansStorage(final Context context) {
        baseDir = new File(context.getCacheDir() + File.separator + SCAN_DIR);
        baseDir.mkdirs();
        if (baseDir.isDirectory()) {
            String[] children = baseDir.list();
            for (int i = 0; i < children.length; i++) {
                new File(baseDir, children[i]).delete();
            }
        }
    }


    public List<String> getFileNames() {
        return fileNames;
    }

}
