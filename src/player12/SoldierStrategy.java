package player12;

import battlecode.common.*;

strictfp class SoldierStrategy {

    static RobotType myType = RobotType.SOLDIER;
    final static int ATTACK_RADIUS_SQUARED = myType.actionRadiusSquared;
    final static int ATTACK_RADIUS_SQUARED_WITHIN_ONE_MOVE = 20;

    static void run(RobotController rc) throws GameActionException {
        /* Pseudocode:
        
        Start with global defensive target. If this is 0, use global offensive target. If this is 0,
        use our backup random target.

        Then, if there are enemies around us, check to see if any are in the shared target list.
        If one is, attack it. If not, choose the enemy soldier with the lowest health, attack it,
        and add it to the shared target list.

        If there are not enemies around us, determine the closest enemy in the shared target list
        and move toward it.

        If there are not enemies in the shared target list, we will simply move toward the global target.

        We get shared target list;

        If we are in range of a target list target, we should attack it.
        
        If not but we are in range of an unlisted enemy target, we should attack it and
        add it to the target list.

        If we are not in range of any enemy tarets, we should move toward a target in target
        list, including global target
        */
            
        
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

        if (rc.isActionReady()) {

            
            RobotPlayer.bfsMove(rc, me, tertiaryTarget);


            // RobotPlayer.move(rc, tertiaryTarget);

            if (rc.canAttack(tertiaryTarget)) {
                rc.attack(tertiaryTarget);
            }
        }
        
        // rc.setIndicatorLine(me, target, 0, 1, 0);
    }
}