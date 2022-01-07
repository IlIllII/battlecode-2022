package player12;

import battlecode.common.*;

import java.util.Random;

class SharedArrayTargetAndIndex {
    MapLocation location;
    int idx;

    SharedArrayTargetAndIndex(MapLocation target, int index) {
        location = target;
        idx = index;
    }
}

class TripleTarget {
    MapLocation primary;
    MapLocation secondary;
    MapLocation tertiary;

    TripleTarget(MapLocation primaryTarget, MapLocation secondaryTarget, MapLocation tertiaryTarget) {
        primary = primaryTarget;
        secondary = secondaryTarget;
        tertiary = tertiaryTarget;
    }
}

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
    static final int SHARED_ARRAY_ENEMY_START_INDEX = 2;
    static int mapWidth;
    static int mapHeight;
    static Team opponent;
    static boolean rotateLeft = rng.nextBoolean();
    static MapLocation backupTarget;
    static int actionRadiusSquared;

    static TripleTarget acquireLocalTargets(RobotController rc, MapLocation globalTarget, RobotInfo[] enemies,
            MapLocation me) throws GameActionException {
        int lowestHPDangerousEnemy = 10000;
        int lowestHPBenignEnemy = 10000;
        MapLocation primaryTarget = globalTarget;
        MapLocation secondaryTarget = globalTarget;
        MapLocation tertiaryTarget = globalTarget;
        for (RobotInfo enemy : enemies) {
            if (enemy.type.equals(RobotType.ARCHON)) {
                RobotPlayer.addLocationToSharedArray(rc, enemy.location, 0, 0);
            }
            int distanceSquaredToEnemy = me.distanceSquaredTo(enemy.location);
            if (distanceSquaredToEnemy <= actionRadiusSquared && (enemy.type.equals(RobotType.SOLDIER)
                    || enemy.type.equals(RobotType.SAGE) || enemy.type.equals(RobotType.WATCHTOWER))) {
                if (enemy.health < lowestHPDangerousEnemy) {
                    primaryTarget = enemy.location;
                    lowestHPDangerousEnemy = enemy.health;
                }
            } else if (distanceSquaredToEnemy <= actionRadiusSquared) {
                if (enemy.health < lowestHPBenignEnemy) {
                    secondaryTarget = enemy.location;
                    lowestHPBenignEnemy = enemy.health;
                }
            } else {
                tertiaryTarget = enemy.location;
            }
        }

        return new TripleTarget(primaryTarget, secondaryTarget, tertiaryTarget);
    }

    /**
     * Find a potential target from the shared array.
     * 
     * `locateATarget` finds the closest enemy, defensive location, or offensive
     * location from the shared array. If none of these locations are available,
     * defaults to a given backup location `backupTarget`.
     * 
     * Additionally, it will see if each location is within view and if it is still
     * valid. If not, it will clear it from the shared array/reset the backup
     * location.
     * 
     * @param rc
     * @param backupTarget
     * @param me           - position of self. Is passing this cheaper than
     *                     rc.getLocation()?
     * @return
     * @throws GameActionException
     */
    static SharedArrayTargetAndIndex locateCombatTarget(RobotController rc, MapLocation me) throws GameActionException {

        boolean usingOffensiveTarget = false;
        MapLocation target = getDefensiveTarget(rc);
        if (RobotPlayer.targetDoesntExist(target)) {
            target = getOffensiveTarget(rc);
            usingOffensiveTarget = true;
        }

        if (usingOffensiveTarget && rc.canSenseLocation(target)) {
            if (rc.canSenseRobotAtLocation(target)) {
                if (rc.senseRobotAtLocation(target).type != RobotType.ARCHON) {
                    rc.writeSharedArray(0, 0);
                    target = new MapLocation(0, 0);
                }
            } else {
                rc.writeSharedArray(0, 0);
                target = new MapLocation(0, 0);
            }
        }

        if (RobotPlayer.targetDoesntExist(target)) {
            target = backupTarget;
            if (target != null && me.distanceSquaredTo(target) <= 4) {
                backupTarget = getRandomMapLocation();
                target = backupTarget;
            }
            usingOffensiveTarget = false;
        }

        int index = -1;
        for (int i = SHARED_ARRAY_ENEMY_START_INDEX; i < 64; i++) {
            int bitvector = rc.readSharedArray(i);
            if (bitvector == 0) {
                index = i;
                break;
            }
            MapLocation loc = decodeLocationFromBitvector(bitvector);
            if (me.distanceSquaredTo(loc) < me.distanceSquaredTo(target)) {
                target = loc;
            }
        }

        SharedArrayTargetAndIndex result = new SharedArrayTargetAndIndex(target, index);

        return result;
    }

    static void attackGlobalTargetIfAble(RobotController rc, MapLocation target, MapLocation me)
            throws GameActionException {
        if (me.distanceSquaredTo(target) <= actionRadiusSquared) {
            if (rc.canSenseLocation(target)) {
                if (rc.canSenseRobotAtLocation(target)) {
                    RobotInfo enemy = rc.senseRobotAtLocation(target);
                    if (enemy.type == RobotType.SOLDIER || enemy.type == RobotType.SAGE
                            || enemy.type == RobotType.WATCHTOWER) {
                        if (rc.canAttack(target)) {
                            rc.attack(target);
                        }
                    }
                }
            }
        }
    }

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

    static int encodeLocationToBitvector(MapLocation loc) {
        int x = loc.x;
        int y = loc.y;
        int bitvector = 0;
        bitvector += x << 6;
        bitvector += y;
        return bitvector;
    }

    // Bytecodes: 144
    public static void addLocationToSharedArray(RobotController rc, MapLocation coordinates, int unitCode, int idx)
            throws GameActionException {
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

                rubbleNumbers[0] = rc.canSenseLocation(rc.adjacentLocation(leftDir)) && rc.canMove(leftDir)
                        ? rc.senseRubble(rc.adjacentLocation(leftDir))
                        : 100;
                rubbleNumbers[1] = rc.canSenseLocation(rc.adjacentLocation(dir))
                        ? rc.senseRubble(rc.adjacentLocation(dir))
                        : 100;
                rubbleNumbers[2] = rc.canSenseLocation(rc.adjacentLocation(rightDir)) && rc.canMove(rightDir)
                        ? rc.senseRubble(rc.adjacentLocation(rightDir))
                        : 100;

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

    static boolean isLandSuitableForBuilding(RobotController rc, MapLocation loc) {
        MapLocation north = loc.add(directions[0]);
        MapLocation east = loc.add(directions[2]);
        MapLocation south = loc.add(directions[4]);
        MapLocation west = loc.add(directions[6]);

        boolean suitable;
        try {
            suitable = (!rc.canSenseRobotAtLocation(north)
                    || rc.senseRobotAtLocation(north).mode != RobotMode.TURRET
                            && rc.senseRobotAtLocation(north).mode != RobotMode.PROTOTYPE)
                    && (!rc.canSenseRobotAtLocation(east)
                            || rc.senseRobotAtLocation(east).mode != RobotMode.TURRET
                                    && rc.senseRobotAtLocation(east).mode != RobotMode.PROTOTYPE)
                    && (!rc.canSenseRobotAtLocation(south)
                            || rc.senseRobotAtLocation(south).mode != RobotMode.TURRET
                                    && rc.senseRobotAtLocation(south).mode != RobotMode.PROTOTYPE)
                    && (!rc.canSenseRobotAtLocation(west)
                            || rc.senseRobotAtLocation(west).mode != RobotMode.TURRET
                                    && rc.senseRobotAtLocation(west).mode != RobotMode.PROTOTYPE);
        } catch (GameActionException e) {
            suitable = false;
        }

        try {
            if (rc.canSenseLocation(loc) && rc.senseRubble(loc) > 10) {
                suitable = false;
            }
        } catch (GameActionException e) {
            e.printStackTrace();
        }

        return suitable;
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        opponent = rc.getTeam().opponent();
        actionRadiusSquared = rc.getType().actionRadiusSquared;
        backupTarget = getRandomMapLocation();

        while (true) {
            try {
                switch (rc.getType()) {
                    case ARCHON:
                        ArchonStrategy.run(rc);
                        break;
                    case MINER:
                        MinerStrategy.run(rc);
                        break;
                    case SOLDIER:
                        SoldierStrategy.run(rc);
                        break;
                    case LABORATORY:
                        LaboratoryStrategy.run(rc);
                        break;
                    case WATCHTOWER:
                        WatchtowerStrategy.run(rc);
                        break;
                    case BUILDER:
                        BuilderStrategy.run(rc);
                        break;
                    case SAGE:
                        SageStrategy.run(rc);
                        break;
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
                rc.resign();
            } finally {
                Clock.yield();
            }
        }
    }
}
