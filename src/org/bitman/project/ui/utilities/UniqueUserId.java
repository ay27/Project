package org.bitman.project.ui.utilities;

import android.content.Context;
import org.bitman.project.ProjectApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-27.
 */
public class UniqueUserId {

    private Context context;
    private String uuid = null;
    private static final String FILENAME = "Installation";
    private File file;

    private static UniqueUserId instance = null;

    public static UniqueUserId getInstance() {
        if (instance == null) {
            return instance = new UniqueUserId();
        }
        return instance;
    }

    private UniqueUserId() {
        this.context = ProjectApplication.instance;

        file = new File(context.getFilesDir(), FILENAME);
        try {
            if (file.exists())
                uuid = readFile(file);
            else
                uuid = writeFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getId() {
        return uuid;
    }

//    public boolean resetId() {
//        boolean flag = file.delete();
//        if (flag) {
//            file = new File(context.getFilesDir(), FILENAME);
//            try {
//                uuid = writeFile(file);
//            } catch (IOException e) {
//                e.printStackTrace();
//                return false;
//            }
//        }
//        return flag;
//    }

    private String readFile(File file) throws IOException {
        RandomAccessFile f = new RandomAccessFile(file, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private String writeFile(File file) throws IOException{
        FileOutputStream out = new FileOutputStream(file);
        String id = UUID.randomUUID().toString();
        String[] ids = id.split("-");
        id = ids[0]+ids[1]+ids[2]+ids[3]+ids[4];
        out.write(id.getBytes());
        out.close();

        return id;
    }
}
