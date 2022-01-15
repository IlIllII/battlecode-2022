package player31;

import battlecode.common.*;

public abstract class BFS {

    final int BYTECODE_REMAINING = 1000;
    final int BYTECODE_REMAINING_NON_SLAND = 2500;
    //static final int BYTECODE_BFS = 5000;
    final int GREEDY_TURNS = 4;

    Pathfinding path;
    static RobotController rc;
    MapTracker mapTracker = new MapTracker();

    int turnsGreedy = 0;

    MapLocation currentTarget = null;




    BFS(RobotController rc){
        this.path = new Pathfinding(rc);
    }

    void reset(){
        turnsGreedy = 0;
        mapTracker.reset();
    }

    void update(MapLocation target){
        if (currentTarget == null || target.distanceSquaredTo(currentTarget) > 0){
            reset();
        } else --turnsGreedy;
        currentTarget = target;
        mapTracker.add(rc.getLocation());
    }

    void activateGreedy(){
        turnsGreedy = GREEDY_TURNS;
    }

    void initTurn(){
        path.initTurn();
    }

    void move(MapLocation target) throws GameActionException{
        move(target, false);
    }

    void move(MapLocation target, boolean greedy) throws GameActionException{
        if (target == null) return;
        if (!rc.isMovementReady()) return;
        if (rc.getLocation().distanceSquaredTo(target) == 0) return;

        update(target);

        if (!greedy && turnsGreedy <= 0){

            //System.err.println("Using bfs");
            Direction dir = getBestDir(target);
            if (dir != null && !mapTracker.check(rc.getLocation().add(dir))){
                rc.move(dir);
                return;
            } else activateGreedy();
        }

        if (rc.getType() == RobotType.SOLDIER) {
            if (Clock.getBytecodesLeft() >= BYTECODE_REMAINING) {
                //System.err.println("Using greedy");
                //System.out.println("Before pathfinding " + Clock.getBytecodeNum());
                path.move(target);
                //System.out.println("After pathfinding " + Clock.getBytecodeNum());
                --turnsGreedy;
            }
        } else{
            if (Clock.getBytecodesLeft() >= BYTECODE_REMAINING_NON_SLAND) {
                //System.err.println("Using greedy");
                //System.out.println("Before pathfinding " + Clock.getBytecodeNum());
                path.move(target);
                //System.out.println("After pathfinding " + Clock.getBytecodeNum());
                --turnsGreedy;
            }
        }
    }

    abstract Direction getBestDir(MapLocation target);


}