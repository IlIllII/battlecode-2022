package player33;

import battlecode.common.*;


strictfp class SageStrategy {

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
    static MapLocation lastLastLastLocation = new MapLocation(0, 0);
    static MapLocation backupRetreatTarget;
    static boolean selfDestructing = false;
    static boolean retreating = false;
    static int selfDestructTimer = 0;
    static int lastTurnsHealth = 100;

    
    static void run(RobotController rc) throws GameActionException {

        
        // * Setting main variables
        boolean fallingBack = false;
        boolean enemySoldierClose = false;
        boolean attacked = false;
        int currentHealth = rc.getHealth();
        MapLocation me = rc.getLocation();
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, RobotPlayer.opponent);
        RobotInfo[] allies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam());
        MapLocation offensiveTarget = Targeting.getOffensiveTarget(rc);
        MapLocation defensiveTarget = Targeting.getDefensiveTarget(rc);
        MapLocation target = Targeting.getTargetFromGlobalAndLocalEnemyLocationsMAXHP(rc, enemies, backupLocation);

        // * These values vary based on current conditions
        aliveTime++;
        if (aliveTime == 1) {
            backupRetreatTarget = me;
        }
        if (!lastLocation.equals(me)) {
            lastLastLastLocation = lastLastLocation;
            lastLastLocation = lastLocation;
            lastLocation = me;
        }
        if (selfDestructing) {
            selfDestructTimer++;
        }
        if (enemies.length > 0 || lastTurnsHealth != currentHealth) {
            selfDestructing = false;
            selfDestructTimer = 0;
        }
        if (currentHealth <= 50) { // TODO Should this be higher even?
            retreating = true;
        }
        if (currentHealth > 98) { // leave wiggle room heal as we move away
            retreating = false;
            selfDestructing = false;
            selfDestructTimer = 0;
        }
        lastTurnsHealth = currentHealth;
        

        // * Are there dangerous soldiers nearby?
        // We care because we want to retreat after attacking them.
        // TODO: This can be more efficient - we iterate over enemies elsewhere.
        for (RobotInfo enemy : enemies) {
            if (enemy.type == RobotType.SOLDIER) {
                enemySoldierClose = true;
                break;
            }
        }
        
        
        // * Targeting cascade
        // Fall back to backup target as default. Might not be needed d/t new targeting scheme.
        if (target == null) {
            backupLocation = RobotPlayer.getRandomMapLocation();
            target = backupLocation;
        }
        // If offensive or defensive targets exist, we will prefer those.
        if (target.equals(backupLocation)) {
            if (offensiveTarget != null) {
                target = offensiveTarget;
            }
            if (defensiveTarget != null) {
                target = defensiveTarget;
            }
        }
        // We want to prefer defense when not engaged but still pursue enemies attacking our archon.
        if (defensiveTarget != null && me.distanceSquaredTo(target) > 49 && me.distanceSquaredTo(defensiveTarget) > 36) {
            target = defensiveTarget;
        }


        // This is an optional group up before rushing
        // if (rc.getRoundNum() < 60) {
        //     target = rc.adjacentLocation(RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)]);
        // }


        // Advance when surrounded by allies.
        // TODO: This is worth investigating - may not be the best strat.
        RobotInfo[] closeByTeammates = rc.senseNearbyRobots(2, rc.getTeam());
        if (closeByTeammates.length > 4 && !retreating) {
            RobotPlayer.move2(rc, target, 2);
        }


        // Attack if able before moving.
        if (rc.isActionReady()) {
            
            MapLocation moveTarget = me;
            int minDistance = 100000;
            int minRubble = 100000;

            for (MapLocation loc : rc.getAllLocationsWithinRadiusSquared(me, 16)) {
                if (rc.canSenseLocation(loc) && rc.senseRubble(loc) <= minRubble) {
                    if (rc.senseRubble(loc) < minRubble) {
                        minDistance = me.distanceSquaredTo(loc);
                        minRubble = rc.senseRubble(loc);
                        moveTarget = loc;
                    } else if (me.distanceSquaredTo(loc) < minDistance) {
                        moveTarget = loc;
                        minDistance = me.distanceSquaredTo(loc);
                        minRubble = rc.senseRubble(loc);
                    }
                }
            }

            if (!moveTarget.equals(me) && me.distanceSquaredTo(target) <= rc.getType().actionRadiusSquared) {
                target = moveTarget;
            } else if (rc.canAttack(target)) {
                rc.attack(target);
                attacked = true;
            }
        }

        // If we are engaged with enemy soldiers, we want to fall back
        if (!rc.isActionReady() && enemySoldierClose) {
            int x = (target.x - me.x);
            int y = (target.y - me.y);
            target = new MapLocation(me.x - x, me.y - y);
            fallingBack = true;
        }

        if (rc.getActionCooldownTurns() > 50) {
            retreating = true;
        }


        // * Movement dispatch.
        if (rc.isMovementReady()) {
            if (!retreating) { // We are advancing.
                if (!fallingBack) {
                    if (attacked || rc.getLocation().distanceSquaredTo(target) <= rc.getType().actionRadiusSquared) {
                        // We do not want to move onto rubble if we are in combat, but
                        // we also want to move if there aren't enemies in attack range.
                        Movement.moveButDontStepOnRubble(rc, target, 2, enemySoldierClose);
                    } else {
                        // If we haven't attacked yet it means we are advancing so we move like normal.
                        Movement.move(rc, target, lastLastLastLocation, 2, enemySoldierClose);
                        RobotPlayer.move(rc, target);
                    }
                } else {
                    // Special move where we do not fall back onto rubble.
                    Movement.fallingBackMove(rc, target);
                }
            } else { // Retreating
                if (!selfDestructing) {
                    MapLocation retreatTarget = Comms.getNearestArchonLocation(rc, me);

                    if (retreatTarget == null) {
                        retreatTarget = backupRetreatTarget;
                    }

                    if (retreatTarget != null) {
                        if (defensiveTarget != null && me.distanceSquaredTo(target) > 64) {
                            if (!fallingBack) {
                                Movement.move(rc, target, lastLastLastLocation, 2, enemySoldierClose);
                                RobotPlayer.move(rc, target);
                            } else {
                                Movement.fallingBackMove(rc, target);
                            }
                        } else {
                            Movement.move(rc, retreatTarget, lastLastLocation, 2, enemySoldierClose);                                
                            if (me.distanceSquaredTo(retreatTarget) <= 25 && enemies.length == 0) {
                                selfDestructing = true;
                            }
                        }

                    } else {
                        System.out.println("Couldnt find retreat target");
                    }
                } else {
                    MapLocation nearestFreeTile = RobotPlayer.findNearestEmptyTile(rc, me);
                    if (nearestFreeTile != null && !enemySoldierClose) {
                        RobotPlayer.move2(rc, nearestFreeTile, 2);
                        if (rc.getLocation().equals(nearestFreeTile)) {
                            if (selfDestructTimer > 2000) {
                                if (currentHealth < 20) {
                                    System.out.println("Disintegrating");
                                    // rc.disintegrate();
                                } else {
                                    selfDestructing = false;
                                    retreating = false;
                                }
                            }
                        }

                    } else {
                        RobotPlayer.move2(rc, target, 2);
                    }
                }
            }

        }

        // * Check for a cheeky attack on the tail end
        // if (rc.isActionReady()) {
        //     if (rc.canAttack(target)) {
        //         rc.attack(target);
        //     }
        // }

        rc.setIndicatorLine(rc.getLocation(), target, 0, 0, 0);
        rc.setIndicatorString("" + rc.getMovementCooldownTurns());

        
        
        // MapLocation me = rc.getLocation();

        // if (me.distanceSquaredTo(backupLocation) <= 4) {
        //     backupLocation = RobotPlayer.getRandomMapLocation();
        // }


        // // if (aliveTime == 0) {
        // //     bfs = new AdvancedMove(rc);
        // // }

        // aliveTime++;
        // if (aliveTime == 2) {
        //     repairLocation = me;
        // }
        

        // MapLocation target = RobotPlayer.locateCombatTarget(rc, me, backupLocation);
        // RobotInfo[] enemies = rc.senseNearbyRobots(-1, RobotPlayer.opponent);

        // RobotPlayer.attackGlobalTargetIfAble(rc, target, me);



        // TripleTarget localTargets = RobotPlayer.acquireLocalTargets(rc, target, enemies, me);

        // MapLocation primaryTarget = localTargets.primary;
        // MapLocation secondaryTarget = localTargets.secondary;
        // MapLocation tertiaryTarget = localTargets.tertiary;

        // // if (rc.getRoundNum() < 45) {
        // //     tertiaryTarget = rc.adjacentLocation(RobotPlayer.directions[RobotPlayer.rng.nextInt(RobotPlayer.directions.length)]);
        // // }


        // if (rc.senseNearbyRobots(2, rc.getTeam()).length > 4) {
        //     RobotPlayer.move2(rc, primaryTarget, 2);
        // }
        // if (rc.canAttack(primaryTarget)) {
        //     rc.attack(primaryTarget);
        //     Comms.setEnemyLocation(rc, primaryTarget);
        // }
        // if (rc.canAttack(secondaryTarget)) {
        //     rc.attack(secondaryTarget);
        // }
        // if (rc.canAttack(tertiaryTarget)) {
        //     rc.attack(tertiaryTarget);
        // }

        // // if (rc.senseNearbyRobots(-1, rc.getTeam()).length < 5 /* && rc.canSenseLocation(primaryTarget) && rc.canSenseRobotAtLocation(primaryTarget) && rc.senseRobotAtLocation(primaryTarget).type.equals(RobotType.SOLDIER) */ ) {
        // //     RobotPlayer.move2(rc, rc.adjacentLocation(me.directionTo(primaryTarget).opposite()).add(me.directionTo(primaryTarget)), 3);
        // // }

        // if (rc.isMovementReady()) {

        //     if (healing || rc.getHealth() < (rc.getType().health / 4) || !rc.isActionReady()) {

        //         ArchonLocation[] archLocs = Comms.getArchonLocations(rc);

        //         MapLocation repairLoc = null;

        //         boolean foundRepairSpot = false;

        //         while (!foundRepairSpot) {
        //             for (ArchonLocation archLoc : archLocs) {
        //                 // if (repairLoc == null || (archLoc.exists && me.distanceSquaredTo(repairLoc) > me.distanceSquaredTo(archLoc.location))) {
        //                 //     repairLoc = archLoc.location;
        //                 // }
        //                 if (archLoc.exists && RobotPlayer.rng.nextInt(4) == 0) {
        //                     repairLoc = archLoc.location;
        //                     foundRepairSpot = true;
        //                 }
        //             }
        //         }

        //         tertiaryTarget = repairLoc;
        //         healing = true;
        //     }

        //     if (rc.getHealth() > rc.getType().health && rc.getActionCooldownTurns() < 5) {
        //         healing = false;
        //     }

        //     // Experimental move.
        //     int startTime = Clock.getBytecodeNum();



            
        //     try {
        //         Direction dir = AdvancedMove.getBestDir(rc, tertiaryTarget);

        //         if (dir != null && !dir.equals(Direction.CENTER) && rc.canMove(dir)) {
        //             if (!rc.adjacentLocation(dir).equals(lastLocation)) {
        //                 rc.move(dir);
        //                 lastLocation = rc.getLocation();
        //             } else {
        //                 if (rc.canMove(rc.getLocation().directionTo(tertiaryTarget))) {
        //                     RobotPlayer.move(rc, tertiaryTarget);
        //                 }
        //             }
        //         }
        //     } catch (Exception e) {
        //         //TODO: handle exception
        //         System.out.println("Move returned null");;
        //     }
        //     // RobotPlayer.move2(rc, tertiaryTarget, recursionLimit);


        //     int end = Clock.getBytecodeNum();

        //     if ((end - startTime) > longestTime) {
        //         longestTime = (end - startTime);
        //     }
        //     rc.setIndicatorString("" + longestTime);
        //     rc.setIndicatorLine(me, tertiaryTarget, 1000, 0, 1000);;

        //     // Fall back to simple move incase other move doesn't work.
        //     RobotPlayer.move(rc, tertiaryTarget);


        //     // RobotPlayer.move(rc, tertiaryTarget);

        //     if (rc.canAttack(tertiaryTarget)) {
        //         rc.attack(tertiaryTarget);
        //     }
        // } /* else {
        //     RobotPlayer.stepOffRubble(rc, me);
        // } */
        // if (!rc.isActionReady() && rc.isMovementReady()) {
        //     MapLocation retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
        //     retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
        //     retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
        //     retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
        //     retreatMove = rc.adjacentLocation(me.directionTo(primaryTarget).opposite());
        //     RobotPlayer.move(rc, retreatMove);
        // }   
    }
}
