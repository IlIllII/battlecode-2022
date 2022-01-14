package player29;

import battlecode.common.*;

strictfp class BuilderStrategy {
    static boolean selfDestruct = RobotPlayer.rng.nextInt(4) == 1;
    static MapLocation backupLocation = RobotPlayer.getRandomMapLocation();


    static void run(RobotController rc) throws GameActionException {
        // int start = Clock.getBytecodeNum();
        // MapLocation me = rc.getLocation();
        // int round = rc.getRoundNum();

        // if (round < 19) {
        //     selfDestruct = true;
        // }

        // if (selfDestruct && rc.getRoundNum() < 400 && rc.senseLead(me) == 0 && rc.senseGold(me) == 0 && rc.senseRubble(me) == 0) {
        //     rc.disintegrate();
        // }

        // MapLocation target = RobotPlayer.locateCombatTarget(rc, me, backupLocation);
        // // int indexOfTarget = targetAndIndex.idx;

        // if (round < 100) {
        //     target = RobotPlayer.getRandomMapLocation();
        // }

        // Team opponent = rc.getTeam().opponent();
        // Team player = rc.getTeam();
        // RobotInfo[] allies = rc.senseNearbyRobots(-1, player);
        // MapLocation repairSpot = new MapLocation(0, 0);
        // boolean repairing = false;
        // for (int i = 0; i < allies.length; i++) {
        //     if (allies[i].type == RobotType.WATCHTOWER && allies[i].mode == RobotMode.PROTOTYPE) {
        //         repairSpot = allies[i].location;
        //         repairing = true;
        //         break;
        //     }
        // }
        // if (repairing) {
        //     if (me.distanceSquaredTo(repairSpot) > 1) {
        //         target = repairSpot;
        //     }
        //     if (rc.canRepair(repairSpot)) {
        //         rc.repair(repairSpot);
        //     }
        // } else if (rc.senseNearbyRobots(-1, opponent).length < 3) {
        //     for (int i = 1; i < RobotPlayer.directions.length; i += 2) {
        //         if (rc.canBuildRobot(RobotType.WATCHTOWER, RobotPlayer.directions[i])) {
        //             MapLocation potentialLoc = rc.adjacentLocation(RobotPlayer.directions[i]);

        //             if (RobotPlayer.isLandSuitableForBuilding(rc, potentialLoc)) {
        //                 rc.buildRobot(RobotType.WATCHTOWER, RobotPlayer.directions[i]);
        //                 break;
        //             }
        //         }
        //     }
        // }

        // int end = Clock.getBytecodeNum();
        // rc.setIndicatorString("" + (end - start));


        // Direction dir = me.directionTo(target);

        // ArchonLocation[] archLocs = Comms.getArchonLocations(rc);

        // MapLocation moveTarget = new MapLocation(1000, 1000);
        
        // boolean foundDefendee = false;
        // for (ArchonLocation archLoc : archLocs) {
        //     if (archLoc.exists && archLoc.shouldDefend) {
        //         foundDefendee = true;
        //         if (me.distanceSquaredTo(archLoc.location) < me.distanceSquaredTo(moveTarget)) {
        //             moveTarget = archLoc.location;
        //         } 
        //     }
        // }
        
        // // if (foundDefendee) {
        // //     target = moveTarget;
        // // }
        

        // // if (round < 50 && !repairing) {
        // //     dir = RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)];
        // // }

        // if (foundDefendee && !repairing) {
        //     RobotPlayer.move2(rc, target, 2);
        // } else if (me.distanceSquaredTo(moveTarget) < 25 && !repairing) {
        //     moveTarget = RobotPlayer.getRandomMapLocation();
        //     RobotPlayer.move2(rc, target, 2);
        // }

        // RobotPlayer.move2(rc, target, 2);


        // // for (int i = 0; i < 9; i++) {
        // //     if (rc.canMove(dir)) {
        // //         rc.move(dir);
        // //         break;
        // //     } else {
        // //         dir = dir.rotateLeft();
        // //         if (dir.equals(me.directionTo(target))) {
        // //             break;
        // //         }
        // //     }
        // // }
    }
}
