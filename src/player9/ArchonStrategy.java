package player9;


import battlecode.common.*;


strictfp class ArchonStrategy {
    static int[] sharedArray = new int[64];
    static int radiusSquared = RobotType.ARCHON.visionRadiusSquared;

    // Bytecodes:
    // Builds a unit in target direction. If target direction is CENTER, picks dir randomly.
    static void buildUnit(RobotController rc, RobotType type, Direction dir) throws GameActionException {
        if (dir.equals(Direction.CENTER)) {
            int dirIndex = RobotPlayer.rng.nextInt(RobotPlayer.directions.length); 
            dir = RobotPlayer.directions[dirIndex];
        }
        
        Direction oldDir = dir;
        int numDirections = 8;
        for (int i = 0; i < numDirections; i++) {
            if (rc.canBuildRobot(type, dir)) {
                rc.buildRobot(type, dir);
                break;
            } else {
                dir = dir.rotateLeft();
                if (dir.equals(oldDir)) {
                    break;
                }
            }
        }
    }

    static MapLocation reflectedLocation(MapLocation original, int mapWidth, int mapHeight) {
        int newX = (mapWidth - 1) - original.x;
        int newY = (mapHeight - 1) - original.y;
        int translateX = newX - original.x;
        int translateY = newY - original.y;
        MapLocation reflectedLocation = original.translate(translateX, translateY);
        return reflectedLocation;
    }

    static MapLocation rotatedLocation(MapLocation original, int mapWidth, int mapHeight) {
        int newX = (mapWidth - 1) - original.x;
        int translateX = newX - original.x;
        MapLocation reflectedLocation = original.translate(translateX, 0);
        return reflectedLocation;
    }

    // index 1 will be for defense location, 0 will be for offense location.
    static void setDefendLocation(RobotController rc, MapLocation loc, int id) throws GameActionException {
        int bitvector = rc.readSharedArray(1);
        int lastEditor = (bitvector & (0x3 << 12)) >> 12;
        int xCoord = (bitvector & (0x3f << 6)) >> 6;
        int yCoord = (bitvector & (0x3f));
        if (((lastEditor == id) && (yCoord != 0 && xCoord != 0)) || ((yCoord == 0) && (xCoord == 0))) {
            lastEditor = id;
            xCoord = loc.x;
            yCoord = loc.y;
            bitvector = 0;
            bitvector += loc.y;
            bitvector += loc.x << 6;
            bitvector += lastEditor << 12;
            rc.writeSharedArray(1, bitvector);
        }
    }

    static void resetDefendLocation(RobotController rc, int id) throws GameActionException {
        int bitvector = rc.readSharedArray(1);
        int lastEditor = (bitvector & (0x3 << 12)) >> 12;
        int xCoord = (bitvector & (0x3f << 6)) >> 6;
        int yCoord = (bitvector & 0x3f);
        if ((lastEditor == id && (yCoord != 0 && xCoord != 0)) || (yCoord == 0 && xCoord == 0)) {
            rc.writeSharedArray(1, 0);
        }
    }

    static void run(RobotController rc) throws GameActionException {
        
        int id = (rc.getID() - 1) / 2;
        int round = rc.getRoundNum();
        // rc.setIndicatorString("" + rc.readSharedArray(1));
        MapLocation me = rc.getLocation();

        // We want to reset the defend position in shared array occasionally in
        // case our archon dies we don't want it locked.
        if (round % 10 == 0) {
            rc.writeSharedArray(1, 0);
        }

        // First round opening strat - make target reflected/rotated self location and
        // build a miner if archon can see any lead.
        if (round == 1) {
            MapLocation firstTarget = reflectedLocation(me, rc.getMapWidth(), rc.getMapHeight());
            int bitvector = 0;
            bitvector += firstTarget.y;
            bitvector += firstTarget.x << 6;
            int existingLocation = rc.readSharedArray(0);
            if (existingLocation == 0) {
                rc.writeSharedArray(0, bitvector);
            } else {
                int x = (existingLocation & (63 << 6)) >> 6;
                int y = (existingLocation & 63);
                if (x == me.x && y == me.y) {
                    firstTarget = rotatedLocation(me, rc.getMapWidth(), rc.getMapHeight());
                    bitvector = 0;
                    bitvector += firstTarget.y;
                    bitvector += firstTarget.x << 6;
                    rc.writeSharedArray(0, bitvector);
                }
            }

            // RobotPlayer.addLocationToSharedArray(rc, a, 1, 1);

            MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(me, radiusSquared);

            for (MapLocation loc : locs) {
                if (rc.canSenseLocation(loc) && rc.senseLead(loc) > 0) {
                    buildUnit(rc, RobotType.MINER, me.directionTo(loc));
                    break;
                }
            }
        } else if (round < 30) {
            buildUnit(rc, RobotType.MINER, Direction.CENTER);
        } else {
            // buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);
            int n = RobotPlayer.rng.nextInt(4);
            if (rc.getTeamLeadAmount(rc.getTeam()) >= 75) {
                if (n < 2) {
                    buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);
                } else if (n > 2) {
                    buildUnit(rc, RobotType.MINER, Direction.CENTER);
                // } else if (n == 2) {
                //     buildUnit(rc, RobotType.SAGE, Direction.CENTER);
                // } else if (n == 3) {
                //     buildUnit(rc, RobotType.BUILDER, Direction.CENTER);
                }
            }
        }

        int start = Clock.getBytecodeNum();
        // if (rc.readSharedArray(2) == 0) {

        for (int i = 2; i < 64; i++) {
            rc.writeSharedArray(i, 0);
        }

        int end = Clock.getBytecodeNum();
        rc.setIndicatorString("" + (end - start));
        

        RobotInfo[] enemyLocs = rc.senseNearbyRobots(radiusSquared, RobotPlayer.opponent);

        if (enemyLocs.length > 0) {
            MapLocation enemyLocation = enemyLocs[0].location;
            setDefendLocation(rc, enemyLocation, id);
        } else {
            resetDefendLocation(rc, id);
        }
    }
}
