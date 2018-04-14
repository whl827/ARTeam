package com.google.ar.core.examples.java.helloar;

public class FindPointHelper {
    private float r0; //radius of first circle - our circle
    private float r1; //radius of second circle

    private float c; // x of center of second circle
    private float d; // y of center of second cricle

    private float lon1, lat1, lon2, lat2, lon3, lat3;

    private float distance;
    private float rou;
    private float xrou;
    private float yrou;

    private float x3;
    private float y3;

    //TODO; first use button to get first location, then walk a few and make another button to say this is location to sit at.
    //TODO; pass in 3 lon and lats.
    //TODO; rad1 = distance between current location to object
    //TODO; rad2 = distance between point2 location to object
    //TODO; then set r0=rad1 and r1=rad2

    //Todo; call getX and getY which are poses coordinates to create a pose.
    // Rad 1 is radius / distance from current lon/lat to lon/lad of point. which is lo3 and la3
    public FindPointHelper(float rad1, float rad2, float x2, float y2,
                           float lo1, float la1, float lo2, float la2,
                           float lo3, float la3)
    {
        this.r0 = rad1;
        this.r1 = rad2;
        this.c = x2;
        this.d = y2;
        this.lon1 = lo1;
        this.lat1 = la1;
        this.lon2 = lo2;
        this.lat2 = la2;
        this.lon3 = lo3;
        this.lat3 = la3;

        this.distance = calculateD();
        this.rou = calcRou();

        this.xrou = calcXRou();
        this.yrou = calcYRou();

        calcXdirection();
        calcYdirection();

        this.x3 = calcX()+this.xrou;
        this.y3 = calcY()+this.yrou;
    }

    //This is trying to get the distance between the current position and
    // the 1st location placed/ second point.
    private float calculateD()
    {
        float preSqrt = (float)(Math.pow(c,2) + Math.pow(d,2));
        return (float)Math.sqrt(preSqrt);
    }

    //Calculates the rou needed to use to multiple the rou * d
    private float calcRou()
    {
        float v1 = (this.distance+r0+r1);
        float v2 = (this.distance+r0-r1);
        float v3 = (this.distance-r0+r1);
        float v4 = (-this.distance+r0+r1);

        return ((float)Math.sqrt(v1*v2*v3*v4))/4;
    }

    private float calcXRou()
    {
        return (2*this.rou*(-this.d/((float)Math.pow(this.distance,2))));
    }

    private float calcYRou()
    {
        return (2*this.rou*(-this.c/((float)Math.pow(this.distance,2))));
    }

    //This gets X without the rou * d
    private float calcX()
    {
        return ((this.c / 2) + (this.c * ((float) Math.pow(this.r0, 2) - (float) Math.pow(this.r1, 2)) /
                (2 * (float) Math.pow(distance, 2))));
    }

    //This gets Y without the rou * d
    private float calcY()
    {
        return ((this.d/2) + (this.d*((float)Math.pow(this.r0,2) - (float)Math.pow(this.r1,2))/
                (2 * (float) Math.pow(distance,2))));
    }

    private void calcXdirection()
    {
        //we know that if lon  smaller than to the left
        if(this.lon3 < this.lon1)
        {
            this.xrou = -this.xrou;
        }
    }

    private void calcYdirection()
    {
        //we know that this lat is less so its downward. y-
        if(this.lat3 < this.lat1)
        {
            this.yrou = -this.yrou;
        }
    }

    private float getX()
    {
        return this.x3;
    }

    private float getY()
    {
        return this.y3;
    }
}
