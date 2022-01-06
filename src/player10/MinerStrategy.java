package player10;

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
        
        MapLocation target = doMinerPathing(rc, me);

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

        if (fleeing) {
            RobotPlayer.move(rc, rc.adjacentLocation(fleeDirection));
        } else {
            RobotPlayer.move(rc, target);
        }

        // Mine
        int start = Clock.getBytecodeNum();
        mine(rc, rc.getLocation());
        int end = Clock.getBytecodeNum();
        rc.setIndicatorString("" + (end - start));
    }

    private static MapLocation doMinerPathing(RobotController rc, MapLocation me) throws GameActionException {
        MapLocation target = globalTarget;
        
        // Set move target
        int distToTarget = me.distanceSquaredTo(target);
        for (MapLocation loc : rc.senseNearbyLocationsWithLead(100)) {
            if (rc.senseLead(loc) > 5) {
                int distToLoc = me.distanceSquaredTo(loc);
                if (distToLoc < distToTarget) {
                    target = loc;
                    distToTarget = distToLoc;
                }
                if (distToLoc == 0) {
                    break;
                }
            }
        }
        for (MapLocation loc : rc.senseNearbyLocationsWithGold(100)) {
            // if there are any gold sources nearby, go there
            // immediately, and don't bother with the rest of the
            // pathfinding.
            target = loc;
            break;
        }
        
        rc.setIndicatorLine(me, target, 100, 0, 0);
        return target;
    }
}
