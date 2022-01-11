package player20;


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

    static void executeBuildStrategy(RobotController rc, MapLocation me, int round, int leadAmount, int archonCount, int roundCutoff) throws GameActionException {

        int randomInteger = RobotPlayer.rng.nextInt(100);

        if (round == 1) { // Set targets and build a miner if we see lead
            MapLocation firstTarget = reflectedLocation(me, rc.getMapWidth(), rc.getMapHeight());
            int bitvector = 0;
            bitvector += firstTarget.y;
            bitvector += firstTarget.x << 6;
            
            EnemyLocation enemyArch = Comms.getOffensiveLocation(rc);

            if (!enemyArch.exists) {
                Comms.setOffensiveLocation(rc, firstTarget);
            } else {
                if (enemyArch.location.equals(me)) {
                    firstTarget = rotatedLocation(me, rc.getMapWidth(), rc.getMapHeight());
                    Comms.setOffensiveLocation(rc, firstTarget);
                }
            }

            MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(me, radiusSquared);

            for (MapLocation loc : locs) {
                if (rc.canSenseLocation(loc) && rc.senseLead(loc) > 0) {
                    buildUnit(rc, RobotType.MINER, me.directionTo(loc));
                    break;
                }
            }
        } else if (rc.getTeamGoldAmount(rc.getTeam()) > 50) {
            buildUnit(rc, RobotType.SAGE, Direction.CENTER);
        } else if (round < roundCutoff) {
            if (randomInteger < (100 / archonCount)) {
                buildUnit(rc, RobotType.MINER, Direction.CENTER);
            }
            if (leadAmount > 200) {
                buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);
            }
            

        } else if (round < 50) {
            if (randomInteger < (100 / archonCount) / 8) {
                buildUnit(rc, RobotType.MINER, Direction.CENTER);
            }
            if (leadAmount > 100) {
                buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);
            }
            buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);
        } else {
            if (leadAmount > 500) {
                buildUnit(rc, RobotType.BUILDER, Direction.CENTER);
            }
            int n = RobotPlayer.rng.nextInt(100);
            if (randomInteger < 25 || leadAmount >= 150) {
                if (n < 75) {
                    buildUnit(rc, RobotType.SOLDIER, Direction.CENTER);
                } else if (n < 98) {
                    buildUnit(rc, RobotType.MINER, Direction.CENTER);
                } else {
                    buildUnit(rc, RobotType.BUILDER, Direction.CENTER);
                }
            }
            
        }
    }

    static boolean goingTowardMainArchon = true;
    static int transformTimer = 100;
    static int movingCounter = 0;
    static int repairCount = 0;

    static void run(RobotController rc) throws GameActionException {

        transformTimer++;
        
        int archonCount = rc.getArchonCount();
        int roundCutoff = 30;
        if (archonCount <= 2) {
            roundCutoff = 20;
        }

        MapLocation target = new MapLocation(1000, 1000);


        int rcId = rc.getID();
        int id = rcId % 2 == 0 ? (rcId - 1) / 2 : (rcId - 2) / 2;
        int round = rc.getRoundNum();
        int leadAmount = rc.getTeamLeadAmount(rc.getTeam());
        MapLocation me = rc.getLocation();
        boolean movingAndFighting = false;
        
        RobotInfo[] enemiesInRange = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] alliedLocs = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());

        boolean dangerClose = false;

        for (RobotInfo enemy : enemiesInRange) {
            if (enemy.type.equals(RobotType.SOLDIER) || enemy.type.equals(RobotType.SAGE) || enemy.type.equals(RobotType.WATCHTOWER) || enemy.type.equals(RobotType.MINER)) {
                dangerClose = true;
            }
        }

        if (round % 10 == 0) {
            Comms.clearArchonLocations(rc);
        }


        if (round < 30) {
            executeBuildStrategy(rc, me, round, leadAmount, archonCount, roundCutoff);
        } else if (id == 0) {

            // clear shared buffer
            for (int i = 2; i < 64; i++) {
                rc.writeSharedArray(i, 0);
            }
            movingAndFighting = false;
            executeBuildStrategy(rc, me, round, leadAmount, archonCount, roundCutoff);
        } else {
            executeBuildStrategy(rc, me, round, leadAmount, archonCount, roundCutoff);
        }


        // Repair before build to save lead in long run.

        for (RobotInfo ally : alliedLocs) {
            if (!rc.isActionReady()) {
                break;
            }
            if (ally.health < ally.type.health) {
                if (rc.canRepair(ally.location)) {
                    rc.repair(ally.location);
                    repairCount++;
                }
            }
        }

        if (movingAndFighting) {


            EnemyLocation oTarget = Comms.getOffensiveLocation(rc);
            MapLocation[] targets = Comms.getEnemyLocations(rc);

            for (MapLocation loc : targets) {
                if (me.distanceSquaredTo(loc) < me.distanceSquaredTo(target)) {
                    target = loc;
                }
            }

            if (oTarget.exists && me.distanceSquaredTo(oTarget.location) < me.distanceSquaredTo(target)) {
                target = oTarget.location;
            }
            

            int distanceToTarget = (int)Math.sqrt(me.distanceSquaredTo(target));
            int visionDistance = (int)Math.sqrt(rc.getType().visionRadiusSquared);

            if (rc.getMode() == RobotMode.TURRET) {
                if (rc.canTransform()) {
                    if (distanceToTarget > visionDistance + 10 && !dangerClose && transformTimer > 100) {
                        rc.transform();
                        transformTimer = 0;
                    }
                } else {
                    if (rc.isActionReady()) {
                        executeBuildStrategy(rc, me, round, leadAmount, archonCount, roundCutoff);
                    }
                }
            }

            if (rc.getMode() == RobotMode.PORTABLE) {
                movingCounter++;

                if (goingTowardMainArchon) {
                    target = Comms.getArchonLocations(rc)[0].location;

                    if (me.distanceSquaredTo(target) <= 25) {
                        goingTowardMainArchon = false;
                    }
                }

                if (round >= 100 && (distanceToTarget < visionDistance || dangerClose || movingCounter > 30) && !goingTowardMainArchon) {
                    if (rc.canTransform() && rc.senseRubble(me) < 10) {
                        rc.transform();
                        movingCounter = 0;
                    } else { // flee
                        RobotPlayer.move2(rc, rc.adjacentLocation(me.directionTo(target).opposite()), 1);
                    }
                } else {
                    RobotPlayer.move2(rc, target, 4);
                }
            }
        } else {
            if (rc.getMode() == RobotMode.PORTABLE) {
                if (rc.canTransform()) {
                    rc.transform();
                }
            }
        }

        // Differential strategies based on round number

        Team tm = rc.getTeam();
        int leadAmt = rc.getTeamLeadAmount(tm);

        // if (rc.readSharedArray(2) == 0) {
            
        
        

        RobotInfo[] enemyLocs = rc.senseNearbyRobots(radiusSquared, RobotPlayer.opponent);

        if (enemyLocs.length > 0) {
            // MapLocation enemyLocation = enemyLocs[0].location;
            // setDefendLocation(rc, enemyLocation, id);

            Comms.setArchonLocation(rc, me, true, id);

        } else {
            Comms.setArchonLocation(rc, me, false, id);
        }

        // int start = Clock.getBytecodeNum();
        // EnemyLocation target = Comms.getOffensiveLocation(rc);
        // int end = Clock.getBytecodeNum();
        // rc.setIndicatorString("set loc: " + (end - start));

        rc.setIndicatorLine(me, target, 100, 100, 100);
        rc.setIndicatorString("Repairs: " + repairCount);
    }
}
