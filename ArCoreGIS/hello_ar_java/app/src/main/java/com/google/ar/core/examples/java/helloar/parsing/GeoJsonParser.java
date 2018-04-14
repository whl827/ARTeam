package com.google.ar.core.examples.java.helloar.parsing;

import android.content.Context;
import android.util.Log;

import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.google.ar.core.examples.java.helloar.R;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Scanner;

import static android.content.ContentValues.TAG;

public class GeoJsonParser 
{
    //Feature-FeatureCollection-Point-Multipoint-LineString-MultiLineString-Polygon
    //Multipolygon-GeometryCollection-properties
    
    private FileReader jsonFile;
    private BufferedReader reader;
    private GeoJSONObject geoJson;
    private InputStream is;
    
    public GeoJsonParser (Context context, String fileName) {
        Scanner scanner = null;
        String wholeLine = "";
        try {
            is = context.getAssets().open(fileName);
//            openRawResource(R.raw.more_map);
            scanner = new Scanner(is);

//            jsonFile = new FileReader(new File(fileName));
//            reader = new BufferedReader(jsonFile);

            String jsonLine;
            while((jsonLine = scanner.next()) != null)
            {
                if(jsonLine==null){
                    break;
                }
                wholeLine += jsonLine;
                Log.d(TAG, "GeoJsonParser: reading - " + jsonLine + "\n");
            }
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "GeoJsonParser1: " + fnfe.getMessage(), fnfe );
        } catch (IOException ioe) {
            Log.e(TAG, "GeoJsonParser2: " + ioe.getMessage(), ioe);
        } catch (Exception e) {
            Log.e(TAG, "GeoJsonParser3: " + e.getMessage(), e);
        } finally {
            try {
                geoJson = GeoJSON.parse(wholeLine);
                Log.d(TAG, "tagme" + geoJson.toString());
                if (reader != null) {
                    reader.close();
                }
                if (jsonFile != null) {
                    jsonFile.close();
                }
            } catch (IOException ioe) {
                Log.e(TAG, "GeoJsonParser: " + ioe.getMessage(), ioe);
            } catch (JSONException jse){
                Log.d(TAG, "Gson Failed");
            }
        }
    }

    public GeoJSONObject getGeoJson()
    {
        return this.geoJson;
    }

    public ArrayList<ArrayList<Float>> getCoordinates(int index)
    {
        ArrayList<ArrayList<Float>> coords = new ArrayList<>();
        try {
            String rawCoords = ((FeatureCollection)(this.geoJson)).getFeatures().get(index).getGeometry().toJSON().getString("coordinates");
            rawCoords = rawCoords.replace("[","");
            rawCoords = rawCoords.replace("]","");

            String[] separated = rawCoords.split(",");

            for(int i = 0;i<separated.length;i+=3)
            {
                ArrayList<Float> adding = new ArrayList<>();
                adding.add(Float.parseFloat(separated[i]));
                adding.add(Float.parseFloat(separated[i+1]));
                adding.add(Float.parseFloat(separated[i+2]));

                coords.add(adding);
            }
            return coords;
        }
        catch (Exception e)
        {
            return null;
        }
    }
}
