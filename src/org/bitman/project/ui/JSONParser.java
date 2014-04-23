package org.bitman.project.ui;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Proudly to use Intellij IDEA.
 * Created by ay27 on 14-4-14.
 */
public class JSONParser {

    private JSONParser() { }

    public static void parseCity(final String data, final ArrayList<Integer> cityId, final ArrayList<String> cityName) throws JSONException {
        try {
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                JSONArray temp = array.getJSONArray(i);
                cityId.add(temp.getInt(0));
                cityName.add(temp.getString(1));
            }
        } catch (JSONException e) {
            throw e;
        }
    }

    public static void parseFileList(final String receive, final ArrayList<String> fileList) throws JSONException {
        try {
            JSONArray array = new JSONArray(receive);
            for (int i=0; i<array.length(); i++) {
                fileList.add(array.getString(i));
            }
        }catch (JSONException e) {
            throw e;
        }
    }

    public static void parseCamList(final String receive, final ArrayList<String> camList) throws JSONException {
        try {
            JSONArray array = new JSONArray(receive);
            for (int i=0; i<array.length(); i++)
            {
                camList.add(array.getString(i));
            }
        }catch (JSONException e) {
            throw e;
        }
    }
}
