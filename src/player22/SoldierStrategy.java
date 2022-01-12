package player22;

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
    static boolean retreating = false;
    static boolean selfDestructing = false;
    static MapLocation backupRetreatTarget;
    // static MapLocation repairLoc = null;
    // static boolean foundRepairSpot = false;
    

    static void run(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.opponent);
        MapLocation target = Targeting.getTargetFromGlobalAndLocalEnemyLocations(rc, enemies, backupLocation);
        MapLocation offensiveTarget = Targeting.getOffensiveTarget(rc);
        MapLocation defensiveTarget = Targeting.getDefensiveTarget(rc);
        
        aliveTime++;
        if (aliveTime == 1) {
            backupRetreatTarget = me;
        }
        
         
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


        // * Checking health in order to retreat
        if (rc.getHealth() <= rc.getType().health / 4) {
            // retreating = true;
        }


        // * Advance when surrounded by allies
        if (rc.senseNearbyRobots(2, rc.getTeam()).length > 4 && !retreating) {
            RobotPlayer.move2(rc, target, 2);
        }


        // * If attacking is possible, attack and then set target to retreating.
        if (rc.isActionReady()) {
            if (rc.canAttack(target)) {
                rc.attack(target);

                int x = (target.x - me.x);
                int y = (target.y - me.y);
                target = new MapLocation(me.x - x, me.y - y);
            }
        }


        // * Dispatch differential movement.
        if (rc.isMovementReady()) {
            if (!retreating) { // Attacking
                Movement.move(rc, target, lastLastLocation, 2);
                lastLastLocation = lastLocation;
                lastLocation = rc.getLocation();
            } else { // Retreating
                if (!selfDestructing) {
                    MapLocation retreatTarget = Comms.getNearestArchonLocation(rc, me);

                    if (retreatTarget == null) {
                        retreatTarget = backupRetreatTarget;
                    }

                    if (retreatTarget != null) {
                        Movement.move(rc, retreatTarget, lastLastLocation, 2);
                        lastLastLocation = lastLocation;
                        lastLocation = rc.getLocation();
                        if (me.distanceSquaredTo(retreatTarget) <= 9) {
                            selfDestructing = true;
                        }

                    } else {
                        System.out.println("Couldnt find retreat target");
                    }
                } else {
                    MapLocation nearestFreeTile = RobotPlayer.findNearestEmptyTile(rc, me);
                    if (nearestFreeTile != null) {
                        RobotPlayer.move2(rc, nearestFreeTile, 2);
                        if (rc.getLocation().equals(nearestFreeTile)) {
                            System.out.println("Disintegrating");
                            rc.disintegrate();
                        }

                    } else {
                        RobotPlayer.move2(rc, target, 2);
                    }
                }
            }

        }

        // * Check for a cheeky attack on the tail end
        if (rc.isActionReady()) {
            if (rc.canAttack(target)) {
                rc.attack(target);
            }
        }

        rc.setIndicatorLine(rc.getLocation(), target, 0, 0, 0);
    }
}