package player2;

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

    static MapLocation[] exploreTiles;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

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

        

        // Finding enemy archon positions and putting them in our shared array. This code puts up
        // to four enemy archon coords in the first 4 entries in the shared array. We need at most 6 bits
        // to encode a dimension of length 60, which is the max a map can be. Right now I am using 8 bits for
        // x and y coords, but we can also use the last two bits to indicate whether the enemy archon is alive
        // or dead. As a reminder, each entry in our shared array stores a 16 bit integer.
        if (rc.getType() == RobotType.ARCHON) {
            for (int i=0; i < 4; i++) {
                if (rc.readSharedArray(i) == 0) {
                    int bitvectorCoord = 0;
                    int xBit = newX;
                    int yBit = newY;
                    bitvectorCoord += xBit << 8;
                    bitvectorCoord += yBit;
                    try {
                        rc.writeSharedArray(i, bitvectorCoord);
                    } catch (Exception e) {
                        System.out.println(rc.getType() + " Exception");
                        e.printStackTrace();
                    }
                }
            }
        }

        // If the unit is a soldier, we want to extract enemy archon coordinates from our shared
        // array. We will have to extend this so the soldiers can target switch when we destory an archon.
        if (rc.getType() == RobotType.SOLDIER) {
            int xTarget;
            int yTarget;
            int archonCoord = rc.readSharedArray(0);
            xTarget = archonCoord >> 8;
            yTarget = (0xFF & archonCoord);
            System.out.println("archonCoord: " + archonCoord + ", xTarget: " + xTarget + ", yTarget: " + yTarget);
            defaultExploreTile = new MapLocation(xTarget, yTarget);
        }

        while (true) {

            turnCount += 1;  // We have now been alive for one more turn!
            // System.out.println("Age: " + turnCount + "; Location: " + rc.getLocation());


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
        // the closer we are to the end of the game, the less likely we are 
        // to build miners 

        // these are paramters we can screw with
        // if we feel really fancy, we could try 
        // some sort of RL to determine the best parameters
        final double base = 1.05;
        final double exponent_modifier = 50;
        
        // prob is y, turnCount is x on this graph 
        // https://www.desmos.com/calculator/igrdgmqx13

        double prob = 1.0 / (1.0 + Math.pow(base, (turnCount - exponent_modifier)));
        boolean buildMiner = rng.nextDouble() < prob;

        // either we try to build a miner or a soldier
        if (buildMiner && rc.canBuildRobot(RobotType.MINER, dir)) {
            rc.buildRobot(RobotType.MINER, dir);
        } else if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
            rc.buildRobot(RobotType.SOLDIER, dir);
        }

        // In the future we might
        // try to parameterize these constants based on the size of the map, turn count,
        // and number of archons. Intuitively I would think larger maps take longer so we might want
        // to mine more resoures comapared to smaller maps. Maybe there is a way to sense how much lead
        // is on a map and produce miners based on that?
        
        // we could track stuff about lead in the global bitvector?
    }

    static void runMiner(RobotController rc) throws GameActionException {
        // THINGS TO CHANGE:
        // miners should run from enemy soldiers and sages.

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

        // This is sensing logic. We basically sense all squares around us and iterate over them
        // until we find a tile with lead on it that's at most 1 square away, at which point we end
        // our iteration early to save some computation. We set the closest lead tile to our move target.
        //
        // This has a propensity to cause backups because several miners will try to mine the same
        // tile but can't all fit. Maybe we should implement logic to make miners move forward if
        // there are 3 miners around them or something.
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

        // Moving. We try to move toward a destination tile, which is either a global tile
        // defined by our defaultExploreTile, or it is a local tile that has lead on it that
        // we found just prior to this.
        //
        // If our path is blocked, we simply rotate and try again. We continue this until we
        // rotate back to the direction we started and then give up. We could try more sophisticated
        // path finding but this may cost computation, otherwise we could randomize our rotation
        // direction at instantiation so half our droids rotate left and half rotate right.
        Direction dir = me.directionTo(moveTarget);
        rc.setIndicatorString(defaultExploreTile.toString());

        // We can make this a for loop with Directions.length iterations so we don't have
        // to check whether we rotated back to the beginning every time. Saves on like 30 bytecodes.
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


    static void runSoldier(RobotController rc) throws GameActionException {
        // THINGS TO CHANGE:
        // 1. soldiers should try to move toward enemies (sometimes)
        //     - ideally, soldiers should count allied soldiers and subtract enemy soldiers,
        //       attacking when the number of enemies is less than the number of allies
        // 2. in order not to get in each others way during a fight, soldiers 
        //    could try to move perpendicular to enemies with some low probability


        // Try to attack someone. We should change this to prioritize enemy
        // soldiers or watch towers.
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }

        // Moving the same way as miners.
        MapLocation me = rc.getLocation();
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
