package player13;

import battlecode.common.*;

strictfp class SoldierStrategy {

    static RobotType myType = RobotType.SOLDIER;
    final static int ATTACK_RADIUS_SQUARED = myType.actionRadiusSquared;
    final static int ATTACK_RADIUS_SQUARED_WITHIN_ONE_MOVE = 20;
    static int longestTime = 0;
    static MapLocation repairLocation;
    static int aliveTime = 0;
    static boolean healing = false;

    static void run(RobotController rc) throws GameActionException {  
        MapLocation me = rc.getLocation();

        aliveTime++;
        if (aliveTime == 2) {
            repairLocation = me;
        }
        

        SharedArrayTargetAndIndex indexAndTarget = RobotPlayer.locateCombatTarget(rc, me);
        MapLocation target = indexAndTarget.location;
        int sharedArrayIndex = indexAndTarget.idx;
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.opponent);

        RobotPlayer.attackGlobalTargetIfAble(rc, target, me);



        TripleTarget localTargets = RobotPlayer.acquireLocalTargets(rc, target, enemies, me);

        MapLocation primaryTarget = localTargets.primary;
        MapLocation secondaryTarget = localTargets.secondary;
        MapLocation tertiaryTarget = localTargets.tertiary;

        // if (rc.getRoundNum() < 45) {
        //     tertiaryTarget = rc.adjacentLocation(RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)]);
        // }


        if (rc.senseNearbyRobots(2, rc.getTeam()).length > 4) {
            RobotPlayer.move2(rc, primaryTarget, 3);
        }
        if (rc.canAttack(primaryTarget)) {
            rc.attack(primaryTarget);
            if (sharedArrayIndex != -1) {
                RobotPlayer.addLocationToSharedArray(rc, primaryTarget, 0, sharedArrayIndex);
            }
        }
        if (rc.canAttack(secondaryTarget)) {
            rc.attack(secondaryTarget);
        }
        if (rc.canAttack(tertiaryTarget)) {
            rc.attack(tertiaryTarget);
        }

        // if (rc.senseNearbyRobots(-1, rc.getTeam()).length < 5 /* && rc.canSenseLocation(primaryTarget) && rc.canSenseRobotAtLocation(primaryTarget) && rc.senseRobotAtLocation(primaryTarget).type.equals(RobotType.SOLDIER) */ ) {
        //     RobotPlayer.move2(rc, rc.adjacentLocation(me.directionTo(primaryTarget).opposite()).add(me.directionTo(primaryTarget)), 3);
        // }

        if (rc.isActionReady() && rc.isMovementReady()) {

            if (healing || rc.getHealth() < (rc.getType().health / 4)) {
                tertiaryTarget = repairLocation;
                healing = true;
            }

            if (rc.getHealth() == rc.getType().health) {
                healing = false;
            }

            // Experimental move.
            int recursionLimit = 4;
            int startTime = Clock.getBytecodeNum();
            if (Clock.getBytecodesLeft() <= longestTime + 1000) {
                recursionLimit = 3;
            }
            RobotPlayer.move2(rc, tertiaryTarget, recursionLimit);
            int end = Clock.getBytecodeNum();

            if ((end - startTime) > longestTime) {
                longestTime = (end - startTime);
            }
            rc.setIndicatorString("" + longestTime);

            // Fall back to simple move incase other move doesn't work.
            RobotPlayer.move(rc, tertiaryTarget);


            // RobotPlayer.move(rc, tertiaryTarget);

            if (rc.canAttack(tertiaryTarget)) {
                rc.attack(tertiaryTarget);
            }
        } else {
            RobotPlayer.stepOffRubble(rc, me);
        }
        if (!rc.isActionReady() && rc.isMovementReady()) {
            RobotPlayer.move(rc, rc.adjacentLocation(me.directionTo(primaryTarget).opposite()));
        }
        
        // rc.setIndicatorLine(me, target, 0, 1, 0);
    }
}