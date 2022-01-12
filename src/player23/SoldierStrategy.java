package player23;

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
    static MapLocation lastLastLastLocation = new MapLocation(0, 0);
    static int MaxMovementCost = 0;
    static boolean retreating = false;
    static boolean selfDestructing = false;
    static MapLocation backupRetreatTarget;
    // static MapLocation repairLoc = null;
    // static boolean foundRepairSpot = false;
    static int maxTargetingCost = 0;
    

    static void run(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        boolean dangerClose = false;
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, RobotPlayer.opponent);
        MapLocation offensiveTarget = Targeting.getOffensiveTarget(rc);
        MapLocation defensiveTarget = Targeting.getDefensiveTarget(rc);
        boolean fallingBack = false;

        if (!lastLocation.equals(me)) {
            lastLastLastLocation = lastLastLocation;
            lastLastLocation = lastLocation;
            lastLocation = me;
        }
        rc.setIndicatorString(lastLastLocation.toString());

        if (rc.getHealth() > 25 || enemies.length > 0) {
            selfDestructing = false;
        }

        if (rc.getHealth() > 45) {
            retreating = false;
        }

        int start = Clock.getBytecodeNum();
        MapLocation target = Targeting.getTargetFromGlobalAndLocalEnemyLocations(rc, enemies, backupLocation);
        int end = Clock.getBytecodeNum();

        maxTargetingCost = (end - start) > maxTargetingCost ? (end - start) : maxTargetingCost;
        rc.setIndicatorString("Targeting: " + maxTargetingCost);
        // MapLocation target = backupLocation;

        
        aliveTime++;
        if (aliveTime == 1) {
            backupRetreatTarget = me;
        }

        if (enemies.length > 0) {
            dangerClose = true;
        }
        
        
        // * Targeting cascade
        if (target == null) {
            backupLocation = RobotPlayer.getRandomMapLocation();
            target = backupLocation;
        }
        if (target.equals(backupLocation) || me.distanceSquaredTo(target) > rc.getType().visionRadiusSquared) {
            if (offensiveTarget != null) {
                target = offensiveTarget;
            }
            if (defensiveTarget != null) {
                target = defensiveTarget;
            }
        }

        // if (rc.getRoundNum() < 60) {
        //     target = rc.adjacentLocation(RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)]);
        // }


        // * Checking health in order to retreat
        if (rc.getHealth() <= 7) {
            retreating = true;
        }


        // * Advance when surrounded by allies
        RobotInfo[] closeByTeammates = rc.senseNearbyRobots(2, rc.getTeam());
        if (closeByTeammates.length > 4 && !retreating) {
            RobotPlayer.move2(rc, target, 2);
        }


        // * If attacking is possible, attack and then set target to retreating.
        if (rc.isActionReady()) {
            if (rc.canAttack(target)) {
                rc.attack(target);
            }
        }

        if (!rc.isActionReady()) {
            int x = (target.x - me.x);
            int y = (target.y - me.y);
            target = new MapLocation(me.x - x, me.y - y);
            fallingBack = true;
        }


        // * Dispatch differential movement.
        if (rc.isMovementReady()) {
            if (!retreating) { // Attacking
                if (!fallingBack) {
                    Movement.move(rc, target, lastLastLastLocation, 2, dangerClose);
                    RobotPlayer.move(rc, target);
                } else {
                    Movement.fallingBackMove(rc, target);
                }
            } else { // Retreating
                if (!selfDestructing) {
                    MapLocation retreatTarget = Comms.getNearestArchonLocation(rc, me);

                    if (retreatTarget == null) {
                        retreatTarget = backupRetreatTarget;
                    }

                    if (retreatTarget != null) {
                        Movement.move(rc, retreatTarget, lastLastLocation, 2, dangerClose);
                        if (me.distanceSquaredTo(retreatTarget) <= 9 && enemies.length == 0) {
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