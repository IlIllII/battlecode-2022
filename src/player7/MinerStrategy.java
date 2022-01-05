package player7;

import battlecode.common.*;


strictfp class MinerStrategy {
    static void run(RobotController rc) throws GameActionException {

        // As a placeholder we will use the global target as our target, but we
        // should replace this with better logic.
        int globalTarget = rc.readSharedArray(0);
        int xCoord = (globalTarget & (63 << 6)) >> 6;
        int yCoord = (63 & globalTarget);

        MapLocation target = new MapLocation(xCoord, yCoord);
        MapLocation me = rc.getLocation();
        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(me, 100);
        
        // Set move target
        for (MapLocation loc : locations) {
            if (rc.canSenseLocation(loc)) {
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
