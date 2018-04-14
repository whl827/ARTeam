package com.google.ar.core.examples.java.helloar;

import java.io.Serializable;

/**
 * Created by Edward on 4/13/2018.
 */

public class ARObject implements Serializable {

    private String objName;
    private String objDesc;

    public ARObject() {

    }

    public String getObjName() {
        return objName;
    }

    public String getObjDesc() {
        return objDesc;
    }

    public void setObjName(String name) {
        objName = name;
    }

    public void setObjDesc(String desc) {
        objDesc = desc;
    }
}
