package player11;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;


public strictfp class RobotPlayer {
    static int turnCount = 0;
    static final Random rng = new Random();
    static final Direction[] directions = Direction.allDirections();
    static final Direction[] cardinalDirections = Direction.cardinalDirections();
    static MapLocation globalTarget;
    static MapLocation localTarget;
    static final int SHARED_ARRAY_SOLDIER_CODE = 0;
    static final int SHARED_ARRAY_SAGE_CODE = 1;
    static final int SHARED_ARRAY_ALIVE_CODE = 1;
    static final int SHARED_ARRAY_DEAD_CODE = 0;
    static int mapWidth;
    static int mapHeight;
    static Team opponent;
    static boolean rotateLeft = rng.nextBoolean();

    static MapLocation getRandomMapLocation() {
        return new MapLocation(rng.nextInt(mapWidth - 1), rng.nextInt(mapHeight - 1));
    }

    static MapLocation getOffensiveTarget(RobotController rc) throws GameActionException {
        int bitvector = rc.readSharedArray(0);
        return decodeLocationFromBitvector(bitvector);
    }

    static boolean targetDoesntExist(MapLocation loc) {
        return loc.x == 0 && loc.y == 0;
    }

    static MapLocation getDefensiveTarget(RobotController rc) throws GameActionException {
        int bitvector = rc.readSharedArray(1);
        return decodeLocationFromBitvector(bitvector);
    }

    static MapLocation decodeLocationFromBitvector(int bitvector) {
        int x = (bitvector & (63 << 6)) >> 6;
        int y = (63 & bitvector);
        return new MapLocation(x, y);
    }

    // Bytecodes: 144
    public static void addLocationToSharedArray(RobotController rc, MapLocation coordinates, int unitCode, int idx) throws GameActionException {
        int uint16 = 0;
        int unitBit = unitCode;
        int xBit = coordinates.x;
        int yBit = coordinates.y;
        int aliveBit = 1;
        uint16 += yBit;
        uint16 += xBit << 6;
        uint16 += unitBit << 14;
        uint16 += aliveBit << 15;
        try {
            rc.writeSharedArray(idx, uint16);
        } catch (Exception e) {
            System.out.println(rc.getType() + " Exception");
            e.printStackTrace();
        }
    }

    // Bytecodes: 844
    // Returns full shared array.
    public static int[] getSharedArray(RobotController rc) throws GameActionException {
        int[] sharedArray = new int[64];
        for (int i = 0; i < 64; i++) {
            sharedArray[i] = rc.readSharedArray(i);
        }
        return sharedArray;
    }

    public static MapLocation[] getTargetList(RobotController rc) throws GameActionException {
        MapLocation[] sharedArray = new MapLocation[62];
        int start = 2;
        int end = 64;
        for (int i = start; i < end; i++) {
            int bitvector = rc.readSharedArray(i);
            if (bitvector == 0) {
                end = i;
                break;
            }
            MapLocation loc = decodeLocationFromBitvector(bitvector);
            sharedArray[i] = loc;
        }
        MapLocation[] slice = new MapLocation[end - start];
        for (int i = 0; i < slice.length; i++) {
            slice[i] = sharedArray[i];
        }
        return slice;
    }

    public static void move(RobotController rc, MapLocation target) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        Direction dir = myLoc.directionTo(target);
        Direction oldDir = dir;
        int numDirections = 8;
        for (int i = 0; i < numDirections; i++) {
            if (rc.canMove(dir)) {
                int[] rubbleNumbers = new int[3];

                Direction leftDir = dir.rotateLeft();
                Direction rightDir = dir.rotateRight();

                rubbleNumbers[0] = rc.canSenseLocation(rc.adjacentLocation(leftDir)) && rc.canMove(leftDir) ? rc.senseRubble(rc.adjacentLocation(leftDir)) : 100;
                rubbleNumbers[1] = rc.canSenseLocation(rc.adjacentLocation(dir)) ? rc.senseRubble(rc.adjacentLocation(dir)) : 100;
                rubbleNumbers[2] = rc.canSenseLocation(rc.adjacentLocation(rightDir)) && rc.canMove(rightDir) ? rc.senseRubble(rc.adjacentLocation(rightDir)) : 100;
                
                int minValue = rubbleNumbers[1];
                int minIdx = 1;

                for (int j = 0; j < 3; j += 2) {
                    if (rubbleNumbers[j] < minValue) {
                        minValue = rubbleNumbers[j];
                        minIdx = j;
                    }
                }

                if (minIdx == 0) {
                    dir = dir.rotateLeft();
                } else if (minIdx == 2) {
                    dir = dir.rotateRight();
                }

                rc.move(dir);
                break;
            } else {
                if (rotateLeft) {
                    dir = dir.rotateLeft();
                } else {
                    dir = dir.rotateRight();
                }
                if (dir.equals(oldDir)) {
                    break;
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        opponent = rc.getTeam().opponent();
        
        while (true) {
            try {
                switch (rc.getType()) {
                    case ARCHON:     ArchonStrategy.run(rc);  break;
                    case MINER:      MinerStrategy.run(rc);   break;
                    case SOLDIER:    SoldierStrategy.run(rc); break;
                    case LABORATORY: LaboratoryStrategy.run(rc); break;
                    case WATCHTOWER: WatchtowerStrategy.run(rc); break;
                    case BUILDER:    BuilderStrategy.run(rc); break;
                    case SAGE:       SageStrategy.run(rc);    break;
                }
            } catch (GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (NullPointerException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
