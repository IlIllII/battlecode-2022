package player36;

import battlecode.common.*;


strictfp class Sage {

    static RobotType myType = RobotType.SAGE;
    static MapLocation backupLocation = RobotPlayer.getRandomMapLocation();
    static MapLocation lastLocation = new MapLocation(0, 0);
    static MapLocation lastLastLocation = new MapLocation(0, 0);
    static MapLocation lastLastLastLocation = new MapLocation(0, 0);
    static boolean retreating = false;
    static  int round = 0;

    static void  attackOrEnvision(RobotController rc, RobotInfo[] enemyInaction) throws GameActionException {
        if (enemyInaction.length == 0) return;
        int buildingCount = 0;
        int droidCount = 0;
        int dist = 999999;
        int tempDist = 0;
        MapLocation closestTarget = null;
        MapLocation tempTarget;
        boolean archon = false;

        for (RobotInfo info : enemyInaction) {
            tempTarget = info.location;
            tempDist = rc.getLocation().distanceSquaredTo(tempTarget);
            if (tempDist < dist) {
                closestTarget = tempTarget;
                dist = tempDist;
            }
            // count building and droid
           if (info.getType() == RobotType.LABORATORY || info.getType() == RobotType.WATCHTOWER || info.getType() == RobotType.LABORATORY) {
               buildingCount++;
           }else{
               droidCount++;
           }

           if (info.getType() == RobotType.ARCHON) archon = true;
       }

        if (droidCount > 5) {
            if (rc.canEnvision(AnomalyType.CHARGE)){
                rc.envision(AnomalyType.CHARGE);
            }
        } else if (buildingCount > 4) {
            if (rc.canEnvision(AnomalyType.FURY)){
                rc.envision(AnomalyType.FURY);
            }
        } else {
            if (rc.canAttack(closestTarget)){
                rc.attack(closestTarget);
            }
        }
    }

    static void run(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(myType.visionRadiusSquared, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(myType.visionRadiusSquared, rc.getTeam());
        RobotInfo[] enemyInaction = rc.senseNearbyRobots(myType.actionRadiusSquared, rc.getTeam().opponent());
        boolean enemySoldierClose = false;
        boolean attacked = false;
        boolean fallingBack = false;
        int currentHealth = rc.getHealth();
        round++;
        // locations
        MapLocation myLocation = rc.getLocation();
        MapLocation offensiveTarget = Targeting.getOffensiveTarget(rc);
        MapLocation defensiveTarget = Targeting.getDefensiveTarget(rc);
        MapLocation target = Targeting.getTargetFromGlobalAndLocalEnemyLocations(rc, enemies, backupLocation);
        // check if retreat
        if (currentHealth <= 30 && round < 40) {
            retreating = true;
        }
        if (currentHealth > 80) {
            retreating = false;
        }

        // last location
        if (!lastLocation.equals(myLocation)) {
            lastLastLastLocation = lastLastLocation;
            lastLastLocation = lastLocation;
            lastLocation = myLocation;
        }
        // target selection
        // prioritize defense over offense
        if (target == null) {
            target = backupLocation;
        }

        if (target.equals(backupLocation) && offensiveTarget != null) {
            target = offensiveTarget;
        }

        if (defensiveTarget != null && (2 * myLocation.distanceSquaredTo(target)) > myLocation.distanceSquaredTo(defensiveTarget)) {
            target = defensiveTarget;
        }

        RobotInfo[] closeByTeammates = rc.senseNearbyRobots(2, rc.getTeam());
        if (!retreating && closeByTeammates.length > 4 ) {
            RobotPlayer.move2(rc, target, 2);
        }

        // check if can envision or attack
        if (rc.isActionReady()) {
            attackOrEnvision(rc, enemyInaction);
            attacked = true;
        }
        /////////////////////////////////////////////////////////
        for (RobotInfo info : enemies){
            if (info.getType() == RobotType.SAGE || info.getType() == RobotType.SOLDIER) {
                enemySoldierClose = true;
            }
        }

        if (!rc.isActionReady() && enemySoldierClose) {
            int x = (target.x - myLocation.x);
            int y = (target.y - myLocation.y);
            target = new MapLocation(myLocation.x - x, myLocation.y - y);
            fallingBack = true;
        }

        // Movement dispatch
        if (rc.isMovementReady()) {
            if (!retreating) { // We are advancing.
                if (!fallingBack) {
                    if (attacked || rc.getLocation().distanceSquaredTo(target) <= rc.getType().actionRadiusSquared) {
                        // We do not want to move onto rubble if we are in combat, but
                        // we also want to move if there aren't enemies in attack range.
                        Movement.moveButDontStepOnRubble(rc, target, 2, enemySoldierClose);
                    } else {
                        // If we haven't attacked yet it means we are advancing so we move like normal.
                        Movement.move(rc, target, lastLastLastLocation, 2, enemySoldierClose);
                        RobotPlayer.move(rc, target);
                    }
                } else {
                    // Special move where we do not fall back onto rubble.
                    Movement.fallingBackMove(rc, target);
                }
            }else {
                MapLocation retreatTarget = Communications.getNearestArchonLocation(rc, myLocation);

                if (retreatTarget == null) {
                    retreatTarget = backupLocation;
                }

                if (retreatTarget != null) {
                    if (defensiveTarget != null && myLocation.distanceSquaredTo(target) > 64) {
                        if (!fallingBack) {
                            Movement.move(rc, target, lastLastLastLocation, 2, enemySoldierClose);
                            RobotPlayer.move(rc, target);
                        } else {
                            Movement.fallingBackMove(rc, target);
                        }
                    } else {
                        Movement.move(rc, retreatTarget, lastLastLocation, 2, enemySoldierClose);
                    }

                } else {
                    System.out.println("Couldnt find retreat target");
                }
            }
        }

        if (rc.isActionReady()) {
            attackOrEnvision(rc, enemyInaction);
            attacked = true;
        }

        }
}
