package com.shengdan.map_convert;

/**
 * 经纬度转平面地图像素坐标
 */
public class MapConvert {

    public final static double R = 6378137;//地球半径
    public final static double EarthP = 2 * Math.PI * R;//地球赤道周长
    public final static double MAX_LATITUDE = 85.0511287798;
    public final static int singleTileSize = 256;//单个瓦片长度

    private int mapWidth;
    private int mapHeight;
    private LatLng centerLatLng;
    private Point centerPoint;
    private int zoom;
    private float density = 1.0f;
    //当zoom为0时，只有一张瓦片也就是256像素,计算公式:256 * Math.pow(2, zoom)
    private double allTileSize;


    public MapConvert(int mapWidth, int mapHeight, int zoom, LatLng centerLatLng, float density) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.centerLatLng = centerLatLng;
        this.zoom = zoom;
        this.density = density;

        this.allTileSize = (singleTileSize * Math.pow(2, zoom));
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
        this.zoom = zoom;
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
        point.x = density * allTileSize * (point.x * (1 / EarthP) + 0.5);
        point.y = density * allTileSize * (point.y * -(1 / EarthP) + 0.5);
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
    public Point getPoint(LatLng latLng) {
        Point targetPoint = toPixel(toMercator(latLng));
        targetPoint.x = (targetPoint.x - centerPoint.x) + mapWidth / 2;
        targetPoint.y = (targetPoint.y - centerPoint.y) + mapHeight / 2;
        return targetPoint.round();
    }

    /**
     * 像素坐标转经纬度
     * @param point
     * @return
     */
    public LatLng getLatLng(Point point) {
        double d = 180 / Math.PI;
        unPixel(subtractCenterPoint(point));
        return new LatLng((2 * Math.atan(Math.exp(point.y / R)) - Math.PI / 2) * d,point.x * d / R);
    }

    /**
     * 减去中心点坐标
     * @param point
     * @return
     */
    public Point subtractCenterPoint(Point point){
        point.x = point.x - mapWidth / 2 + centerPoint.x;
        point.y = point.y - mapHeight / 2 + centerPoint.y;
        return point;
    }

    /**
     * 转像素转为墨卡托坐标
     * @param point
     * @return
     */
    private Point unPixel(Point point) {
        point.x = (point.x / (density * allTileSize) - 0.5) / (1 / EarthP);
        point.y = (point.y / (density * allTileSize) - 0.5) / - (1 / EarthP);
        return point;
    }

}
