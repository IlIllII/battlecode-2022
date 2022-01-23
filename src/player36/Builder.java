package player36;

import battlecode.common.*;

strictfp class Builder {
    static MapLocation backupLocation = RobotPlayer.getRandomMapLocation();
    static MapLocation parentLocation;
    static MapLocation buildLocation = new MapLocation(-1,-1);
    static RobotType myType = RobotType.BUILDER;
    static int currRound = -1;
    static boolean TryBuild = true;
    static boolean TryRepair = false;
    static MapLocation closestEdge;

    static int herustic(RobotController rc, int rubble, MapLocation myLocation) throws GameActionException{
        MapLocation offensive = null;
        if (offensive == null){
            return rubble + closestEdge.distanceSquaredTo(myLocation);
        }else {
            return rubble - 2 * myLocation.distanceSquaredTo(offensive);
        }
    }


    static MapLocation getBuildLocation(RobotController rc, MapLocation[] nearLocations) throws GameActionException{
        MapLocation build_Location = rc.getLocation();
        MapLocation myLocation = rc.getLocation();

        int minDistance = 99999;
        int minRubble = 99999;
        // find block with fewest rubble
        for (MapLocation location : nearLocations){
            if (herustic(rc, minRubble, build_Location) > herustic(rc, rc.senseRubble(location), location)&& location.distanceSquaredTo(parentLocation) > 4 && !rc.canSenseRobotAtLocation(location)) {
                minDistance = myLocation.distanceSquaredTo(location);
                minRubble = rc.senseRubble(location);
                build_Location = location;
            }
        }
        return build_Location;
    }

    static void run(RobotController rc) throws GameActionException {
        MapLocation[] nearlocations;
        MapLocation myLocation = rc.getLocation();
        nearlocations = rc.getAllLocationsWithinRadiusSquared(myLocation, myType.visionRadiusSquared);
        ArchonLocation[] archLocs = Communications.getArchonLocations(rc);
        int lead = rc.getTeamLeadAmount(rc.getTeam());
        // denote parent location
        currRound++;
        // set closest archon as parent archon
        for (RobotInfo info : rc.senseNearbyRobots(2)) {
            if (info.getType() == RobotType.ARCHON) {
                parentLocation = info.getLocation();
                break;
            }
        }

        if (currRound == 0) {
            int width = rc.getMapWidth();
            int height = rc.getMapHeight();
            MapLocation[] locations = new MapLocation[8];
            locations[0] = new MapLocation(0,0);
            locations[1] = new MapLocation(0,height);
            locations[2] = new MapLocation(width,height);
            locations[3] = new MapLocation(width,0);
            locations[4] = new MapLocation(width/2,height);
            locations[5] = new MapLocation(width,height/2);
            locations[6] = new MapLocation(width/2,0);
            locations[7] = new MapLocation(0,height/2);
            int distance = 999999;
            for (MapLocation location : locations) {
                if (distance > location.distanceSquaredTo(myLocation)) {
                    distance = location.distanceSquaredTo(myLocation);
                    closestEdge = location;
                }
            }
            rc.setIndicatorString(closestEdge.x + " ," + closestEdge.y);
        }

        //try to repair near buildings
        for (RobotInfo info : rc.senseNearbyRobots(myType.actionRadiusSquared, rc.getTeam())) {
            if (!rc.isActionReady()) break;
            if (info.getType() == RobotType.LABORATORY && info.getHealth() < RobotType.LABORATORY.health) {
                rc.setIndicatorString("repair " + info.health);
                TryRepair = true;
                if (rc.canRepair(info.getLocation())) {
                    rc.repair(info.getLocation());
                }
                break;
            }

            if (info.getType() == RobotType.ARCHON && info.getHealth() < RobotType.ARCHON.health) {
                rc.setIndicatorString("repair " + info.health);
                TryRepair = true;
                if (rc.canRepair(info.getLocation())) {
                    rc.repair(info.getLocation());
                }
                break;
            }
            TryRepair = false;
        }

        /////////////////////////////////////
        boolean defendArchon = false;
        for (ArchonLocation archLoc : archLocs) {
            if (archLoc != null && archLoc.exists && archLoc.shouldDefend) {
                defendArchon = true;
            }
        }

        // every 120 try build
        if (currRound % 120 == 0) {
            TryBuild = true;
        }

        // find a location to build
        if (buildLocation.equals(new MapLocation(-1,-1)) || rc.canSenseRobotAtLocation(buildLocation)){
            buildLocation = getBuildLocation(rc, nearlocations);
        }

// when not repairing
       if (!TryRepair){
           if (TryBuild && myLocation.distanceSquaredTo(buildLocation) <= 2) {
               Direction dir = myLocation.directionTo(buildLocation);
               if (rc.canBuildRobot(RobotType.LABORATORY, dir) && !defendArchon && lead > 200) {
                   rc.buildRobot(RobotType.LABORATORY, dir);
                   TryBuild = false;
               }

           }else{
               if (TryBuild && rc.isMovementReady()) {
                   RobotPlayer.move2(rc, buildLocation, 2);
               }
           }

       }




        //////////////////////////////////////////////////////////////////////////////////////



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
