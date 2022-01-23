package player36;

import battlecode.common.*;


strictfp class Watchtower {
    static MapLocation backupLocation = RobotPlayer.getRandomMapLocation();

    static final RobotType myType = RobotType.WATCHTOWER;
    final static int ATTACK_RADIUS_SQUARED = myType.actionRadiusSquared;

    static void run(RobotController rc) throws GameActionException {
    }
}
