package player13;

import battlecode.common.*;

public class MoveUnrolled {

    static void move2(RobotController rc, MapLocation target, int recursionLimit) throws GameActionException {
        simpleShortestPath(rc, rc.getLocation(), target, 0, 0, recursionLimit);
    }

    static int simpleShortestPath(RobotController rc, MapLocation start, MapLocation target, int currentWeight, int recursionLevel, int recursionLimit) throws GameActionException {
        if (recursionLevel == recursionLimit) {
            int finalWeight = currentWeight + rc.senseRubble(start) + (int)Math.sqrt(start.distanceSquaredTo(target));
            return finalWeight;
        }

        currentWeight += rc.senseRubble(start);

        
        Direction initialDir = start.directionTo(target);
        Direction leftDir = initialDir.rotateLeft();
        Direction rightDir = initialDir.rotateRight();
        MapLocation leftTile = rc.adjacentLocation(leftDir);
        int leftWeight = leftTile.distanceSquaredTo(target);
        if (rc.canSenseLocation(leftTile) && !rc.canSenseRobotAtLocation(leftTile)) {
            leftWeight = simpleShortestPath(rc, leftTile, target, currentWeight, recursionLevel + 1, recursionLimit);
        }
        
        MapLocation centerTile = rc.adjacentLocation(initialDir);
        int centerWeight = centerTile.distanceSquaredTo(target);
        if (rc.canSenseLocation(centerTile) && !rc.canSenseRobotAtLocation(centerTile)) {
            centerWeight = simpleShortestPath(rc, centerTile, target, currentWeight, recursionLevel + 1, recursionLimit);
        }
        
        MapLocation rightTile = rc.adjacentLocation(rightDir);
        int rightWeight = centerTile.distanceSquaredTo(target);
        if (rc.canSenseLocation(rightTile) && !rc.canSenseRobotAtLocation(rightTile)) {
            rightWeight = simpleShortestPath(rc, rightTile, target, currentWeight, recursionLevel + 1, recursionLimit);
        }

        if (recursionLevel != 0) {
            return Math.min(Math.min(leftWeight, centerWeight), rightWeight);
        }

        if (centerWeight <= leftWeight && centerWeight <= rightWeight) {
            if (rc.canMove(initialDir)) {
                rc.move(initialDir);
                return 0;
            }
        }
        if (leftWeight <= centerWeight && leftWeight <= rightWeight) {
            if (rc.canMove(leftDir)) {
                rc.move(leftDir);
                return 0;
            }
        }
        if (rc.canMove(rightDir)) {
            rc.move(rightDir);
            return 0;
        }
        return 1;
    }


    static void move3(RobotController rc, MapLocation start, MapLocation target) {
        
    }
}
