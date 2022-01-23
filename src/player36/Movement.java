package player36;

import battlecode.common.*;

public class Movement {
    final static int MINIMUM_BFS_COST = 2600;
    final static int FULL_BFS_COST = 4700;
    final static int THREE_QUARTER_BFS_COST = 3700;
    final static int HALF_BFS_BFS_COST = MINIMUM_BFS_COST;
    final static int REALLY_HIGH_RUBBLE_VALUE = 200;


    private static Direction getBestDirectionUsingBFS(RobotController rc, MapLocation target, int bytecodeLeft) {
        Direction dir = null;
        if (bytecodeLeft > FULL_BFS_COST) {
            dir = FullBFS.getBestDir(rc, target);
        } else if (bytecodeLeft > THREE_QUARTER_BFS_COST) {
            dir = ThreeQuarterBFS.getBestDir(rc, target);
        } else if (bytecodeLeft > HALF_BFS_BFS_COST) {
            dir = HalfBFS.getBestDir(rc, target);
        }
        return dir;
    }

    static boolean move(RobotController rc, MapLocation target, MapLocation lastLastLocation)
            throws GameActionException {

        // * CONSIDER: May overflow bytecode when falling back
        // to simpleMove because we don't account for that cost.
        int bytecodeLeft = Clock.getBytecodesLeft();

        if (rc.getLocation().equals(target)) {
            rc.setIndicatorString("already on target");
            return false;
        }

        if (bytecodeLeft > MINIMUM_BFS_COST) {

            Direction dir = getBestDirectionUsingBFS(rc, target, bytecodeLeft);

            if (dir != null && !dir.equals(Direction.CENTER)) {
                if (!rc.getLocation().equals(lastLastLocation)) {
                    if (rc.canMove(dir)) {
                        rc.move(dir);
                        rc.setIndicatorString("using BFS");
                        return true;
                    } else {
                        rc.setIndicatorString("BFS lead to bad loc");
                        simpleMove(rc, target);
                    }
                } else {
                    rc.setIndicatorString("simpleMove d/t lastLocation");
                    simpleMove(rc, target);
                }
            } else {
                rc.setIndicatorString("simpleMove d/t bfs==null/center");
                simpleMove(rc, target);
            }

        } else {
            rc.setIndicatorString("simpleMove d/t bytecode");
            simpleMove(rc, target);
            return true;
        }
        // rc.setIndicatorString("didn't move for some reason");
        return false;
    }

    static boolean simpleMove(RobotController rc, MapLocation target) throws GameActionException {

        if (!rc.isMovementReady()) {
            return false;
        }

        MapLocation myLoc = rc.getLocation();
        Direction dir = myLoc.directionTo(target);
        int numDirections = 8;
        for (int i = numDirections; --i >= 0;) {

            if (rc.canMove(dir)) {
                rc.move(dir);
                return true;
            }
            dir = Statics.rotateLeft ? dir.rotateLeft() : dir.rotateRight();
        }
        return false;
    }

    static boolean move(RobotController rc, MapLocation target, MapLocation lastLastLocation, int recursionLevel, boolean dangerClose) throws GameActionException {
        int bytecodeLeft = Clock.getBytecodesLeft();
        if (bytecodeLeft > 2600) {
            Direction dir = null;

            if (bytecodeLeft > 4700) {
                dir = AdvancedMove1.getBestDir(rc, target);
            } else if (bytecodeLeft > 3700) {
                dir = AdvancedMove2.getBestDir(rc, target);
            } else if (bytecodeLeft > 2600) {
                dir = AdvancedMove3.getBestDir(rc, target);
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
                dir = AdvancedMove1.getBestDir(rc, target);
            } else if (bytecodeLeft > 3700) {
                dir = AdvancedMove2.getBestDir(rc, target);
            } else if (bytecodeLeft > 2600) {
                dir = AdvancedMove3.getBestDir(rc, target);
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
