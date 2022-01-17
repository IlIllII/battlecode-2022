package player33;


import battlecode.common.*;


strictfp class MinerStrategyBigMap {
    static MapLocation globalTarget;

    static MapLocation earlyGameTarget(RobotController rc, int mapWidth, int mapHeight) {
        System.out.println("Width: " + mapWidth + ", Height:" + mapHeight);


        MapLocation randLoc = RobotPlayer.getRandomMapLocation();
        return randLoc; // ! We can change this after the tourney.

        // int n = RobotPlayer.rng.nextInt(100);
        // MapLocation me = rc.getLocation();

        // // rivers
        // if (mapWidth == 55 && mapHeight == 45) {
        //     MapLocation loc1 = new MapLocation(8, 41);
        //     MapLocation loc2 = new MapLocation(45, 3);

        //     if (n < 100) {
        //         return me.distanceSquaredTo(loc1) >= me.distanceSquaredTo(loc2) ? loc2 : loc1;
        //     } else {
        //         return randLoc;
        //     }
        // }

        // if (mapWidth == 60 && mapHeight == 60) {
        //     // eckleberg
        //     if (me.x - mapWidth < -50 || me.x - mapWidth > -8 || me.y - mapHeight < -50 || me.y - mapHeight > -8) {
        //         return randLoc;
        //     } else { // Colosseum
        //         MapLocation loc1 = new MapLocation(0, 59);
        //         MapLocation loc2 = new MapLocation(0, 0);
        //         MapLocation loc3 = new MapLocation(59, 59);
        //         MapLocation loc4 = new MapLocation(59, 0);
        //         MapLocation loc5 = new MapLocation(28, 0);
        //         MapLocation loc6 = new MapLocation(28, 59);
        //         MapLocation loc7 = new MapLocation(52, 29);
        //         MapLocation minLoc = new MapLocation(1000, 1000);
    
        //         MapLocation[] locs = {loc1, loc2, loc3, loc4, loc5, loc6};
        //         for (MapLocation loc : locs) {
        //             if (me.distanceSquaredTo(loc) < me.distanceSquaredTo(minLoc)) {
        //                 minLoc = loc;
        //             }
        //         }
        //         if (minLoc.equals(loc5) || minLoc.equals(loc6)) {
        //             if (n < 75) {
        //                 minLoc = loc7;
        //             }
        //         }
        //         return minLoc;
        //     }
        // }

        // // intersection
        // if (mapWidth == 49 && mapHeight == 25) {
        //     MapLocation loc1 = new MapLocation(4, 12);
        //     MapLocation loc2 = new MapLocation(10, 12);
        //     MapLocation loc3 = new MapLocation(14, 12);
        //     MapLocation loc4 = new MapLocation(22, 12);
        //     MapLocation loc5 = new MapLocation(28, 12);
        //     MapLocation loc6 = new MapLocation(32, 12);
        //     MapLocation loc7 = new MapLocation(40, 12);
        //     MapLocation loc8 = new MapLocation(46, 12);
        //     MapLocation[] locs = {loc1, loc2, loc3, loc4, loc5, loc6, loc7, loc8};
        //     return locs[RobotPlayer.rng.nextInt(8)];
        // }

        // // fortress
        // if (mapWidth == 60 && mapHeight == 30) {
        //     return new MapLocation(30, 16);
        // }

        // // squer
        // if (mapWidth == 25 && mapHeight == 25) {
        //     return new MapLocation(12, 12);
        // }

        // // nottestsmall
        // if (mapWidth == 20 && mapHeight == 20) {
        //     if (n > 50) {
        //         return new MapLocation(2, 30);
        //     } else {
        //         return new MapLocation(30, 2);
        //     }
        // }



        // return RobotPlayer.getRandomMapLocation();
    }
    
    static void mine(RobotController rc, MapLocation loc) throws GameActionException {
        for (Direction dir : RobotPlayer.directions) {
            if (!rc.isActionReady()) {
                break;
            }
            MapLocation newLoc = rc.adjacentLocation(dir);
            if (rc.canSenseLocation(newLoc)) {
                while (rc.senseGold(newLoc) > 0 && rc.canMineGold(newLoc)) {
                    rc.mineGold(newLoc);
                }
                while (rc.senseLead(newLoc) > 1 && rc.canMineLead(newLoc)) {
                    rc.mineLead(newLoc);
                }
            }
        }
    }


    static MapLocation findNearbyMetals(RobotController rc, MapLocation me, MapLocation target, boolean fleeing, boolean dontSteal, RobotInfo[] allies, MapLocation backupRetreatLocation) throws GameActionException {
        
        int distanceToTarget = me.distanceSquaredTo(target);
        int leadCount = 0;

        int miningCutoff = fleeing ? 0 : 1;


        int minDistance = 10000;
        for (ArchonLocation archLoc : Comms.getArchonLocations(rc)) {
            if (me.distanceSquaredTo(archLoc.location) < minDistance) {
                minDistance = me.distanceSquaredTo(archLoc.location);
            }
        }

        if (me.distanceSquaredTo(backupRetreatLocation) < minDistance) {
            minDistance = me.distanceSquaredTo(backupRetreatLocation);
        }

        if (minDistance != 10000 && minDistance > 144) {
            dontSteal = false;
        } else if (minDistance < 144) {
            dontSteal = true;
        }

        rc.setIndicatorString("" + minDistance);

        // for (RobotInfo ally: allies) {
        //     if (ally.type.equals(RobotType.ARCHON)) {
        //         dontSteal = true;
        //     }
        // }
        

        int leadCutoff = 10;
        
        if (dontSteal) {
            miningCutoff = 1;
        } else {
            miningCutoff = 0;
            leadCutoff = 0;
        }

        for (MapLocation loc : rc.senseNearbyLocationsWithLead(rc.getType().visionRadiusSquared)) {
            leadCount = rc.senseLead(loc);
            if (leadCount > leadCutoff) {
                int distanceToLoc = me.distanceSquaredTo(loc);
                if (distanceToLoc < distanceToTarget) {
                    target = loc;
                    distanceToTarget  = distanceToLoc;
                }
            }
            if (leadCount > miningCutoff && me.distanceSquaredTo(loc) <= 2) {
                while (rc.isActionReady() && rc.senseLead(loc) > miningCutoff) {
                    rc.mineLead(loc);
                }
            }
        }

        for (MapLocation loc : rc.senseNearbyLocationsWithGold(rc.getType().visionRadiusSquared)) {
            target = loc;
            if (me.distanceSquaredTo(loc) <= 2) {
                while (rc.senseGold(loc) > 0 && rc.isActionReady()) {
                    rc.mineGold(loc);
                }
            }
            break;
        }

        return target;
    }


    static MapLocation backupRetreatTarget;
    static int age = 0;
    static boolean healing = false;
    static boolean retreating = false;
    static boolean selfDestructing = false;
    static MapLocation lastLocation = new MapLocation(0, 0);
    static MapLocation lastLastLocation = new MapLocation(0, 0);
    static MapLocation lastLastLastLocation = new MapLocation(0, 0);

    static void run(RobotController rc) throws GameActionException {
        MapLocation me = rc.getLocation();
        if (!lastLocation.equals(me)) {
            lastLastLastLocation = lastLastLocation;
            lastLastLocation = lastLocation;
            lastLocation = me;
        }
        
        // * Setting targets - globalTarget (for exploring) -> localTarget (has metal)
        age++;
        if (age == 1) {
            backupRetreatTarget = me;
        }
        if (globalTarget == null) {
            if (rc.getRoundNum() < 20) {
                globalTarget = earlyGameTarget(rc, RobotPlayer.mapWidth, RobotPlayer.mapHeight);
            } else {
                globalTarget = RobotPlayer.getRandomMapLocation();
            }
        }
        if (me.distanceSquaredTo(globalTarget) <= 9) {
            globalTarget = RobotPlayer.getRandomMapLocation();
        }
        MapLocation target = globalTarget;
        MapLocation fleeTarget = null;

        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        boolean dontSteal = true;

        // Only steal when in enemy territory
        if (allies.length > 0) {
            dontSteal = true;
        }


        


        // * Should we flee/retreat?
        boolean fleeing = false;
        if (enemies.length > 0) {
            for (RobotInfo enemy : enemies) {
                Comms.setEnemyLocation(rc, enemy.location, Comms.getEnemyLocations(rc));
                if (enemy.type == RobotType.SOLDIER) {
                    if (rc.getHealth() < rc.getType().health / 3) {
                        retreating = true;
                        break;
                    } else {
                        fleeing = true;
                        fleeTarget = Targeting.getFallbackTarget(me, enemy.location);
                        if (fleeTarget.x >= 0 && fleeTarget.y >= 0 && fleeTarget.x <= RobotPlayer.mapWidth && fleeTarget.y <= RobotPlayer.mapHeight) {
                            globalTarget = fleeTarget;
                        }
                    }
                    break;
                }
            }
        }

        
        target = findNearbyMetals(rc, me, target, fleeing, dontSteal, allies, backupRetreatTarget);
        

        if (allies.length > 0 && rc.senseLead(me) <= 1) {
            for (RobotInfo ally : allies) {
                if (ally.type == RobotType.MINER) {
                    if (target.distanceSquaredTo(ally.location) <= 4) {
                        target = globalTarget;
                        break;
                    }
                }
            }
        }


        if (rc.getHealth() < rc.getType().health / 3) {
            retreating = true;
            target = backupRetreatTarget;
        }

        if (rc.getHealth() == rc.getType().health) {
            retreating = false;
        }

        // Mine
        // mine(rc, rc.getLocation());

        if (retreating && !selfDestructing) {
            MapLocation retreatTarget = Comms.getNearestArchonLocation(rc, me);

            if (retreatTarget == null) {
                retreatTarget = backupRetreatTarget;
            }

            if (retreatTarget != null) {
                Movement.move(rc, retreatTarget, lastLastLocation, 2, false);
                if (me.distanceSquaredTo(retreatTarget) <= 9) {
                    selfDestructing = true;
                }

            } else {
                System.out.println("Couldnt find retreat target");
            }
        } else if (selfDestructing) {
            MapLocation nearestFreeTile = RobotPlayer.findNearestEmptyTile(rc, me);
            if (nearestFreeTile != null) {
                RobotPlayer.move2(rc, nearestFreeTile, 2);
                if (rc.getLocation().equals(nearestFreeTile)) {
                    System.out.println("Disintegrating");
                    rc.disintegrate();
                }

            } else {
                RobotPlayer.move2(rc, target, 2);
            }
        }
        
        if (!retreating) {
            if (fleeing && fleeTarget != null) {
                Movement.move(rc, fleeTarget, lastLastLocation, 1, false);
            } else if (!me.equals(target)) {
                Movement.move(rc, target, lastLastLastLocation, 1, false);
                RobotPlayer.move(rc, target);
            } else {
                RobotPlayer.stepOffRubble(rc, me);
            }
        }

        // if (rc.isMovementReady()) {
        // }

        // int start = Clock.getBytecodeNum();
        // int end = Clock.getBytecodeNum();
        // int leftB = Clock.getBytecodesLeft();
        // rc.setIndicatorString(target.toString());
        rc.setIndicatorDot(target, 1000, 0, 0);
        
    }
}
