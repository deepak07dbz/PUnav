package com.example.accelerometer;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    public static boolean copyAssetsToStorage(Context context, String sourceFolder, String destinationFolder) {
        AssetManager assetManager = context.getAssets();
        String[] files;
        boolean allCopied = true;
        try {
            // List files in the assets folder
            files = assetManager.list(sourceFolder);
            if (files != null) {
                for (String filename : files) {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        // Open file from assets folder
                        in = assetManager.open(sourceFolder + File.separator + filename);
                        // Create file in app's isolated directory
                        File outFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + File.separator + destinationFolder + File.separator + filename);
                        // Copy content
                        Log.d("FILE", outFile.toString());
                        outFile.getParentFile().mkdirs();
                        out = new FileOutputStream(outFile);
                        copyFile(in, out);
                        Log.d("FILE", filename + "done");
                    } catch(IOException e) {
                        e.printStackTrace();
                        allCopied = false;
                    } finally {
                        // Close streams
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            allCopied = false;
        }
        return allCopied;
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
