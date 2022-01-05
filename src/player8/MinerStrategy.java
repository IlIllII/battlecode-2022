package player8;

import battlecode.common.*;


strictfp class MinerStrategy {
    static MapLocation globalTarget = new MapLocation(-100, -100);
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
                if (rc.senseGold(loc) > 0) {
                    target = loc;
                    break;
                }
                if (rc.senseLead(loc) > 0) {
                    if (me.distanceSquaredTo(loc) < me.distanceSquaredTo(target)) {
                        target = loc;
                    } else if (me.distanceSquaredTo(loc) == 0) {
                        break;
                    }
                }
            }
        }
        rc.setIndicatorLine(me, target, 100, 0, 0);

        // Mine
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        RobotPlayer.move(rc, target);
    }
}
