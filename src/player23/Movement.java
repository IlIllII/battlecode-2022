package player23;

import battlecode.common.*;

public class Movement {
    static boolean move(RobotController rc, MapLocation target, MapLocation lastLastLocation, int recursionLevel, boolean dangerClose) throws GameActionException {
        
        if (Clock.getBytecodesLeft() > 6450) {
            Direction dir = AdvancedMove.getBestDir(rc, target);
            if (dir != null && !rc.getLocation().equals(lastLastLocation)) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    // if (dangerClose && rc.senseRubble(rc.getLocation()) >= rc.senseRubble(rc.adjacentLocation(dir))) {
                    //     rc.move(dir);
                    // } else {
                    //     RobotPlayer.stepOffRubble(rc, rc.getLocation());
                    // }
                } else {
                    RobotPlayer.move(rc, rc.adjacentLocation(dir));
                }
            }
        } else {
            RobotPlayer.move2(rc, target, recursionLevel);
        }
        
        if (rc.isMovementReady()) {
            return false;
        }
        return true;
    }

    public static void fallingBackMove(RobotController rc, MapLocation target) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        Direction dir = myLoc.directionTo(target);
        int baseRubble = rc.senseRubble(myLoc);
        if (rc.canMove(dir)) {
            int[] rubbleNumbers = new int[3];

            Direction leftDir = dir.rotateLeft();
            Direction rightDir = dir.rotateRight();

            rubbleNumbers[0] = rc.canSenseLocation(rc.adjacentLocation(leftDir)) && rc.canMove(leftDir)
                    ? rc.senseRubble(rc.adjacentLocation(leftDir))
                    : 100;
            rubbleNumbers[1] = rc.canSenseLocation(rc.adjacentLocation(dir))
                    ? rc.senseRubble(rc.adjacentLocation(dir))
                    : 100;
            rubbleNumbers[2] = rc.canSenseLocation(rc.adjacentLocation(rightDir)) && rc.canMove(rightDir)
                    ? rc.senseRubble(rc.adjacentLocation(rightDir))
                    : 100;

            int minValue = rubbleNumbers[1];
            int minIdx = 1;

            for (int j = 0; j < 3; j += 2) {
                if (rubbleNumbers[j] < minValue) {
                    minValue = rubbleNumbers[j];
                    minIdx = j;
                }
            }

            if (minIdx == 0) {
                dir = dir.rotateLeft();
            } else if (minIdx == 2) {
                dir = dir.rotateRight();
            }

            if (rc.senseRubble(rc.adjacentLocation(dir)) <= baseRubble) {
                rc.move(dir);
            } else {
                RobotPlayer.stepOffRubble(rc, myLoc);
            }
        }
    }
}
