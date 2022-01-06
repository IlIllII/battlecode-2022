package player10;

import battlecode.common.*;


strictfp class SoldierStrategy {
    static int x = RobotPlayer.rng.nextInt(RobotPlayer.mapWidth - 1);
    static int y = RobotPlayer.rng.nextInt(RobotPlayer.mapHeight - 1);
    static MapLocation backupTarget = new MapLocation(x, y);

    static void run(RobotController rc) throws GameActionException {
        int[] sharedArray = RobotPlayer.getSharedArray(rc);

        boolean weAreDefending = true;
        MapLocation target;
        MapLocation me = rc.getLocation();
        int globalTarget = sharedArray[RobotPlayer.DEFEND_LOCATION];
        // if the global target is empty, read from attack instead
        if (globalTarget == 0) {
            globalTarget = sharedArray[RobotPlayer.ATTACK_LOCATION];
            weAreDefending = false;
        }
        // if there is nothing to attack, move to backup target
        if (globalTarget == 0) {
            target = backupTarget;
        } else {
            // we're attacking, so we destructure bits
            // to get the target location
            int xCoord = (globalTarget & (63 << 6)) >> 6;
            int yCoord = (63 & globalTarget);
            target = new MapLocation(xCoord, yCoord);
        }
        
        // If global target is dead, we set it to 0.
        Team us = rc.getTeam();
        if (!weAreDefending && rc.canSenseLocation(target)) {
            if ((rc.canSenseRobotAtLocation(target) && (rc.senseRobotAtLocation(target).team == us)) || !rc.canSenseRobotAtLocation(target)) {
                RobotPlayer.addLocationToSharedArray(rc, new MapLocation(0, 0), 0, 0);
            }
        }

        if (rc.getRoundNum() < 90) {
            int x = me.x + (RobotPlayer.rng.nextInt(20) - 10);
            int y = me.y + (RobotPlayer.rng.nextInt(20) - 10);
            target = new MapLocation(x, y);
        }

        if (sharedArray[RobotPlayer.FIRST_SOLDIER_TARGET] != 0) {
            int minDistance = 3600;
            for (int i = 2; i < sharedArray.length; i++) {
                if (sharedArray[i] != 0) {
                    int bitvector = sharedArray[i];
                    int x = (bitvector & (63 << 6)) >> 6;
                    int y = (63 & bitvector);
                    MapLocation loc = new MapLocation(x, y);
                    if (me.distanceSquaredTo(loc) < minDistance) {
                        target = loc;
                        minDistance = me.distanceSquaredTo(loc);
                        if (rc.canAttack(target)) {
                            rc.attack(target);
                        }
                    }
                }
            }
        }

        if (me.isWithinDistanceSquared(target, rc.getType().actionRadiusSquared)) {
            if (rc.canAttack(target)) {
                rc.attack(target);
            }
        }

        if (target.x == 0 && target.y == 0) {
            // int x = RobotPlayer.rng.nextInt(rc.getMapWidth() - 1);
            // int y = RobotPlayer.rng.nextInt(rc.getMapHeight() - 1);
            // target = new MapLocation(x, y);
            target = backupTarget;
        }



        boolean moving = true;
        boolean attacking = false;

        RobotInfo[] allies = rc.senseNearbyRobots(-1, us);
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, us.opponent());

        if (rc.isActionReady()) {
            RobotType enemyType = RobotType.MINER;
            if (enemies.length > 0) {
                for (RobotInfo enemy : enemies) {
                    if (enemy.type.equals(RobotType.ARCHON)) {
                        RobotPlayer.addLocationToSharedArray(rc, enemy.location, 0, 0);
                    }
                    if (me.distanceSquaredTo(target) >= me.distanceSquaredTo(enemy.location)) {
                        if (enemy.type.equals(RobotType.SOLDIER)) {
                            target = enemy.location;
                            enemyType = RobotType.SOLDIER;
                        } else if (!enemyType.equals(RobotType.SOLDIER)) {
                            target = enemy.location;
                        }
                        if (me.distanceSquaredTo(target) < RobotType.SOLDIER.actionRadiusSquared) {
                            attacking = true;
                            if (allies.length < enemies.length) {
                                moving = false;
                            }
                            for (int i = 2; i < sharedArray.length; i++) {
                                if (sharedArray[i] == 0) {
                                    RobotPlayer.addLocationToSharedArray(rc, target, 0, i);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        
        if (attacking) {
            if (rc.canAttack(target)) {
                rc.attack(target);
            }
        }

        rc.setIndicatorLine(me, target, 0, 1, 0);

        boolean movingToward = enemies.length + 1 < allies.length || enemies.length == 0 ? true : false;

        if (movingToward) {
            RobotPlayer.move(rc, target);
        } else {
            target = rc.adjacentLocation(me.directionTo(target).opposite());
            RobotPlayer.move(rc, target);
        }

        if (me.equals(backupTarget)) {
            int x = RobotPlayer.rng.nextInt(RobotPlayer.mapWidth - 1);
            int y = RobotPlayer.rng.nextInt(RobotPlayer.mapHeight - 1);
            backupTarget = new MapLocation(x, y);
        }
    }
}
