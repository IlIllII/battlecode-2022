package player21;

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
    static MapLocation lastLastLocation = new MapLocation(0, 0);
    static int MaxMovementCost = 0;
    // static MapLocation repairLoc = null;
    // static boolean foundRepairSpot = false;
    

    static void run(RobotController rc) throws GameActionException {


        MapLocation me = rc.getLocation();

        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, RobotPlayer.opponent);
        MapLocation target = Targeting.getTargetFromGlobalAndLocalEnemyLocations(rc, enemies, backupLocation);
        MapLocation offensiveTarget = Targeting.getOffensiveTarget(rc);
        MapLocation defensiveTarget = Targeting.getDefensiveTarget(rc);
        
        // * Targeting cascade
        if (target == null) {
            backupLocation = RobotPlayer.getRandomMapLocation();
            target = backupLocation;
        }

        if (target.equals(backupLocation)) {
            if (offensiveTarget != null) {
                target = offensiveTarget;
            }
            if (defensiveTarget != null) {
                target = defensiveTarget;
            }
        }



        // TODO: Test this - moving toward target if clumping
        if (rc.senseNearbyRobots(2, rc.getTeam()).length > 4) {
            RobotPlayer.move2(rc, target, 2);
        }


        if (rc.isActionReady()) {
            if (rc.canAttack(target)) {
                rc.attack(target);

                // int x = (target.x - me.x);
                // int y = (target.y - me.y);
                // target = new MapLocation(me.x - x, me.y - y);

                if (!rc.isActionReady() && rc.isMovementReady()) {
                    MapLocation retreatMove = rc.adjacentLocation(me.directionTo(target).opposite());
                    retreatMove = rc.adjacentLocation(me.directionTo(target).opposite());
                    retreatMove = rc.adjacentLocation(me.directionTo(target).opposite());
                    retreatMove = rc.adjacentLocation(me.directionTo(target).opposite());
                    retreatMove = rc.adjacentLocation(me.directionTo(target).opposite());
                    RobotPlayer.move2(rc, retreatMove, 3);
                }
            }
        }


        if (rc.isMovementReady()) {

            // if (!rc.isActionReady()) {
            //     RobotPlayer.stepOffRubble(rc, me);
            // }

            if (Clock.getBytecodesLeft() > 6300) {
            rc.setIndicatorString("Using BFS");

                int start = Clock.getBytecodeNum();
                Direction dir = AdvancedMoveCopy.getBestDir(rc, target);
                int end = Clock.getBytecodeNum();
                if (end - start > MaxMovementCost) {
                    MaxMovementCost = end - start;
                }
                
                if (dir != null && rc.canMove(dir) && !rc.adjacentLocation(dir).equals(lastLocation)) {
                    lastLastLocation = lastLocation;
                    lastLocation = rc.getLocation();
                    rc.move(dir);
                } else {
                    RobotPlayer.move(rc, target);
                }
            } else {
                rc.setIndicatorString("Using Move2");
            }
        }


        if (rc.isActionReady()) {
            if (rc.canAttack(target)) {
                rc.attack(target);
            }
        }

        rc.setIndicatorLine(rc.getLocation(), target, 0, 0, 0);
    }
}