package com.shengdan.map_convert;

public class Main {

    public static void main(String[] args) {
        System.out.println(System.currentTimeMillis());
        MapConvert m = new MapConvert(600, 758, 14, new LatLng(30.772389001386095, 120.68156035497184),2);
        System.out.println("targetPoint: "+m.getPoint(new LatLng(30.77347790731735,120.68347644410971)));
        System.out.println("targetLatLng： "+m.getLatLng(new Point(389.29622594825923,319.9373898431659)));
        System.out.println(System.currentTimeMillis());
    }
}
