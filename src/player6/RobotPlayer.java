package player6;

import battlecode.common.*;
import java.util.Random;


public strictfp class RobotPlayer {

    static int turnCount = 0;

    static final Random rng = new Random();

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static MapLocation exploreTarget;

    public static MapLocation reflectedLocation(MapLocation original, int mapWidth, int mapHeight) {
        int newX = mapWidth - original.x;
        int newY = mapHeight - original.y;
        int translateX = newX - original.x;
        int translateY = newY - original.y;
        MapLocation reflectedLocation = original.translate(translateX, translateY);
        return reflectedLocation;
    }

    public static void populateEnemyStartingPosition(RobotController rc,MapLocation original, int mapWidth, int mapHeight) throws GameActionException {
        for (int i = 0; i < 4; i++) {
            if (rc.readSharedArray(i) == 0) {
                int bitvectorCoord = 0;
                int xBit = mapWidth - original.x;
                int yBit = mapHeight - original.y;
                bitvectorCoord += xBit << 6;
                bitvectorCoord += yBit;
                try {
                    rc.writeSharedArray(i, bitvectorCoord);
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }
    }

    // * Note:
    // This implementation uses our shared array as an array of bitvectors encoding
    // map locations. Each integer in our 64 integer array is structured as follows:
    //
    // |   1 bit   |   3 bits  | 6 bits | 6 bits | -> 16 bits total.
    // | alive bit | unit_type | xCoord | yCoord |

    public static void addLocationToSharedArray(RobotController rc, MapLocation coordinates, String unitType) throws GameActionException {
        int unit_integer = 0;
        switch (unitType) {
            case "archon":
                unit_integer = 0;
                break;
            case "soldier":
                unit_integer = 1;
                break;
            case "sage":
                unit_integer = 3;
                break;
            case "lead":
                unit_integer = 4;
                break;
            case "placeholder1": // We get 3 bits so we can encode 6 values
                unit_integer = 5;
                break;
            case "placeholder2": // We get 3 bits so we can encode 6 values
                unit_integer = 6;
                break;
            default:
                break;
        }
        for (int i = 0; i < 64; i++) {
            if (rc.readSharedArray(i) == 0) { // TODO: Change to read array if alive
                int uint16 = 0;
                int xBit = 0;
                int yBit = 0;
                int unitBit = unit_integer;
                int aliveBit = 1;
                uint16 |= yBit;
                uint16 |= xBit << 6;
                uint16 |= unitBit << 12;
                uint16 |= aliveBit << 15;
                try {
                    rc.writeSharedArray(i, uint16);
                } catch (Exception e) {
                    //TODO: handle exception
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        int mapWidth = rc.getMapWidth();
        int mapHeight = rc.getMapHeight();
        MapLocation startingLocation = rc.getLocation();

        // By default we will explore toward the reflected position, but we
        // can change this.
        exploreTarget = reflectedLocation(startingLocation, mapWidth, mapHeight);

        if (rc.getType() == RobotType.ARCHON) {
            for (int i = 0; i < 4; i++) {
                if (rc.readSharedArray(i) == 0) {
                    int bitvectorCoord = 0;
                    int xBit = mapWidth - startingLocation.x;
                    int yBit = mapHeight - startingLocation.y;
                }
            }
        }
    }
}
