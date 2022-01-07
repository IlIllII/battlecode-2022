package player11;

import battlecode.common.*;

strictfp class BuilderStrategy {

    static void run(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();

        SharedArrayTargetAndIndex targetAndIndex = RobotPlayer.locateCombatTarget(rc, me);
        MapLocation target = targetAndIndex.location;
        // int indexOfTarget = targetAndIndex.idx;

        Team opponent = rc.getTeam().opponent();
        Team player = rc.getTeam();
        RobotInfo[] allies = rc.senseNearbyRobots(-1, player);
        MapLocation repairSpot = new MapLocation(0, 0);
        boolean repairing = false;
        for (int i = 0; i < allies.length; i++) {
            if (allies[i].type == RobotType.WATCHTOWER && allies[i].mode == RobotMode.PROTOTYPE) {
                repairSpot = allies[i].location;
                repairing = true;
                break;
            }
        }
        if (repairing) {
            if (me.distanceSquaredTo(repairSpot) > 1) {
                target = repairSpot;
            }
            if (rc.canRepair(repairSpot)) {
                rc.repair(repairSpot);
            }
        } else if (rc.senseNearbyRobots(-1, opponent).length < 3) {
            for (int i = 1; i < RobotPlayer.directions.length; i += 2) {
                if (rc.canBuildRobot(RobotType.WATCHTOWER, RobotPlayer.directions[i])) {
                    MapLocation potentialLoc = rc.adjacentLocation(RobotPlayer.directions[i]);

                    if (RobotPlayer.isLandSuitableForBuilding(rc, potentialLoc)) {
                        rc.buildRobot(RobotType.WATCHTOWER, RobotPlayer.directions[i]);
                        break;
                    }
                }
            }
        }

        Direction dir = me.directionTo(target);
        rc.setIndicatorString(target.toString());

        while (true) {
            if (rc.canMove(dir)) {
                rc.move(dir);
                break;
            } else {
                dir = dir.rotateLeft();
                if (dir == me.directionTo(target)) {
                    break;
                }
            }
        }
    }
}
