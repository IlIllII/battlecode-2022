package player25;

import battlecode.common.*;


strictfp class MinerStrategy {
    static MapLocation globalTarget;

    static MapLocation earlyGameTarget(RobotController rc, int mapWidth, int mapHeight) {
        System.out.println("Width: " + mapWidth + ", Height:" + mapHeight);


        MapLocation randLoc = RobotPlayer.getRandomMapLocation();
        int n = RobotPlayer.rng.nextInt(100);
        MapLocation me = rc.getLocation();

        // rivers
        if (mapWidth == 55 && mapHeight == 45) {
            MapLocation loc1 = new MapLocation(8, 41);
            MapLocation loc2 = new MapLocation(45, 3);

            if (n < 100) {
                return me.distanceSquaredTo(loc1) >= me.distanceSquaredTo(loc2) ? loc2 : loc1;
            } else {
                return randLoc;
            }
        }

        if (mapWidth == 60 && mapHeight == 60) {
            // eckleberg
            if (me.x - mapWidth < -50 || me.x - mapWidth > -8 || me.y - mapHeight < -50 || me.y - mapHeight > -8) {
                return randLoc;
            } else { // Colosseum
                MapLocation loc1 = new MapLocation(0, 59);
                MapLocation loc2 = new MapLocation(0, 0);
                MapLocation loc3 = new MapLocation(59, 59);
                MapLocation loc4 = new MapLocation(59, 0);
                MapLocation loc5 = new MapLocation(28, 0);
                MapLocation loc6 = new MapLocation(28, 59);
                MapLocation loc7 = new MapLocation(52, 29);
                MapLocation minLoc = new MapLocation(1000, 1000);
    
                MapLocation[] locs = {loc1, loc2, loc3, loc4, loc5, loc6};
                for (MapLocation loc : locs) {
                    if (me.distanceSquaredTo(loc) < me.distanceSquaredTo(minLoc)) {
                        minLoc = loc;
                    }
                }
                if (minLoc.equals(loc5) || minLoc.equals(loc6)) {
                    if (n < 75) {
                        minLoc = loc7;
                    }
                }
                return minLoc;
            }
        }

        if (mapWidth == 49 && mapHeight == 25) {
            MapLocation loc1 = new MapLocation(4, 12);
            MapLocation loc2 = new MapLocation(10, 12);
            MapLocation loc3 = new MapLocation(14, 12);
            MapLocation loc4 = new MapLocation(22, 12);
            MapLocation loc5 = new MapLocation(28, 12);
            MapLocation loc6 = new MapLocation(32, 12);
            MapLocation loc7 = new MapLocation(40, 12);
            MapLocation loc8 = new MapLocation(46, 12);
            MapLocation[] locs = {loc1, loc2, loc3, loc4, loc5, loc6, loc7, loc8};
            return locs[RobotPlayer.rng.nextInt(8)];
        }

        if (mapWidth == 60 && mapHeight == 30) {
            return new MapLocation(30, 16);
        }



        return RobotPlayer.getRandomMapLocation();
    }
    
    static void mine(RobotController rc, MapLocation loc) throws GameActionException {
        for (Direction dir : RobotPlayer.directions) {
            if (!rc.isActionReady()) {
                break;
            }
            MapLocation newLoc = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(newLoc)) {
                while (rc.senseGold(newLoc) > 0 && rc.canMineGold(newLoc)) {
                    rc.mineGold(newLoc);
                }
                while (rc.senseLead(newLoc) > 1 && rc.canMineLead(newLoc)) {
                    rc.mineLead(newLoc);
                }
            }
        }
    }


    static MapLocation findNearbyMetals(RobotController rc, MapLocation me, MapLocation target) throws GameActionException {
        
        int distanceToTarget = me.distanceSquaredTo(target);

        for (MapLocation loc : rc.senseNearbyLocationsWithLead(rc.getType().visionRadiusSquared)) {
            if (rc.senseLead(loc) > 10) {
                int distanceToLoc = me.distanceSquaredTo(loc);
                if (distanceToLoc < distanceToTarget) {
                    target = loc;
                    distanceToTarget  = distanceToLoc;
                }
                if (distanceToLoc == 0) {
                    break;
                }
            }
        }

        for (MapLocation loc : rc.senseNearbyLocationsWithGold(rc.getType().visionRadiusSquared)) {
            target = loc;
            break;
        }

        return target;
    }


    static MapLocation backupRetreatTarget;
    static int age = 0;
    static boolean healing = false;
    static boolean retreating = false;
    static boolean selfDestructing = false;
    static MapLocation lastLocation = new MapLocation(0, 0);
    static MapLocation lastLastLocation = new MapLocation(0, 0);
    static MapLocation lastLastLastLocation = new MapLocation(0, 0);

    static void run(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        if (!lastLocation.equals(me)) {
            lastLastLastLocation = lastLastLocation;
            lastLastLocation = lastLocation;
            lastLocation = me;
        }
        
        age++;
        if (age == 1) {
            backupRetreatTarget = me;
        }

        if (globalTarget == null) {
            if (rc.getRoundNum() < 20) {
                globalTarget = earlyGameTarget(rc, RobotPlayer.mapWidth, RobotPlayer.mapHeight);
            } else {
                globalTarget = RobotPlayer.getRandomMapLocation();
            }
        }

        

        if (me.distanceSquaredTo(globalTarget) <= 2) {
            globalTarget = RobotPlayer.getRandomMapLocation();
        }
        
        MapLocation target = globalTarget;


        target = findNearbyMetals(rc, me, target);

        MapLocation fleeTarget = null;


        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        boolean fleeing = false;
        if (enemies.length > 0) {
            for (RobotInfo enemy : enemies) {
                Comms.setEnemyLocation(rc, enemy.location, Comms.getEnemyLocations(rc));
                if (enemy.type == RobotType.SOLDIER) {
                    if (rc.getHealth() < rc.getType().health / 3) {
                        retreating = true;
                    } else {
                        fleeing = true;
                        fleeTarget = rc.adjacentLocation(me.directionTo(enemy.location));
                        globalTarget = RobotPlayer.getRandomMapLocation();
                    }
                    break;
                }
            }
        }

        if (rc.getHealth() < rc.getType().health / 3) {
            retreating = true;
            target = backupRetreatTarget;
        }

        if (rc.getHealth() == rc.getType().health) {
            retreating = false;
        }

        // Mine
        mine(rc, rc.getLocation());

        if (retreating && !selfDestructing) {
            MapLocation retreatTarget = Comms.getNearestArchonLocation(rc, me);

            if (retreatTarget == null) {
                retreatTarget = backupRetreatTarget;
            }

            if (retreatTarget != null) {
                RobotPlayer.move2(rc, retreatTarget, 2);
                if (me.distanceSquaredTo(retreatTarget) <= 9) {
                    selfDestructing = true;
                }

            } else {
                System.out.println("Couldnt find retreat target");
            }
        } else if (selfDestructing) {
            MapLocation nearestFreeTile = RobotPlayer.findNearestEmptyTile(rc, me);
            if (nearestFreeTile != null) {
                RobotPlayer.move2(rc, nearestFreeTile, 2);
                if (rc.getLocation().equals(nearestFreeTile)) {
                    System.out.println("Disintegrating");
                    rc.disintegrate();
                }

            } else {
                RobotPlayer.move2(rc, target, 2);
            }
        }
        
        if (!retreating) {
            if (fleeing && fleeTarget != null) {
                RobotPlayer.move(rc, fleeTarget);
            } else if (!me.equals(target)) {
                Movement.move(rc, target, lastLastLastLocation, 1, false);
                RobotPlayer.move(rc, target);
            }
        }

        if (rc.isMovementReady()) {
            RobotPlayer.stepOffRubble(rc, me);
        }

        // int start = Clock.getBytecodeNum();
        // int end = Clock.getBytecodeNum();
        // int leftB = Clock.getBytecodesLeft();
        rc.setIndicatorString(target.toString());
        rc.setIndicatorDot(target, 1000, 0, 0);
        
    }
}
