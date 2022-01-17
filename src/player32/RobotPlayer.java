package player32;

import battlecode.common.*;

import java.util.Random;

class SharedArrayTargetAndIndex {
    MapLocation location;
    int idx;

    SharedArrayTargetAndIndex(MapLocation target, int index) {
        location = target;
        idx = index;
    }
}

class TripleTarget {
    MapLocation primary;
    MapLocation secondary;
    MapLocation tertiary;

    TripleTarget(MapLocation primaryTarget, MapLocation secondaryTarget, MapLocation tertiaryTarget) {
        primary = primaryTarget;
        secondary = secondaryTarget;
        tertiary = tertiaryTarget;
    }
}

class BFSNode {
    public int totalWeight;
    public MapLocation nodeLocation;
    public BFSNode parent;
    public MapLocation targetLocation;

    BFSNode(MapLocation nodePosition, BFSNode parentNode, int totalPathWeight, MapLocation targetLoc) {
        totalWeight = totalPathWeight;
        nodeLocation = nodePosition;
        parent = parentNode;
        targetLocation = targetLoc;
    }

    public String toString() {
        return nodeLocation.toString();
    }

    public boolean greaterThan(BFSNode other) {
        return totalWeight > other.totalWeight;
    }

    public boolean lessThan(BFSNode other) {
        return totalWeight < other.totalWeight;
    }
}

// class BFSQueueElement {
//     boolean head;
//     boolean tail;
//     BFSQueueElement next;
//     BFSQueueElement previous;
//     BFSNode el;
//     int index;
// }


public strictfp class RobotPlayer {
    static int turnCount = 0;
    static final Random rng = new Random();
    static final Direction[] directions = Direction.allDirections();
    static final Direction[] cardinalDirections = Direction.cardinalDirections();
    static MapLocation globalTarget;
    static MapLocation localTarget;
    static final int SHARED_ARRAY_SOLDIER_CODE = 0;
    static final int SHARED_ARRAY_SAGE_CODE = 1;
    static final int SHARED_ARRAY_ALIVE_CODE = 1;
    static final int SHARED_ARRAY_DEAD_CODE = 0;
    static final int SHARED_ARRAY_ENEMY_START_INDEX = 2;
    static int mapWidth;
    static int mapHeight;
    static Team opponent;
    static boolean rotateLeft = rng.nextBoolean();
    static MapLocation backupTarget;
    static int actionRadiusSquared;

    static MapLocation findNearestEmptyTile(RobotController rc, MapLocation me) throws GameActionException {
        MapLocation[] nearbyTiles = rc.getAllLocationsWithinRadiusSquared(me, 100);
        int minDistance = 10000;
        MapLocation nearestFreeTile = null;

        for (MapLocation tile : nearbyTiles) {
            if (rc.canSenseLocation(tile) && !rc.canSenseRobotAtLocation(tile) && rc.senseLead(tile) == 0 && rc.senseGold(tile) == 0) {
                if (nearestFreeTile == null || me.distanceSquaredTo(tile) < minDistance) {
                    nearestFreeTile = tile;
                    minDistance = me.distanceSquaredTo(nearestFreeTile);
                }
            }
        }
        
        return nearestFreeTile;
    }

    static TripleTarget acquireLocalTargets(RobotController rc, MapLocation globalTarget, RobotInfo[] enemies,
            MapLocation me) throws GameActionException {
        int lowestHPDangerousEnemy = 10000;
        int lowestHPBenignEnemy = 10000;
        MapLocation primaryTarget = globalTarget;
        MapLocation secondaryTarget = globalTarget;
        MapLocation tertiaryTarget = globalTarget;

        for (RobotInfo enemy : enemies) {
            if (enemy.type.equals(RobotType.ARCHON)) {
                Comms.setOffensiveLocation(rc, enemy.location);
            }
            int distanceSquaredToEnemy = me.distanceSquaredTo(enemy.location);
            if (distanceSquaredToEnemy <= actionRadiusSquared && (enemy.type.equals(RobotType.SOLDIER)
                    || enemy.type.equals(RobotType.SAGE) || enemy.type.equals(RobotType.WATCHTOWER))) {
                if (enemy.health < lowestHPDangerousEnemy) {
                    primaryTarget = enemy.location;
                    lowestHPDangerousEnemy = enemy.health;
                }
            } else if (distanceSquaredToEnemy <= actionRadiusSquared) {
                if (enemy.health < lowestHPBenignEnemy) {
                    secondaryTarget = enemy.location;
                    lowestHPBenignEnemy = enemy.health;
                }
            } else {
                tertiaryTarget = enemy.location;
            }
        }

        return new TripleTarget(primaryTarget, secondaryTarget, tertiaryTarget);
    }

    /**
     * Find a potential target from the shared array.
     * 
     * `locateATarget` finds the closest enemy, defensive location, or offensive
     * location from the shared array. If none of these locations are available,
     * defaults to a given backup location `backupTarget`.
     * 
     * Additionally, it will see if each location is within view and if it is still
     * valid. If not, it will clear it from the shared array/reset the backup
     * location.
     * 
     * @param rc
     * @param backupTarget
     * @param me           - position of self. Is passing this cheaper than
     *                     rc.getLocation()?
     * @return
     * @throws GameActionException
     */
    static MapLocation locateCombatTarget(RobotController rc, MapLocation me, MapLocation backupLocation) throws GameActionException {

        boolean usingOffensiveTarget = false;
        MapLocation target = backupLocation;

        ArchonLocation[] archonLocations = Comms.getArchonLocations(rc);

        boolean shouldDefend = false;

        for (ArchonLocation archLoc : archonLocations) {
            if (archLoc.exists && archLoc.shouldDefend) {
                shouldDefend = true;
                if (target == null || me.distanceSquaredTo(archLoc.location) < me.distanceSquaredTo(target)) {
                    target = archLoc.location;
                }
            }
        }

        if (!shouldDefend) {
            EnemyLocation oTarget = Comms.getOffensiveLocation(rc);
            if (oTarget.exists) {
                target = oTarget.location;
                usingOffensiveTarget = true;
            }
        }

        if (usingOffensiveTarget && rc.canSenseLocation(target)) {
            if (rc.canSenseRobotAtLocation(target)) {
                if (rc.senseRobotAtLocation(target).type != RobotType.ARCHON) {
                    Comms.clearOffensiveLocation(rc);
                    target = backupTarget;
                }
            } else {
                Comms.clearOffensiveLocation(rc);
                target = backupTarget;
            }
        }

        if (RobotPlayer.targetDoesntExist(target)) {
            target = backupTarget;
            if (target != null && me.distanceSquaredTo(target) <= 4) {
                backupTarget = getRandomMapLocation();
                target = backupTarget;
            }
            usingOffensiveTarget = false;
        }

        // int index = -1;
        // for (int i = SHARED_ARRAY_ENEMY_START_INDEX; i < 64; i++) {
        //     int bitvector = rc.readSharedArray(i);
        //     if (bitvector == 0) {
        //         index = i;
        //         break;
        //     }
        //     MapLocation loc = decodeLocationFromBitvector(bitvector);
        //     if (me.distanceSquaredTo(loc) < me.distanceSquaredTo(target)) {
        //         target = loc;
        //     }
        // }

        EnemyLocation[] enemiesInSharedArray = Comms.getEnemyLocations(rc);
        for (EnemyLocation loc : enemiesInSharedArray) {
            if (me.distanceSquaredTo(loc.location) < me.distanceSquaredTo(target)) {
                target = loc.location;
            }
        }

        return target;
    }

    static void attackGlobalTargetIfAble(RobotController rc, MapLocation target, MapLocation me)
            throws GameActionException {
        if (me.distanceSquaredTo(target) <= actionRadiusSquared) {
            if (rc.canSenseLocation(target)) {
                if (rc.canSenseRobotAtLocation(target)) {
                    RobotInfo enemy = rc.senseRobotAtLocation(target);
                    if (enemy.type == RobotType.SOLDIER || enemy.type == RobotType.SAGE
                            || enemy.type == RobotType.WATCHTOWER) {
                        if (rc.canAttack(target)) {
                            rc.attack(target);
                        }
                    }
                }
            }
        }
    }

    static MapLocation getRandomMapLocation() {
        return new MapLocation(rng.nextInt(mapWidth - 1), rng.nextInt(mapHeight - 1));
    }

    static MapLocation getOffensiveTarget(RobotController rc) throws GameActionException {
        int bitvector = rc.readSharedArray(0);
        return decodeLocationFromBitvector(bitvector);
    }

    static boolean targetDoesntExist(MapLocation loc) {
        return loc.x == 0 && loc.y == 0;
    }

    static MapLocation getDefensiveTarget(RobotController rc) throws GameActionException {
        int bitvector = rc.readSharedArray(1);
        return decodeLocationFromBitvector(bitvector);
    }

    static MapLocation decodeLocationFromBitvector(int bitvector) {
        int x = (bitvector & (63 << 6)) >> 6;
        int y = (63 & bitvector);
        return new MapLocation(x, y);
    }

    static int encodeLocationToBitvector(MapLocation loc) {
        int x = loc.x;
        int y = loc.y;
        int bitvector = 0;
        bitvector += x << 6;
        bitvector += y;
        return bitvector;
    }

    // Bytecodes: 144
    public static void addLocationToSharedArray(RobotController rc, MapLocation coordinates, int unitCode, int idx)
            throws GameActionException {
        int uint16 = 0;
        int unitBit = unitCode;
        int xBit = coordinates.x;
        int yBit = coordinates.y;
        int aliveBit = 1;
        uint16 += yBit;
        uint16 += xBit << 6;
        uint16 += unitBit << 14;
        uint16 += aliveBit << 15;
        try {
            rc.writeSharedArray(idx, uint16);
        } catch (Exception e) {
            System.out.println(rc.getType() + " Exception");
            e.printStackTrace();
        }
    }

    // Bytecodes: 844
    // Returns full shared array.
    public static int[] getSharedArray(RobotController rc) throws GameActionException {
        int[] sharedArray = new int[64];
        for (int i = 0; i < 64; i++) {
            sharedArray[i] = rc.readSharedArray(i);
        }
        return sharedArray;
    }

    public static MapLocation[] getTargetList(RobotController rc) throws GameActionException {
        MapLocation[] sharedArray = new MapLocation[62];
        int start = 2;
        int end = 64;
        for (int i = start; i < end; i++) {
            int bitvector = rc.readSharedArray(i);
            if (bitvector == 0) {
                end = i;
                break;
            }
            MapLocation loc = decodeLocationFromBitvector(bitvector);
            sharedArray[i] = loc;
        }
        MapLocation[] slice = new MapLocation[end - start];
        for (int i = 0; i < slice.length; i++) {
            slice[i] = sharedArray[i];
        }
        return slice;
    }

    public static void move(RobotController rc, MapLocation target) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        Direction dir = myLoc.directionTo(target);
        Direction oldDir = dir;
        int numDirections = 8;
        for (int i = 0; i < numDirections; i++) {
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

                rc.move(dir);
                break;
            } else {
                if (rotateLeft) {
                    dir = dir.rotateLeft();
                } else {
                    dir = dir.rotateRight();
                }
                if (dir.equals(oldDir)) {
                    break;
                }
            }
        }
    }

    static MapLocation pathRecur(BFSNode currentNode, MapLocation start) {
        BFSNode parent = currentNode;
        while (!parent.parent.nodeLocation.equals(start)) {
            parent = parent.parent;
        }
        assert !parent.nodeLocation.equals(start);
        return parent.nodeLocation;
    }

    static void stepOffRubble(RobotController rc, MapLocation me) throws GameActionException {
        if (rc.isMovementReady() == false) {
            return;
        }

        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(me, 2);
        int baseRubble = rc.senseRubble(me);
        if (baseRubble == 0) {
            return;
        }
        Direction moveDirection = Direction.CENTER;        
        for (int i = 0; i < locations.length; i++) {
            if (rc.canSenseLocation(locations[i]) && !rc.canSenseRobotAtLocation(locations[i])) {
                int rubble = rc.senseRubble(locations[i]);
                if (rubble < baseRubble) {
                    moveDirection = me.directionTo(locations[i]);
                    baseRubble = rubble;
                }
            }
        }
        
        if (rc.canMove(moveDirection)) {
            rc.move(moveDirection);
        }
    }

    static void move2(RobotController rc, MapLocation target, int recursionLimit) throws GameActionException {
        Random rng = new Random();
        simpleShortestPath(rc, rc.getLocation(), target, 0, 0, recursionLimit, rng);

        // Direction dir = AdvancedMove(rc, target);
        // if (rc.canMove(dir)) {
        //     rc.move(dir);
        // }
    }

    static int simpleShortestPath(RobotController rc, MapLocation start, MapLocation target, int currentWeight, int recursionLevel, int recursionLimit, Random rng) throws GameActionException {
        if (recursionLevel == recursionLimit) {
            int finalWeight = currentWeight + rc.senseRubble(start) + (int)Math.sqrt(start.distanceSquaredTo(target));
            return finalWeight;
        }

        int rubbleAmount = rc.senseRubble(start);

        rubbleAmount /= 10;

        if (rubbleAmount > 50 && rng.nextInt(10) > 1) {
            rubbleAmount *= 10;
        }

        currentWeight += rubbleAmount;

        
        Direction initialDir = start.directionTo(target);
        Direction leftDir = initialDir.rotateLeft();
        Direction rightDir = initialDir.rotateRight();
        MapLocation leftTile = rc.adjacentLocation(leftDir);
        int leftWeight = leftTile.distanceSquaredTo(target);
        if (rc.canSenseLocation(leftTile) && !rc.canSenseRobotAtLocation(leftTile)) {
            leftWeight = simpleShortestPath(rc, leftTile, target, currentWeight, recursionLevel + 1, recursionLimit, rng);
        }
        
        MapLocation centerTile = rc.adjacentLocation(initialDir);
        int centerWeight = centerTile.distanceSquaredTo(target);
        if (rc.canSenseLocation(centerTile) && !rc.canSenseRobotAtLocation(centerTile)) {
            centerWeight = simpleShortestPath(rc, centerTile, target, currentWeight, recursionLevel + 1, recursionLimit, rng);
        }
        
        MapLocation rightTile = rc.adjacentLocation(rightDir);
        int rightWeight = centerTile.distanceSquaredTo(target);
        if (rc.canSenseLocation(rightTile) && !rc.canSenseRobotAtLocation(rightTile)) {
            rightWeight = simpleShortestPath(rc, rightTile, target, currentWeight, recursionLevel + 1, recursionLimit, rng);
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

    // static void simpleMove(RobotController rc, MapLocation start, MapLocation target) {
    //     Direction initialDir = start.directionTo(target);
    //     MapLocation left1 = rc.adjacentLocation(initialDir.rotateLeft());
    //     MapLocation center1 = rc.adjacentLocation(initialDir);
    //     MapLocation right1 = rc.adjacentLocation(initialDir.rotateRight());




    // }
    

    /** Best first search 
     * @throws GameActionException 
     *
     */
    static void bfsMove(RobotController rc, MapLocation start, MapLocation target) throws GameActionException {
        // We should set target to be a tile within vision distance in the direction toward target.
        MapLocation finalTarget = target;
        // MapLocation newTarget = start;
        // if (!rc.canSenseLocation(target)) {
        //     Direction dir = start.directionTo(target);
        //     target = start;
    
        //     int stop = (int)(Math.sqrt(rc.getType().visionRadiusSquared) - 1);
        //     for (int i = 0; i < stop; i++) {
        //         target = target.add(dir);
        //     }
        //     finalTarget = target;
        // } else {
        //     finalTarget = target;
        // }

        
        
        BFSNode startNode = new BFSNode(start, null, (int)Math.sqrt(start.distanceSquaredTo(finalTarget)) * 100, finalTarget);
        // System.out.println("Just before crash");
        NodeHeap queue = new NodeHeap(200, rc);
        // // queue.printSzie();
        queue.insert(startNode);
        // queue.insert(startNode);
        // queue.printSzie();
        // queue.print();
        Map visited = new Map();

        // MapLocation me = rc.getLocation();

        // MapLocation[] tiles = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 25);
        // int[] weights = new int[tiles.length];
        // int[] indices = new int[tiles.length];
        // int numTiles = 0;
        // for (int i = 0; i < weights.length; i++) {

        //     // Check to see if tile is closer to target than starting tile and is unoccupied.
        //     if (me.distanceSquaredTo(target) > tiles[i].distanceSquaredTo(target) && !rc.canSenseRobotAtLocation(tiles[i])) {

        //         if (me.distanceSquaredTo(tiles[i]) <= 2) {
        //             indices[numTiles] = i;
        //             numTiles++;
        //             weights[i] = rc.senseRubble(tiles[i]);
        //             continue;
        //         }

        //         for (int j = 0; j < numTiles; j++) {
        //             if (tiles[indices[j]].distanceSquaredTo(tiles[i]) <= 2) {

        //             }
        //         }
        //     }
        // }
        
        
        // 131 bytecode
        visited.add(startNode.nodeLocation, startNode);

        // BFSNode[] toVisit = new BFSNode[500];
        // toVisit[0] = startNode;
        
        
        
        int explorations = 0;
        // int currentIndex = 0;
        // int endIndex = 1;
        // while(currentIndex != endIndex) {

        // }
        
        while (queue.length() >= 1) {
            explorations += 1;
            int startTime = Clock.getBytecodeNum();
            BFSNode node = queue.remove(); // 234 bytecode;
            int end = Clock.getBytecodeNum();
            rc.setIndicatorString("" + (end - startTime));
            // queue.print();
            // queue.printSzie();

            
            if (explorations > 2) {
                MapLocation moveTarget = pathRecur(node, start);
                rc.setIndicatorLine(start, moveTarget, 0, 0, 100);
                if (rc.canMove(start.directionTo(moveTarget))) {
                    rc.move(start.directionTo(moveTarget));
                }
                return;
            }

            for (MapLocation neighborLoc : rc.getAllLocationsWithinRadiusSquared(node.nodeLocation, 2)) {
                if (!rc.canSenseLocation(neighborLoc) || rc.canSenseRobotAtLocation(neighborLoc)) {
                    continue;
                }
                BFSNode neighbor = new BFSNode(neighborLoc, node, (int)Math.sqrt(neighborLoc.distanceSquaredTo(finalTarget)) - (int)Math.sqrt(node.nodeLocation.distanceSquaredTo(finalTarget)) * 10 + (1 + rc.senseRubble(neighborLoc) / 10) * 10 + node.totalWeight, finalTarget);
                if (!visited.contains(neighborLoc)) {
                    if (neighbor.nodeLocation.equals(finalTarget)) {

                        MapLocation moveTarget = pathRecur(neighbor, start);
                        rc.setIndicatorLine(start, moveTarget, 0, 0, 100);
                        if (rc.canMove(start.directionTo(moveTarget))) {
                            rc.move(start.directionTo(moveTarget));
                        }

                        return;

                    } else {
                        visited.add(neighborLoc, neighbor);
                        queue.insert(neighbor);
                    }
                }
            }
        }
    }

    static boolean isLandSuitableForBuilding(RobotController rc, MapLocation loc) {
        MapLocation north = loc.add(directions[0]);
        MapLocation east = loc.add(directions[2]);
        MapLocation south = loc.add(directions[4]);
        MapLocation west = loc.add(directions[6]);

        boolean suitable;
        try {
            suitable = (!rc.canSenseRobotAtLocation(north)
                    || rc.senseRobotAtLocation(north).mode != RobotMode.TURRET
                            && rc.senseRobotAtLocation(north).mode != RobotMode.PROTOTYPE)
                    && (!rc.canSenseRobotAtLocation(east)
                            || rc.senseRobotAtLocation(east).mode != RobotMode.TURRET
                                    && rc.senseRobotAtLocation(east).mode != RobotMode.PROTOTYPE)
                    && (!rc.canSenseRobotAtLocation(south)
                            || rc.senseRobotAtLocation(south).mode != RobotMode.TURRET
                                    && rc.senseRobotAtLocation(south).mode != RobotMode.PROTOTYPE)
                    && (!rc.canSenseRobotAtLocation(west)
                            || rc.senseRobotAtLocation(west).mode != RobotMode.TURRET
                                    && rc.senseRobotAtLocation(west).mode != RobotMode.PROTOTYPE);
        } catch (GameActionException e) {
            suitable = false;
        }

        try {
            if (rc.canSenseLocation(loc) && rc.senseRubble(loc) > 10) {
                suitable = false;
            }
        } catch (GameActionException e) {
            e.printStackTrace();
        }

        return suitable;
    }


    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        mapWidth = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        opponent = rc.getTeam().opponent();
        actionRadiusSquared = rc.getType().actionRadiusSquared;
        backupTarget = getRandomMapLocation();


        while (true) {
            try {
                switch (rc.getType()) {
                    case ARCHON:
                        ArchonStrategy.run(rc);
                        break;
                    case MINER:
                        MinerStrategy.run(rc);
                        break;
                    case SOLDIER:
                        SoldierStrategy.run(rc);
                        break;
                    case LABORATORY:
                        LaboratoryStrategy.run(rc);
                        break;
                    case WATCHTOWER:
                        WatchtowerStrategy.run(rc);
                        break;
                    case BUILDER:
                        BuilderStrategy.run(rc);
                        break;
                    case SAGE:
                        SageStrategy.run(rc);
                        break;
                }
            } catch (GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (NullPointerException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
                rc.resign();
            } finally {
                Clock.yield();
            }
        }
    }
}
