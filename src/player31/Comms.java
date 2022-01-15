package player31;

import battlecode.common.*;

public class Comms {

    /*
    New idea is to use the shared array as follows:

    Indices:
        0: Message
        1: Offensive target
        2-5: allied archon locations
        6-7: allied unit counts
        8-63: enemy locations
    */
    static final int LENGTH_OF_SHARED_ARRAY = 64;
    static final int MESSAGE_INDEX = 0;
    static final int OFFENSIVE_TARGET_INDEX = 1;
    static final int ARCHON_INDEX = 2;
    static final int UNIT_COUNT_INDEX = 6;
    static final int ENEMY_TARGETS_INDEX = 10;
    static final int ENEMY_ARRAY_CAPACITY = 53;

    static int[] enemyIntegerArray = new int[54];
    static int enemyArrayInsertionIndex = 0;


    /**
     * Bytecode cost: 123
     */
    static void setOffensiveLocation(RobotController rc, MapLocation location) throws GameActionException {
        int bitvector = 0;
        int x = location.x;
        int y = location.y;
        bitvector |= (x << 6) | y;
        rc.writeSharedArray(OFFENSIVE_TARGET_INDEX, bitvector);
    }

    /**
     * Bytecode cost: 38
     */
    static EnemyLocation getOffensiveLocation(RobotController rc) throws GameActionException {
        int bitvector = rc.readSharedArray(OFFENSIVE_TARGET_INDEX);
        MapLocation location = decodeMapLocationFromBitvector(bitvector);
        boolean exists = bitvector != 0;
        return new EnemyLocation(location, exists, OFFENSIVE_TARGET_INDEX);
    }

    /**
     * Bytecode cost: 106
     */
    static void clearOffensiveLocation(RobotController rc) throws GameActionException {
        rc.writeSharedArray(OFFENSIVE_TARGET_INDEX, 0);
    }


    /**
     * Bytecode cost: 135
     */
    static void setArchonLocation (RobotController rc, MapLocation archonLocation, boolean shouldDefend, int archonID) throws GameActionException {
        int bitvector = 0;
        int x = archonLocation.x;
        int y = archonLocation.y;
        int defendBit = shouldDefend ? 1 : 0;
        bitvector |= (x << 6) | y | (defendBit << 14);
        rc.writeSharedArray(ARCHON_INDEX + archonID, bitvector);
    }

    /**
     * Bytecode cost: 211
     * 
     * @param rc
     * @return
     * @throws GameActionException
     */
    static ArchonLocation[] getArchonLocations(RobotController rc) throws GameActionException {
        int bitvector1 = rc.readSharedArray(ARCHON_INDEX);
        int bitvector2 = rc.readSharedArray(ARCHON_INDEX + 1);
        int bitvector3 = rc.readSharedArray(ARCHON_INDEX + 2);
        int bitvector4 = rc.readSharedArray(ARCHON_INDEX + 3);

        MapLocation loc1 = decodeMapLocationFromBitvector(bitvector1);
        MapLocation loc2 = decodeMapLocationFromBitvector(bitvector2);
        MapLocation loc3 = decodeMapLocationFromBitvector(bitvector3);
        MapLocation loc4 = decodeMapLocationFromBitvector(bitvector4);

        boolean shouldDefend1 = ((bitvector1 & (1 << 14)) >> 14) == 1;
        boolean shouldDefend2 = ((bitvector2 & (1 << 14)) >> 14) == 1;
        boolean shouldDefend3 = ((bitvector3 & (1 << 14)) >> 14) == 1;
        boolean shouldDefend4 = ((bitvector4 & (1 << 14)) >> 14) == 1;

        boolean exists1 = bitvector1 != 0;
        boolean exists2 = bitvector2 != 0;
        boolean exists3 = bitvector3 != 0;
        boolean exists4 = bitvector4 != 0;

        ArchonLocation archonLocation1 = new ArchonLocation(loc1, shouldDefend1, exists1);
        ArchonLocation archonLocation2 = new ArchonLocation(loc2, shouldDefend2, exists2);
        ArchonLocation archonLocation3 = new ArchonLocation(loc3, shouldDefend3, exists3);
        ArchonLocation archonLocation4 = new ArchonLocation(loc4, shouldDefend4, exists4);

        ArchonLocation[] result = { archonLocation1, archonLocation2, archonLocation3, archonLocation4 };

        return result;
    }

    static MapLocation getNearestArchonLocation(RobotController rc, MapLocation me) throws GameActionException {
        ArchonLocation[] archonLocations = Comms.getArchonLocations(rc);
        MapLocation retreatTarget = null;
        int minDistance = 1000000000;

        for (ArchonLocation archonLocation : archonLocations) {
            if (archonLocation.exists && (retreatTarget == null || me.distanceSquaredTo(archonLocation.location) < minDistance)) {
                retreatTarget = archonLocation.location;
                minDistance = me.distanceSquaredTo(archonLocation.location);
            }
        }
        
        return retreatTarget;
    }

    /**
     * Bytecode cost: 415
     * 
     * @param rc
     * @throws GameActionException
     */
    static void clearArchonLocations(RobotController rc) throws GameActionException {
        rc.writeSharedArray(ARCHON_INDEX, 0);
        rc.writeSharedArray(ARCHON_INDEX + 1, 0);
        rc.writeSharedArray(ARCHON_INDEX + 2, 0);
        rc.writeSharedArray(ARCHON_INDEX + 3, 0);
    }



    static void getMessage() {

    }

    static void setMessage() {

    }

    static void clearMessage() {

    }


    static void getUnitCounts() {

    }

    static void getUnitCount() {

    }

    static void setUnitCount() {

    }

    static void clearUnitCounts() {

    }



    static EnemyLocation[] getEnemyLocations(RobotController rc) throws GameActionException {
        EnemyLocation[] enemyLocations = new EnemyLocation[54];

        for (int i = 0; i < ENEMY_ARRAY_CAPACITY; i++) {
            int bitvector = rc.readSharedArray(ENEMY_TARGETS_INDEX + i);

            if (bitvector == 0) {
                enemyLocations[i] = new EnemyLocation(decodeMapLocationFromBitvector(bitvector), false, i);
            } else {
                enemyLocations[i] = new EnemyLocation(decodeMapLocationFromBitvector(bitvector), true, i);
                // System.out.println(enemyLocations[i].location.toString());
            }
            // if (enemyLocations[i] != null) {
            //     System.out.println(enemyLocations[i].location.toString());
            // }
            // if (i == ENEMY_ARRAY_CAPACITY - 1) {
            //     return enemyLocations;
            // }
        }


        return enemyLocations;
    }


    static void setEnemyLocation(RobotController rc, MapLocation location, EnemyLocation[] enemyLocations) throws GameActionException {
        int bitvector = encodeMapLocationIntoBitvector(location);
        bitvector |= (1 << 14);

        for (int i = 0; i < enemyLocations.length; i++) {
            if (enemyLocations[i] == null || !enemyLocations[i].exists) {
                rc.writeSharedArray(ENEMY_TARGETS_INDEX + i, bitvector);
                break;
            }
        }
    }

    static void clearEnemyLocation(RobotController rc, int index) throws GameActionException {
        rc.writeSharedArray(index, 0);
    }

    static void clearEnemyLocations(RobotController rc) throws GameActionException {
        for (int i = ENEMY_TARGETS_INDEX; i < 64; i++) {
            rc.writeSharedArray(i, 0);
        }
    }

    static MapLocation decodeMapLocationFromBitvector(int bitvector) {
        return new MapLocation((bitvector & (63 << 6)) >> 6, bitvector & 63);
    }

    static int encodeMapLocationIntoBitvector(MapLocation loc) {
        int bitvector = 0;
        int x = loc.x;
        int y = loc.y;
        bitvector |= (x << 6) | y;
        return bitvector;
    }

}

class ArchonLocation {
    public MapLocation location;
    public boolean shouldDefend;
    public boolean exists;

    public ArchonLocation(MapLocation location, boolean shouldDefend, boolean exists) {
        this.location = location;
        this.shouldDefend = shouldDefend;
        this.exists = exists;
    }
}

class EnemyLocation {
    public MapLocation location;
    public boolean exists;
    public int index;

    public EnemyLocation(MapLocation location, boolean exists, int index) {
        this.location = location;
        this.exists = exists;
        this.index = index;
    }
}