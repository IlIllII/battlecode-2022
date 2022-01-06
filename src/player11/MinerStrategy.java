package player11;

import battlecode.common.*;


strictfp class MinerStrategy {
    static MapLocation globalTarget = new MapLocation(-100, -100);
    
    static void mine(RobotController rc, MapLocation loc) throws GameActionException {
        if (rc.senseGold(loc) > 1 && rc.canMineGold(loc)) {
            rc.mineGold(loc);
        } else if (rc.senseLead(loc) > 1 && rc.canMineLead(loc)) {
            rc.mineLead(loc);
        }

        for (Direction dir : RobotPlayer.directions) {
            if (!rc.isActionReady()) {
                break;
            }
            MapLocation newLoc = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(newLoc)) {
                while (rc.senseGold(newLoc) > 1 && rc.canMineGold(newLoc)) {
                    rc.mineGold(newLoc);
                }
                while (rc.senseLead(newLoc) > 1 && rc.canMineLead(newLoc)) {
                    rc.mineLead(newLoc);
                }
            }
        }
    }

    static void run(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();

        if (rc.canSenseLocation(globalTarget) && (rc.senseLead(globalTarget) == 0) || globalTarget.x == -100) {
            int x = Math.min(RobotPlayer.rng.nextInt(rc.getMapWidth()), rc.getMapWidth() - 1);
            int y = Math.min(RobotPlayer.rng.nextInt(rc.getMapHeight()), rc.getMapHeight() - 1);
            globalTarget = new MapLocation(x, y);
        }
        
        MapLocation target = globalTarget;


        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(me, 100);
        
        // Set move target
        for (MapLocation loc : locations) {
            if (rc.canSenseLocation(loc)) {
                if (rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).type == RobotType.ARCHON && rc.senseRobotAtLocation(loc).team == rc.getTeam().opponent()) {
                    RobotPlayer.addLocationToSharedArray(rc, loc, 0, 0);
                }
                if (rc.senseGold(loc) > 1) {
                    target = loc;
                    break;
                }
                if (rc.senseLead(loc) > 5) {
                    if (me.distanceSquaredTo(loc) < me.distanceSquaredTo(target)) {
                        target = loc;
                    } else if (me.distanceSquaredTo(loc) == 0) {
                        break;
                    }
                }
            }
        }
        rc.setIndicatorLine(me, target, 100, 0, 0);

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        boolean fleeing = false;
        Direction fleeDirection = Direction.NORTH;
        if (enemies.length > 0) {
            for (RobotInfo enemy : enemies) {
                if (enemy.type == RobotType.SOLDIER) {
                    fleeing = true;
                    fleeDirection = me.directionTo(enemy.location).opposite();
                }
            }
        }

        if (!fleeing) {
            RobotPlayer.move(rc, target);
        } else {
            RobotPlayer.move(rc, rc.adjacentLocation(fleeDirection));
        }
        // RobotPlayer.move(rc, target);

        // Mine
        int start = Clock.getBytecodeNum();
        mine(rc, rc.getLocation());
        int end = Clock.getBytecodeNum();
        rc.setIndicatorString("" + (end - start));

        // for (int dx = -1; dx <= 1; dx++) {
        //     for (int dy = -1; dy <= 1; dy++) {
        //         MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
        //         while (rc.canMineGold(mineLocation)) {
        //             rc.mineGold(mineLocation);
        //         }
        //         while (rc.canMineLead(mineLocation)) {
        //             rc.mineLead(mineLocation);
        //         }
        //     }
        // }

    }
}
