package player36;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import java.util.Random;

public class Statics {
    public static boolean rotateLeft;
    public static Random rng;
    public static Team player;
    public static Team opponent;
    public static int mapHeight;
    public static int mapWidth;
    static final Direction[] directions = Direction.allDirections();

    public static void init(RobotController rc) {
        rng = new Random();
        rotateLeft = rng.nextBoolean();
        player = rc.getTeam();
        opponent = player.opponent();
        mapHeight = rc.getMapHeight();
        mapWidth = rc.getMapWidth();
    }

    public static MapLocation getRandomMapLocation() {
        return new MapLocation(rng.nextInt(mapWidth - 1), rng.nextInt(mapHeight - 1));
    }
}
