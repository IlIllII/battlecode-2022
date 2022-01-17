package player33;

import battlecode.common.*;

public class Movement {
    static boolean move(RobotController rc, MapLocation target, MapLocation lastLastLocation, int recursionLevel, boolean dangerClose) throws GameActionException {
        int bytecodeLeft = Clock.getBytecodesLeft();
        if (bytecodeLeft > 2600) {
            Direction dir = null;

            if (bytecodeLeft > 4700) {
                dir = AdvancedMove2.getBestDir(rc, target);
            } else if (bytecodeLeft > 3700) {
                dir = AdvancedMove3.getBestDir(rc, target);
            } else if (bytecodeLeft > 2600) {
                dir = AdvancedMove4.getBestDir(rc, target);
            }

            if (dir != null && !dir.equals(Direction.CENTER) && !rc.getLocation().equals(lastLastLocation)) {
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
            // System.out.println("Moving w/ move2");
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

    static boolean moveButDontStepOnRubble(RobotController rc, MapLocation target, int recursionLevel, boolean dangerClose) throws GameActionException {
        int bytecodeLeft = Clock.getBytecodesLeft();
        if (bytecodeLeft > 2600) {
            Direction dir = null;

            if (bytecodeLeft > 4700) {
                dir = AdvancedMove2.getBestDir(rc, target);
            } else if (bytecodeLeft > 3700) {
                dir = AdvancedMove3.getBestDir(rc, target);
            } else if (bytecodeLeft > 2600) {
                dir = AdvancedMove4.getBestDir(rc, target);
            }

            if (dir != null && !dir.equals(Direction.CENTER)) {
                if (rc.canMove(dir)) {
                    if (rc.senseRubble(rc.getLocation()) >= rc.senseRubble(rc.adjacentLocation(dir))) {
                        rc.move(dir);
                    }
                }
            }
        } else {
            RobotPlayer.move(rc, target);
        }
        
        if (rc.isMovementReady()) {
            return false;
        }
        return true;
    }
}
