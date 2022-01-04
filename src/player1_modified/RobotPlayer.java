package player1_modified;

import battlecode.common.*;
import java.util.Random;


public strictfp class RobotPlayer {

    static int turnCount = 0;

    static final Random rng = new Random();

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };


    static MapLocation defaultExploreTile;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        int mapHeight = rc.getMapHeight();
        int mapWidth = rc.getMapWidth();
        MapLocation startingLoc = rc.getLocation();
        
        int newX = mapWidth - startingLoc.x;
        int newY = mapHeight - startingLoc.y;
        int translateX = newX - startingLoc.x;
        int translateY = newY - startingLoc.y;
        defaultExploreTile = startingLoc;
        defaultExploreTile = defaultExploreTile.translate(translateX, translateY);

        while (true) {

            turnCount += 1;  // We have now been alive for one more turn!
            System.out.println("Age: " + turnCount + "; Location: " + rc.getLocation());


            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                switch (rc.getType()) {
                    case ARCHON:     runArchon(rc);  break;
                    case MINER:      runMiner(rc);   break;
                    case SOLDIER:    runSoldier(rc); break;
                    case LABORATORY: // Examplefuncsplayer doesn't use any of these robot types below.
                    case WATCHTOWER: // You might want to give them a try!
                    case BUILDER:
                    case SAGE:       break;
                }
            } catch (GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    static void runArchon(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        // Build a robot in that direction.
        
        // in the early game, we will build more miners
        // in the later game, we will build more soldiers

        // use a sigmoid to determine the probability of building a miner
        // the closer we are to the end of the game, the less likely we are to build a miner
        double prob = 1.0 / (1.0 + Math.pow(1.05, (turnCount - 50)));
        boolean buildMiner = rng.nextDouble() < prob;
        
        if (buildMiner && rc.canBuildRobot(RobotType.MINER, dir)) {
            rc.buildRobot(RobotType.MINER, dir);
        } else if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
            rc.buildRobot(RobotType.SOLDIER, dir);
        }
    }

    static void runMiner(RobotController rc) throws GameActionException {
        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        // Sensing and moving
        MapLocation[] locationsAroundMe = rc.getAllLocationsWithinRadiusSquared(me, 20);

        MapLocation moveTarget = defaultExploreTile;

        for (int i = 0; i < locationsAroundMe.length; i++) {
            if (me.distanceSquaredTo(moveTarget) <= 1) {
                break;
            }
            MapLocation loc = locationsAroundMe[i];
            if (rc.senseLead(loc) > 0) {
                if (me.distanceSquaredTo(loc) < me.distanceSquaredTo(moveTarget)) {
                    moveTarget = loc;
                }
            }
        }

        Direction dir = me.directionTo(moveTarget);

        rc.setIndicatorString(defaultExploreTile.toString());

        while (true) {
            if (rc.canMove(dir)) {
               rc.move(dir);
               break;
            } else {
                dir = dir.rotateLeft();
                if (dir == me.directionTo(moveTarget)) {
                    break;
                }
            }
        }
    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runSoldier(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();

        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }

        MapLocation moveTarget = defaultExploreTile;
        Direction dir = me.directionTo(moveTarget);
        rc.setIndicatorString(defaultExploreTile.toString());

        while (true) {
            if (rc.canMove(dir)) {
               rc.move(dir);
               break;
            } else {
                dir = dir.rotateLeft();
                if (dir == me.directionTo(moveTarget)) {
                    break;
                }
            }
        }
    }
}
