package player20test;

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
    // static MapLocation repairLoc = null;
    // static boolean foundRepairSpot = false;
    


    static BFS bfs;

    static void run(RobotController rc) throws GameActionException { 
        MapLocation me = rc.getLocation();

        if (me.distanceSquaredTo(backupLocation) <= 4) {
            backupLocation = RobotPlayer.getRandomMapLocation();
        }

        aliveTime++;
        if (aliveTime == 2) {
            repairLocation = me;
        }
        

        CombatTargetAndEnemyLocs combatTargetAndLocs = RobotPlayer.locateCombatTarget(rc, me, backupLocation);
        MapLocation target = combatTargetAndLocs.target;
        EnemyLocation[] enemyLocations = combatTargetAndLocs.locations;


        RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.opponent);

        RobotPlayer.attackGlobalTargetIfAble(rc, target, me);


        TripleTarget localTargets = RobotPlayer.acquireLocalTargets(rc, target, enemies, me);

        MapLocation primaryTarget = localTargets.primary;
        MapLocation secondaryTarget = localTargets.secondary;
        MapLocation tertiaryTarget = localTargets.tertiary;


        if (rc.senseNearbyRobots(2, rc.getTeam()).length > 4) {
            RobotPlayer.move2(rc, primaryTarget, 2);
        }
        if (rc.canAttack(primaryTarget)) {
            rc.attack(primaryTarget);
            Comms.setEnemyLocation(rc, primaryTarget, enemyLocations);
        }
        if (rc.canAttack(secondaryTarget)) {
            rc.attack(secondaryTarget);
        }
        if (rc.canAttack(tertiaryTarget)) {
            rc.attack(tertiaryTarget);
        }


        if (rc.isActionReady() && rc.isMovementReady()) {  
            try {
                if (Clock.getBytecodesLeft() > 6500) {
                    Direction dir = AdvancedMoveCopy.getBestDir(rc, tertiaryTarget);
    
                    if (dir != null && !dir.equals(Direction.CENTER) && rc.canMove(dir)) {
                        if (!rc.adjacentLocation(dir).equals(lastLocation)) {
                            lastLocation = rc.getLocation();
                            rc.move(dir);
                        } else {
                            if (rc.canMove(rc.getLocation().directionTo(tertiaryTarget))) {
                                RobotPlayer.move(rc, tertiaryTarget);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                //TODO: handle exception
                System.out.println("Move returned null");;
            }

            
            // Fall back to simple move incase other move doesn't work.
            RobotPlayer.move2(rc, tertiaryTarget, 3);
            
            
            // RobotPlayer.move(rc, tertiaryTarget);

            if (rc.canAttack(tertiaryTarget)) {
                rc.attack(tertiaryTarget);
            }
        } 

        if (!rc.isActionReady() && rc.isMovementReady()) {
            MapLocation retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
            retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
            retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
            retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
            retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());

            RobotPlayer.lowRubbleMove(rc, retreatMove);
        }
        
        rc.setIndicatorLine(me, tertiaryTarget, 1000, 0, 1000);
    }
}