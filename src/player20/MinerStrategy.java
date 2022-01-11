package player20;

import battlecode.common.*;


strictfp class MinerStrategy {
    static MapLocation globalTarget = RobotPlayer.getRandomMapLocation();
    
    static void mine(RobotController rc, MapLocation loc) throws GameActionException {
        for (Direction dir : RobotPlayer.directions) {
            if (!rc.isActionReady()) {
                break;
            }
            MapLocation newLoc = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(newLoc)) {
                while (rc.senseGold(newLoc) > 0 && rc.canMineGold(newLoc)) {
                    rc.mineGold(newLoc);
                }
                while (rc.senseLead(newLoc) > 1 && rc.canMineLead(newLoc)) {
                    rc.mineLead(newLoc);
                }
            }
        }
    }


    static MapLocation findNearbyMetals(RobotController rc, MapLocation me, MapLocation target) throws GameActionException {
        
        int distanceToTarget = me.distanceSquaredTo(target);

        for (MapLocation loc : rc.senseNearbyLocationsWithLead(rc.getType().visionRadiusSquared)) {
            if (rc.senseLead(loc) > 10) {
                int distanceToLoc = me.distanceSquaredTo(loc);
                if (distanceToLoc < distanceToTarget) {
                    target = loc;
                    distanceToTarget  = distanceToLoc;
                }
                if (distanceToLoc == 0) {
                    break;
                }
            }
        }

        for (MapLocation loc : rc.senseNearbyLocationsWithGold(rc.getType().visionRadiusSquared)) {
            target = loc;
            break;
        }

        return target;
    }

    static MapLocation repairLocation;
    static int age = 0;
    static boolean healing = false;

    static void run(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        age++;

        if (age == 1) {
            repairLocation = me;
        }

        if (me.distanceSquaredTo(globalTarget) <= 2) {
            globalTarget = RobotPlayer.getRandomMapLocation();
        }
        
        MapLocation target = globalTarget;



        
        target = findNearbyMetals(rc, me, target);


        // // Set move target
        // for (MapLocation loc : locations) {
        //     if (rc.canSenseLocation(loc)) {
        //         if (rc.canSenseRobotAtLocation(loc) && rc.senseRobotAtLocation(loc).type == RobotType.ARCHON && rc.senseRobotAtLocation(loc).team == rc.getTeam().opponent()) {
        //             RobotPlayer.addLocationToSharedArray(rc, loc, 0, 0);
        //         }
        //         if (rc.senseGold(loc) > 1) {
        //             target = loc;
        //             break;
        //         }
        //         if (rc.senseLead(loc) > 5) {
        //             if (me.distanceSquaredTo(loc) < me.distanceSquaredTo(target)) {
        //                 target = loc;
        //             } else if (me.distanceSquaredTo(loc) == 0) {
        //                 break;
        //             }
        //         }
        //     }
        // }
        // rc.setIndicatorLine(me, target, 100, 0, 0);

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        boolean fleeing = false;
        if (rc.getHealth() < rc.getType().health / 4) {
            if (enemies.length > 0) {
                for (RobotInfo enemy : enemies) {
                    if (enemy.type == RobotType.SOLDIER) {
                        fleeing = true;
                        target = repairLocation;
                        break;
                    }
                }
            }
        }

        if (healing == true || rc.getHealth() < rc.getType().health / 3) {
            healing = true;
            target = repairLocation;
        }

        if (healing && rc.getHealth() == rc.getType().health) {
            healing = false;
        }

        // Mine
        mine(rc, rc.getLocation());
        
        if (!fleeing) {
            if (!me.equals(target)) {
                int bytecodesLeft = Clock.getBytecodesLeft();
                if (bytecodesLeft > 6300) {
                    Direction dirr = AdvancedMove.getBestDir(rc, target);
                    if (dirr != null && rc.canMove(dirr)) {
                        rc.move(dirr);
                        rc.setIndicatorString("Using advancedMove");
                    }
                } else {
                    RobotPlayer.move2(rc, target, 2);
                }
            }
        } else {
            RobotPlayer.move2(rc, repairLocation, 2);
        }
        if (rc.isMovementReady()) {
            RobotPlayer.stepOffRubble(rc, me);
        }

        // int start = Clock.getBytecodeNum();
        // int end = Clock.getBytecodeNum();
        // int leftB = Clock.getBytecodesLeft();
        // rc.setIndicatorString("" + leftB);
        
    }
}
