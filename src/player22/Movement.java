package player22;

import battlecode.common.*;

public class Movement {
    static boolean move(RobotController rc, MapLocation target, MapLocation lastLastLocation, int recursionLevel) throws GameActionException {
        
        if (Clock.getBytecodesLeft() > 6300) {
            Direction dir = AdvancedMove.getBestDir(rc, target);
            if (dir != null && !rc.adjacentLocation(dir).equals(lastLastLocation)) {
                if (rc.canMove(dir)) {
                    rc.move(dir);
                }
            }
        } else {
            RobotPlayer.move2(rc, target, 2);
        }
        
        if (rc.isMovementReady()) {
            return false;
        }
        return true;
    }
}
