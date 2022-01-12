package player23;

import battlecode.common.*;


strictfp class MinerStrategy {
    static MapLocation globalTarget = RobotPlayer.getRandomMapLocation();
    
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

        if (me.distanceSquaredTo(globalTarget) <= 2) {
            globalTarget = RobotPlayer.getRandomMapLocation();
        }
        
        MapLocation target = globalTarget;

        target = findNearbyMetals(rc, me, target);

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        boolean fleeing = false;
        if (enemies.length > 0) {
            for (RobotInfo enemy : enemies) {
                if (enemy.type == RobotType.SOLDIER) {
                    Comms.setEnemyLocation(rc, enemy.location, Comms.getEnemyLocations(rc));
                    if (rc.getHealth() < rc.getType().health / 3) {
                        retreating = true;
                    } else {
                        fleeing = true;
                        globalTarget = RobotPlayer.getRandomMapLocation();
                    }
                    target = backupRetreatTarget;
                    break;
                }
            }
        }

        if (rc.getHealth() < rc.getType().health / 3) {
            retreating = true;
            target = backupRetreatTarget;
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
            if (!me.equals(target)) {
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
