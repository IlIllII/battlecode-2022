package player33;

import battlecode.common.*;

strictfp class BuilderStrategy {
    static boolean selfDestruct = RobotPlayer.rng.nextInt(4) == 1;
    static MapLocation backupLocation = RobotPlayer.getRandomMapLocation();
    static int labCount = 0;


    static void run(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        int round = rc.getRoundNum();
        Team player = rc.getTeam();
        Team opponent = rc.getTeam().opponent();
        int leadAmount = rc.getTeamLeadAmount(player);

        int x = RobotPlayer.mapWidth - me.x > RobotPlayer.mapWidth / 2 ? 0 : RobotPlayer.mapWidth - 1;
        int y = RobotPlayer.mapHeight - me.y > RobotPlayer.mapHeight / 2 ? 0 : RobotPlayer.mapHeight - 1;

        MapLocation target = new MapLocation(x, y);

        if (round < 2000) {
            selfDestruct = true;
        }


        RobotInfo[] allies = rc.senseNearbyRobots(-1, player);
        MapLocation repairSpot = new MapLocation(0, 0);

        boolean repairing = false;
        for (int i = 0; i < allies.length; i++) {
            if (((allies[i].type == RobotType.WATCHTOWER || allies[i].type == RobotType.LABORATORY)  && allies[i].mode == RobotMode.PROTOTYPE) || (allies[i].type == RobotType.ARCHON && allies[i].getHealth() < RobotType.ARCHON.health)) {
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
        } /* else if (rc.senseNearbyRobots(-1, opponent).length < 3 && leadAmount > 200 && round > 200) {
            for (int i = 1; i < RobotPlayer.directions.length; i += 2) {
                if (rc.canBuildRobot(RobotType.WATCHTOWER, RobotPlayer.directions[i])) {
                    MapLocation potentialLoc = rc.adjacentLocation(RobotPlayer.directions[i]);

                    // if (RobotPlayer.isLandSuitableForBuilding(rc, potentialLoc)) {
                        rc.buildRobot(RobotType.WATCHTOWER, RobotPlayer.directions[i]);
                        break;
                    // }
                }
            }
        } */
        // } else if (labCount == 0 && rc.senseNearbyRobots(-1, opponent).length < 3 && leadAmount > 260 && round > 50) {
        //     for (int i = 1; i < RobotPlayer.directions.length; i += 2) {
        //         if (rc.canBuildRobot(RobotType.LABORATORY, RobotPlayer.directions[i])) {
        //             MapLocation potentialLoc = rc.adjacentLocation(RobotPlayer.directions[i]);

        //             if (RobotPlayer.isLandSuitableForBuilding(rc, potentialLoc)) {
        //                 rc.buildRobot(RobotType.LABORATORY, RobotPlayer.directions[i]);
        //                 labCount++;
        //                 break;
        //             }
        //         }
        //     }
        // }

        // if (!repairing && !selfDestruct) {
        //     Movement.move(rc, target, me, 1, false);
        // } else {
        //     Movement.move(rc, repairSpot, me, 1, false);
        // }

        if (repairing) {
            Movement.move(rc, repairSpot, me, 1, false);
        }

        rc.setIndicatorLine(me, target, 0, 0, 0);









        // int start = Clock.getBytecodeNum();
        // MapLocation me = rc.getLocation();

        // if (round < 19) {
        //     selfDestruct = true;
        // }

        if (selfDestruct && !repairing) {
            MapLocation nearestFreeTile = RobotPlayer.findNearestEmptyTile(rc, me);
            if (nearestFreeTile != null) {
                Movement.move(rc, nearestFreeTile, target, 1, false);
                if (rc.getLocation().equals(nearestFreeTile)) {
                    rc.disintegrate();
                }
            } else {
                // System.out.println("Should be moving");
                Movement.move(rc, target, new MapLocation(1000, 1000), 1, false);
            }
        }

        // MapLocation target = RobotPlayer.locateCombatTarget(rc, me, backupLocation);
        // // int indexOfTarget = targetAndIndex.idx;

        // if (round < 100) {
        //     target = RobotPlayer.getRandomMapLocation();
        // }

        // Team opponent = rc.getTeam().opponent();
        // Team player = rc.getTeam();

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
