package player31;

import battlecode.common.*;


strictfp class LaboratoryStrategy {
    static void run(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        MapLocation moveTarget = me;
        int minDistance = 100000;
        int minRubble = 100000;

        for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(me, 10000)) {
            if (rc.canSenseLocation(loc) && rc.senseRubble(loc) <= minRubble) {
                if (rc.senseRubble(loc) < minRubble) {
                    minDistance = me.distanceSquaredTo(loc);
                    minRubble = rc.senseRubble(loc);
                    moveTarget = loc;
                } else if (me.distanceSquaredTo(loc) < minDistance) {
                    moveTarget = loc;
                    minDistance = me.distanceSquaredTo(loc);
                    minRubble = rc.senseRubble(loc);
                }
            }
        }
        
        int round = rc.getRoundNum();

        if (round > 50 && !moveTarget.equals(me)) {
            if (rc.isTransformReady() && rc.getMode().equals(RobotMode.TURRET) && rc.canTransform()) {
                rc.transform();
            }
        }

        if (rc.getMode().equals(RobotMode.PORTABLE)) {
            if (me.equals(moveTarget)) {
                if (rc.isTransformReady() && rc.canTransform()) {
                    rc.transform();
                }
            }

            if (rc.isMovementReady()) {
                Movement.move(rc, moveTarget, new MapLocation(1000, 1000), 2, false);
            }
        }

        if (rc.isActionReady() && rc.canTransmute() && rc.getTeamLeadAmount(rc.getTeam()) > 80) {
            rc.transmute();
        }
    }
}
