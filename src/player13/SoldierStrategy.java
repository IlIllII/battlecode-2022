package player13;

import battlecode.common.*;

strictfp class SoldierStrategy {

    static RobotType myType = RobotType.SOLDIER;
    final static int ATTACK_RADIUS_SQUARED = myType.actionRadiusSquared;
    final static int ATTACK_RADIUS_SQUARED_WITHIN_ONE_MOVE = 20;
    static int longestTime = 0;

    static void run(RobotController rc) throws GameActionException {  
        MapLocation me = rc.getLocation();
        // boolean moving = true;
        

        // Check for defensive, offensive, and random global target.
        // Guaranteed to be non-null.
        SharedArrayTargetAndIndex indexAndTarget = RobotPlayer.locateCombatTarget(rc, me);
        MapLocation target = indexAndTarget.location;
        int sharedArrayIndex = indexAndTarget.idx;
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.opponent);

        // First check if closest global target is a dangerous enemy.
        // If so, attack it.
        RobotPlayer.attackGlobalTargetIfAble(rc, target, me);



        // We want to prefer to attack soldiers, watchtowers, and sages.
        // int enemiesInRange = 0;
        // int distanceSquaredToDangerousEnemy = 100000;
        // int distanceSquaredToBenignEnemy = 100000;
        TripleTarget localTargets = RobotPlayer.acquireLocalTargets(rc, target, enemies, me);

        MapLocation primaryTarget = localTargets.primary;
        MapLocation secondaryTarget = localTargets.secondary;
        MapLocation tertiaryTarget = localTargets.tertiary;


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

        if (rc.isActionReady() && rc.isMovementReady()) {

            // Experimental move.
            int recursionLimit = 4;
            int startTime = Clock.getBytecodeNum();
            if (Clock.getBytecodesLeft() <= longestTime + 100) {
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
        
        // rc.setIndicatorLine(me, target, 0, 1, 0);
    }
}