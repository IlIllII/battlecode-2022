package player19;

import battlecode.common.*;

strictfp class SoldierStrategy {

    static RobotType myType = RobotType.SOLDIER;
    final static int ATTACK_RADIUS_SQUARED = myType.actionRadiusSquared;
    final static int ATTACK_RADIUS_SQUARED_WITHIN_ONE_MOVE = 20;
    static int longestTime = 0;
    static MapLocation repairLocation;
    static int aliveTime = 0;
    static boolean healing = false;
    static MapLocation backupLocation = RobotPlayer.getRandomMapLocation();
    static MapLocation lastLocation = new MapLocation(0, 0);
    static boolean selfDestructing = false;
    static boolean foundRepairSpot = false;
    static MapLocation repairLoc = new MapLocation(1000, 1000);



    static BFS bfs;

    static void run(RobotController rc) throws GameActionException { 
        MapLocation me = rc.getLocation();

        if (me.distanceSquaredTo(backupLocation) <= 4) {
            backupLocation = RobotPlayer.getRandomMapLocation();
        }


        // if (aliveTime == 0) {
        //     bfs = new AdvancedMove(rc);
        // }

        aliveTime++;
        if (aliveTime == 2) {
            repairLocation = me;
        }
        

        MapLocation target = RobotPlayer.locateCombatTarget(rc, me, backupLocation);
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.opponent);

        // RobotPlayer.attackGlobalTargetIfAble(rc, target, me);



        TripleTarget localTargets = RobotPlayer.acquireLocalTargets(rc, target, enemies, me);

        MapLocation primaryTarget = localTargets.primary;
        MapLocation secondaryTarget = localTargets.secondary;
        MapLocation tertiaryTarget = localTargets.tertiary;

        // if (rc.getRoundNum() < 45) {
        //     tertiaryTarget = rc.adjacentLocation(RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)]);
        // }


        if (rc.senseNearbyRobots(2, rc.getTeam()).length > 4 && !selfDestructing) {
            RobotPlayer.move2(rc, primaryTarget, 2);
        }
        if (rc.canAttack(primaryTarget)) {
            rc.attack(primaryTarget);
            Comms.setEnemyLocation(rc, primaryTarget);
        }
        if (rc.canAttack(secondaryTarget)) {
            rc.attack(secondaryTarget);
        }
        if (rc.canAttack(tertiaryTarget)) {
            rc.attack(tertiaryTarget);
        }

        if (selfDestructing) {
            MapLocation minTile = null;
            double minDistance = 10000;

            if (rc.senseLead(me) == 0) {
                rc.disintegrate();
            }

            MapLocation[] tilesNearMe = rc.getAllLocationsWithinRadiusSquared(me, 100);

            for (MapLocation tile : tilesNearMe) {
                if (rc.canSenseLocation(tile) && rc.senseLead(tile) == 0 && rc.senseGold(tile) == 0 && me.distanceSquaredTo(tile) < minDistance) {
                    minTile = tile;
                    minDistance = me.distanceSquaredTo(tile);
                }
            }

            if (minTile != null) {
                RobotPlayer.move2(rc, minTile, 2);
            } else {
                RobotPlayer.move(rc, primaryTarget);
            }
            if (rc.senseLead(me) == 0) {
                rc.disintegrate();
            }


        }

        // if (rc.senseNearbyRobots(-1, rc.getTeam()).length < 5 /* && rc.canSenseLocation(primaryTarget) && rc.canSenseRobotAtLocation(primaryTarget) && rc.senseRobotAtLocation(primaryTarget).type.equals(RobotType.SOLDIER) */ ) {
        //     RobotPlayer.move2(rc, rc.adjacentLocation(me.directionTo(primaryTarget).opposite()).add(me.directionTo(primaryTarget)), 3);
        // }

        if (rc.isActionReady() && rc.isMovementReady()) {

            if (healing || rc.getHealth() < (rc.getType().health / 4)) {

                ArchonLocation[] archLocs = Comms.getArchonLocations(rc);



                while (!foundRepairSpot) {
                    for (ArchonLocation archLoc : archLocs) {
                        // if (repairLoc == null || (archLoc.exists && me.distanceSquaredTo(repairLoc) > me.distanceSquaredTo(archLoc.location))) {
                        //     repairLoc = archLoc.location;
                        // }
                        if (archLoc.exists && RobotPlayer.rng.nextInt(4) == 0) {
                            repairLoc = archLoc.location;
                            foundRepairSpot = true;
                        }
                    }
                }

                primaryTarget = repairLoc;
                healing = true;
            }

            if (rc.getHealth() < rc.getType().health / 2 && me.distanceSquaredTo(repairLoc) <= 4) {
                selfDestructing = true;
            }

            if (rc.getHealth() > rc.getType().health - 3) {
                healing = false;
            }

            // Experimental move.
            int startTime = Clock.getBytecodeNum();



            
            try {
                Direction dir = AdvancedMove.getBestDir(rc, primaryTarget);

                if (dir != null && !dir.equals(Direction.CENTER) && rc.canMove(dir)) {
                    if (!rc.adjacentLocation(dir).equals(lastLocation)) {
                        rc.move(dir);
                        lastLocation = rc.getLocation();
                    } else {
                        if (rc.canMove(rc.getLocation().directionTo(primaryTarget))) {
                            RobotPlayer.move(rc, primaryTarget);
                        }
                    }
                }
            } catch (Exception e) {
                //TODO: handle exception
                System.out.println("Move returned null");;
            }
            // RobotPlayer.move2(rc, tertiaryTarget, recursionLimit);


            int end = Clock.getBytecodeNum();

            if ((end - startTime) > longestTime) {
                longestTime = (end - startTime);
            }
            rc.setIndicatorString("" + longestTime);
            rc.setIndicatorLine(me, primaryTarget, 1000, 0, 1000);;

            // Fall back to simple move incase other move doesn't work.
            RobotPlayer.move(rc, primaryTarget);


            // RobotPlayer.move(rc, tertiaryTarget);

            if (rc.canAttack(tertiaryTarget)) {
                rc.attack(tertiaryTarget);
            }
        }
        
        if (!rc.isActionReady() && rc.isMovementReady()) {
            MapLocation retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
            retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
            retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
            retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
            retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
            RobotPlayer.move(rc, retreatMove);
        }


        // try {
        //     Direction dir = AdvancedMove.getBestDir(rc, primaryTarget);

        //     if (dir != null && !dir.equals(Direction.CENTER) && rc.canMove(dir)) {
        //         if (!rc.adjacentLocation(dir).equals(lastLocation)) {
        //             rc.move(dir);
        //             lastLocation = rc.getLocation();
        //         } else {
        //             if (rc.canMove(rc.getLocation().directionTo(tertiaryTarget))) {
        //                 RobotPlayer.move(rc, tertiaryTarget);
        //             }
        //         }
        //     }
        // } catch (Exception e) {
        //     //TODO: handle exception
        //     System.out.println("Move returned null");;
        // }
        RobotPlayer.stepOffRubble(rc, me);
        
        // rc.setIndicatorLine(me, target, 0, 1, 0);
    }
}