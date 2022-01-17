package player33;

import battlecode.common.*;

strictfp class SoldierStrategyBigMap {

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
    static int lastTurnsHealth = 0;
    static int selfDestructTimer = 0;
    static int healingCutoff = 25;
    

    static void run(RobotController rc) throws GameActionException {

        // * Setting main variables
        boolean fallingBack = false;
        boolean enemySoldierClose = false;
        boolean attacked = false;
        int currentHealth = rc.getHealth();
        MapLocation me = rc.getLocation();
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, RobotPlayer.opponent);
        RobotInfo[] allies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());
        MapLocation offensiveTarget = Targeting.getOffensiveTarget(rc);
        MapLocation defensiveTarget = Targeting.getDefensiveTarget(rc);
        MapLocation target = Targeting.getTargetFromGlobalAndLocalEnemyLocations(rc, enemies, backupLocation);



        // * These values vary based on current conditions
        aliveTime++;
        if (aliveTime == 1) {
            backupRetreatTarget = me;
            if (RobotPlayer.mapHeight < 30 && RobotPlayer.mapWidth < 30) {
                healingCutoff = 25;
            } else if (RobotPlayer.mapHeight < 30 || RobotPlayer.mapWidth < 30) {
                healingCutoff = 20;
            } else if (RobotPlayer.mapHeight < 40 || RobotPlayer.mapWidth < 40) {
                healingCutoff = 17;
            } else if (RobotPlayer.mapHeight < 50 || RobotPlayer.mapWidth < 50) {
                healingCutoff = 15;
            } else {
                healingCutoff = 12;
            }
            healingCutoff = 25;
        }
        if (!lastLocation.equals(me)) {
            lastLastLastLocation = lastLastLocation;
            lastLastLocation = lastLocation;
            lastLocation = me;
        }
        if (selfDestructing) {
            selfDestructTimer++;
        }
        if (enemies.length > 0 || lastTurnsHealth != currentHealth) {
            selfDestructing = false;
            selfDestructTimer = 0;
        }
        if (currentHealth <= healingCutoff) { // TODO Should this be higher even?
            retreating = true;
        }
        if (currentHealth > 48) { // leave wiggle room heal as we move away
            retreating = false;
            selfDestructing = false;
            selfDestructTimer = 0;
        }
        lastTurnsHealth = currentHealth;
        

        // * Are there dangerous soldiers nearby?
        // We care because we want to retreat after attacking them.
        // TODO: This can be more efficient - we iterate over enemies elsewhere.
        for (RobotInfo enemy : enemies) {
            if (enemy.type == RobotType.SOLDIER) {
                enemySoldierClose = true;
                break;
            }
        }
        
        
        // * Targeting cascade
        // Fall back to backup target as default. Might not be needed d/t new targeting scheme.
        if (target == null) {
            backupLocation = RobotPlayer.getRandomMapLocation();
            target = backupLocation;
        }
        // If offensive or defensive targets exist, we will prefer those.
        if (target.equals(backupLocation)) {
            if (offensiveTarget != null) {
                target = offensiveTarget;
            }
            if (defensiveTarget != null) {
                target = defensiveTarget;
            }
        }
        // We want to prefer defense when not engaged but still pursue enemies attacking our archon.
        if (defensiveTarget != null && me.distanceSquaredTo(target) > 49 && me.distanceSquaredTo(defensiveTarget) > 36) {
            target = defensiveTarget;
        }


        // This is an optional group up before rushing
        // if (rc.getRoundNum() < 60) {
        //     target = rc.adjacentLocation(RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)]);
        // }


        // Advance when surrounded by allies.
        // TODO: This is worth investigating - may not be the best strat.
        RobotInfo[] closeByTeammates = rc.senseNearbyRobots(2, rc.getTeam());
        if (closeByTeammates.length > 4 && !retreating) {
            RobotPlayer.move2(rc, target, 2);
        }


        // Attack if able before moving.
        if (rc.isActionReady()) {
            if (rc.canAttack(target)) {
                rc.attack(target);
                attacked = true;
            }
        }

        // If we are engaged with enemy soldiers, we want to fall back
        if (!rc.isActionReady() && enemySoldierClose) {
            int x = (target.x - me.x);
            int y = (target.y - me.y);
            target = new MapLocation(me.x - x, me.y - y);
            fallingBack = true;
        }


        // * Movement dispatch.
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
            } else { // Retreating
                if (!selfDestructing) {
                    MapLocation retreatTarget = Comms.getNearestArchonLocation(rc, me);

                    if (retreatTarget == null) {
                        retreatTarget = backupRetreatTarget;
                    }

                    if (retreatTarget != null) {
                        if (defensiveTarget != null && me.distanceSquaredTo(target) > 64) {
                            if (!fallingBack) {
                                Movement.move(rc, target, lastLastLastLocation, 2, enemySoldierClose);
                                RobotPlayer.move(rc, target);
                            } else {
                                Movement.fallingBackMove(rc, target);
                            }
                        } else {
                            Movement.move(rc, retreatTarget, lastLastLocation, 2, enemySoldierClose);                                
                            if (me.distanceSquaredTo(retreatTarget) <= 25 && enemies.length == 0) {
                                selfDestructing = true;
                            }
                        }

                    } else {
                        System.out.println("Couldnt find retreat target");
                    }
                } else {
                    MapLocation nearestFreeTile = RobotPlayer.findNearestEmptyTile(rc, me);
                    if (nearestFreeTile != null && !enemySoldierClose) {
                        RobotPlayer.move2(rc, nearestFreeTile, 2);
                        if (rc.getLocation().equals(nearestFreeTile)) {
                            if (selfDestructTimer > 10) {
                                if (currentHealth < 20) {
                                    System.out.println("Disintegrating");
                                    rc.disintegrate();
                                } else {
                                    selfDestructing = false;
                                    retreating = false;
                                }
                            }
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