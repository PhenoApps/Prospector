//package consumerphysics.com.myscioapplication.storage;
package org.phenoapps.prospector.storage;

import android.content.Context;

import com.consumerphysics.android.sdk.model.ScioReading;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nadavg on 01/08/2016.
 */
public class ScanStorage {

    private final static String SCAN_DIR = "scans";
    private File baseDir;

    private List<String> fileNames = new ArrayList<>();
    private List<ScioReading> scioReadings = new ArrayList<>();

    public void deleteScan(int position) {
        new File(baseDir, fileNames.get(position)).delete();

        scioReadings.remove(position);
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

    public void saveScanToStorage(final ScioReading scioReading) {
        try {
            FileOutputStream fos = new FileOutputStream(new File(baseDir, String.valueOf(System.currentTimeMillis())));
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(scioReading);
            os.close();
            fos.close();
        }
        catch (FileNotFoundException e) {
        }
        catch (IOException e) {
        }
    }

    public void loadScansFromStorage() {
        File[] filelist = baseDir.listFiles();
        String[] theNamesOfFiles = new String[filelist.length];
        scioReadings.clear();
        fileNames.clear();
        for (int i = 0; i < theNamesOfFiles.length; i++) {
            try {
                FileInputStream fis = new FileInputStream(new File(baseDir, filelist[i].getName()));
                ObjectInputStream is = new ObjectInputStream(fis);
                ScioReading reading = (ScioReading) is.readObject();
                is.close();
                fis.close();

                fileNames.add(filelist[i].getName());
                scioReadings.add(reading);
            }
            catch (FileNotFoundException e) {
            }
            catch (OptionalDataException e) {
            }
            catch (StreamCorruptedException e) {
            }
            catch (IOException e) {
            }
            catch (ClassNotFoundException e) {
            }
        }
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public List<ScioReading> getScioReadings() {
        return scioReadings;
    }
}
