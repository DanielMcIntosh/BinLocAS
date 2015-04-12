package com.example.binloc;

import android.graphics.PointF;

import com.google.android.gms.maps.model.LatLng;

public class Bin implements Comparable{
	private LatLng loc;
	private int types;
	public static final int LitterMask = 0B001, RecyclingMask = 0B010, GreenMask = 0B100, PersonMask = 0B1000;

    private static int distAlgrthm = 1;
	
	public Bin(LatLng loc, int types)
	{
		this.loc = loc;
		this.types = types;
	}
	
	@Override
	public int compareTo(Object other) {
		Bin b2 = (Bin)other;
		return (int) Math.copySign(1, MainActivity.myLocation.getDist(this, 1)-MainActivity.myLocation.getDist(b2, 1));
	}
	
	
	
	/**
	 * calculates the approximate distance within an error of 
	 * points should be in the form x = latitude, y = longitude
	 * @param p2
	 * @param technique
	 * 		controls which technique is used to calculate distance.
	 * 		0 will use pythagorean, 1 haversine, 2 Spherical law of cosines 
	 * @return distance
	 */
    public double getDist(Bin p2, int technique) { return getDist(p2.getLatLng(), technique);}
	public double getDist(LatLng p2, int technique)
	{
		int R = 6371000; // metres
		double lat1 = Math.toRadians(this.getLat()), lat2 = Math.toRadians(p2.latitude);
		double deltaLat = Math.toRadians(lat2-lat1);
		double lon1 = Math.toRadians(this.getLon()), lon2 = Math.toRadians(p2.longitude);
		double deltaLon = Math.toRadians(lon2-lon1);
		
		double d = 0;
		switch(technique){
			case 0: //pythagorean
			{
				double x = (lon2-lon1) * Math.cos((lat1+lat2)/2);
				double y = (lat2-lat1);
				d = Math.sqrt(x*x + y*y) * R;
				break;
			}
			case 1: //haversine
			{
				double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
				        Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
				double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
				d = R * c;
				break;
			}
			case 2: //Spherical Law of Cosines
			{
				d = Math.acos( Math.sin(lat1)*Math.sin(lat2) + Math.cos(lat1)*Math.cos(lat2) * Math.cos(deltaLon) ) * R;
			}
		}
		return d;
	}
	
	public double getLat() { return loc.latitude; }
	public double getLon() { return loc.longitude; }
	public LatLng getLatLng() { return loc; }

	public int getTypes() {return types; }
	public boolean isLitter() { return (types&LitterMask) != 0; }
	public boolean isRecycling() { return (types&RecyclingMask) != 0; }
	public boolean isGreenBin() { return (types&GreenMask) != 0; }
	public boolean isPerson() { return (types&PersonMask) != 0; }

    public static void setDistAlgrthm(int distAlgrthm) { Bin.distAlgrthm = distAlgrthm; }
    public static int getDistAlgrthm() { return distAlgrthm; }
}
