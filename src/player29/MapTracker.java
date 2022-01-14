package player29;

import battlecode.common.MapLocation;

public class MapTracker {

    final static int MAX_MAP_SIZE = 60;
    final static int INT_BITS = 32;
    final static int ARRAY_SIZE = 120;

    static int[] visitedLocations = new int[ARRAY_SIZE];


    MapTracker(){
    }

    void reset(){
        visitedLocations = new int[ARRAY_SIZE];
    }

    void add(MapLocation loc){
        int arrayPos = (loc.x%MAX_MAP_SIZE)*(1 + (loc.y%MAX_MAP_SIZE)/INT_BITS);
        int bitPos = loc.y%INT_BITS;
        visitedLocations[arrayPos] |= (1 << bitPos);
    }

    boolean check(MapLocation loc){
        int arrayPos = (loc.x%MAX_MAP_SIZE)*(1 + (loc.y%MAX_MAP_SIZE)/INT_BITS);
        int bitPos = loc.y%INT_BITS;
        return ((visitedLocations[arrayPos] & (1 << bitPos)) > 0);
    }

}