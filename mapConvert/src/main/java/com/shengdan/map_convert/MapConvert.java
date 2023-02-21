package com.shengdan.map_convert;

/**
 * 经纬度与平面地图像素坐标互转
 */
public class MapConvert {

    public final static double R = 6378137;//地球半径
    public final static double EarthP = 2 * Math.PI * R;//地球赤道周长
    public final static double MAX_LATITUDE = 85.0511287798;
    public final static int singleTileSize = 256;//单个瓦片长度
    public final static double InvertEarthP = 1 / EarthP;//地球赤道周长的倒数

    private int mapWidth;
    private int mapHeight;
    private LatLng centerLatLng;
    private Point centerPoint;
    //当zoom为0时，只有一张瓦片也就是256像素,计算公式:256 * Math.pow(2, zoom)
    private double allTileSize;


    public MapConvert(int mapWidth, int mapHeight, int zoom, LatLng centerLatLng, float density) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.centerLatLng = centerLatLng;
//        this.density = density;

        this.allTileSize = density * singleTileSize * Math.pow(2, zoom);
        this.centerPoint = toPixel(toMercator(centerLatLng));
    }

    /**
     * 非移动端的设备可以不传density，默认为1
     *
     * @param mapWidth
     * @param mapHeight
     * @param zoom
     * @param centerLatLng
     */
    public MapConvert(int mapWidth, int mapHeight, int zoom, LatLng centerLatLng) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.centerLatLng = centerLatLng;
        this.allTileSize = (singleTileSize * Math.pow(2, zoom));
        centerPoint = toPixel(toMercator(centerLatLng));
    }


    /**
     * 将经纬度转为平面墨卡托投影坐标
     * 这里是 WGS84 Web墨卡托 即 EPSG3857，单位是米
     *
     * @param latLng 经纬度
     * @return
     */
    private Point toMercator(LatLng latLng) {
        double d = Math.PI / 180;
        double max = MAX_LATITUDE;
        double lat = Math.max(Math.min(max, latLng.lat), -max);
        double sin = Math.sin(lat * d);

        return new Point(
                R * latLng.lng * d,
                R * Math.log((1 + sin) / (1 - sin)) / 2);
    }

    /**
     * 将墨卡托坐标换算成像素坐标，公式如下:
     * (总像素 * (地球赤道周长的倒数 * 墨卡托坐标x+0.5),总像素 * (地球赤道周长的倒数 * 墨卡托坐标y + 0.5))
     * 这里为什么要加0.5?
     *
     * @param point 墨卡托坐标Point
     * @return
     */
    private Point toPixel(Point point) {
        point.x = allTileSize * (point.x * InvertEarthP + 0.5);
        point.y = allTileSize * (point.y * -InvertEarthP + 0.5);
        return point;
    }

    /**
     * 根据 中心平面像素坐标 换算得出 目标平面像素坐标
     * 容器内目标点x = （目标点x - 中心点x）+ 容器宽/2
     * 容器内目标点y = （目标点y - 中心点y）+ 容器高/2
     *
     * @param latLng
     * @return
     */
    public Point getPoint(LatLng latLng, float heading) {
        Point targetPoint = toPixel(toMercator(latLng));

        targetPoint.x = (targetPoint.x - centerPoint.x) + mapWidth / 2;
        targetPoint.y = (targetPoint.y - centerPoint.y) + mapHeight / 2;
        calcNewPoint(targetPoint, heading);

        return targetPoint.round();
    }

    /**
     * 根据航向角计算旋转后的坐标
     * @param targetPoint
     * @param rHeading
     * @return
     */
    private Point calcHeading(Point targetPoint, double rHeading) {
        double d = (targetPoint.y - mapHeight / 2) / (targetPoint.x - mapWidth / 2);//原始坐标斜率
        double oHeading = Math.abs(Math.atan(d) * 180.0 / Math.PI);//斜率转角度
        //以地图容器中心点为圆心、原始点为圆上点 计算出半径
        double r = Math.sqrt(Math.pow((targetPoint.x - mapWidth / 2), 2) + Math.pow((targetPoint.y - mapHeight / 2), 2));

        //计算旋转后的坐标 公式：x = 中心点x + r * cos(a) , y = 中心点y - r * sin(a)
        targetPoint.x =  mapWidth/2 + Math.cos(Math.toRadians(oHeading + rHeading)) * r ;
        targetPoint.y =  mapHeight/2 - Math.sin(Math.toRadians(oHeading + rHeading)) * r;

        return targetPoint;
    }

    private  Point calcNewPoint(Point p, float angle) {
        // calc arc
        float l = (float) ((angle * Math.PI) / 180);

        //sin/cos value
        float cosv = (float) Math.cos(l);
        float sinv = (float) Math.sin(l);

        // calc new point
        float newX = (float) ((p.x - mapWidth/2) * cosv - (p.y - mapHeight/2) * sinv + mapWidth/2);
        float newY = (float) ((p.x - mapWidth/2) * sinv + (p.y - mapHeight/2) * cosv + mapHeight/2);

        //update value
        p.x = newX;
        p.y = newY;
        return p;
    }

    /**
     * 像素坐标转经纬度
     *
     * @param point
     * @return
     */
    public LatLng getLatLng(Point point) {
        double d = 180 / Math.PI;
        unPixel(subtractCenterPoint(point));
        return new LatLng((2 * Math.atan(Math.exp(point.y / R)) - Math.PI / 2) * d, point.x * d / R);
    }

    /**
     * 减去中心点坐标
     *
     * @param point
     * @return
     */
    public Point subtractCenterPoint(Point point) {
        point.x = point.x - mapWidth / 2 + centerPoint.x;
        point.y = point.y - mapHeight / 2 + centerPoint.y;
        return point;
    }

    /**
     * 转像素转为墨卡托坐标
     *
     * @param point
     * @return
     */
    private Point unPixel(Point point) {
        point.x = (point.x / (allTileSize) - 0.5) / InvertEarthP;
        point.y = (point.y / (allTileSize) - 0.5) / -InvertEarthP;
        return point;
    }

}

