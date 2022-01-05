package player8;

import battlecode.common.*;


strictfp class SoldierStrategy {
    static void run(RobotController rc) throws GameActionException {
        int globalTarget = rc.readSharedArray(1);
        boolean defense = true;
        if (globalTarget == 0) {
            globalTarget = rc.readSharedArray(0);
            defense = false;
        }
        int xCoord = (globalTarget & (63 << 6)) >> 6;
        int yCoord = (63 & globalTarget);
        MapLocation target = new MapLocation(xCoord, yCoord);
        MapLocation me = rc.getLocation();

        if (!defense && rc.canSenseLocation(target)) {
            if ((rc.canSenseRobotAtLocation(target) && (rc.senseRobotAtLocation(target).team == rc.getTeam())) || !rc.canSenseRobotAtLocation(target)) {
                int x = Math.min(RobotPlayer.rng.nextInt(rc.getMapWidth()), rc.getMapWidth() - 1);
                int y = Math.min(RobotPlayer.rng.nextInt(rc.getMapHeight()), rc.getMapHeight() - 1);
                RobotPlayer.addLocationToSharedArray(rc, new MapLocation(x, y), 0, 0);
            }
        }

        boolean moving = true;
        boolean attacking = false;

        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.opponent);
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
                    }
                }
            }
        }

        if (moving) {
            RobotPlayer.move(rc, target);
        }

        if (attacking) {
            if (rc.canAttack(target)) {
                rc.attack(target);
            }
        }
    }
}
