package myplayer;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public class RobotPlayer {
    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;
    static int dontTurnTimer = 0;
    static int dontLookForCheeseTimer = 0;
    static int stuckTimer = 0;
    static MapLocation forwards;
    static MapLocation ratSpawn;
    static Direction targetDirection;
    static int focusID;
    static ArrayList<Integer> foundIDs = new ArrayList<>();
    enum MODE {
        HUNT,
        FLEE,
        HARVEST,
        STARVING,
        SEARCH,
        GUARD,
    }

    static MapLocation ZeroZero;
    static MapLocation KingLocation;
    static MODE behaviour = MODE.HARVEST;
    static int idleDelay = 0;
    static MapLocation focusLastKnownLocation;
    static final int idleDelayTime = 10;
    static RobotInfo[] robots;
    static boolean isAGuard = false;
    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
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

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
       //if (rc.getID() % 7 == 0) {// scrapped
       //   behaviour = MODE.GUARD;
       //    isAGuard = true;
       //}
        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("It is working!");
        ZeroZero = rc.getLocation().translate(-rc.getLocation().x,-rc.getLocation().y); // (0,0)
        focusLastKnownLocation = ZeroZero;
        // You can also use indicators to save debug notes in replays.
        while (true) {
            if (rc.getType() == UnitType.RAT_KING && rc.getHealth() >= 20) {
                rc.writeSharedArray(5,0);
            } else if (rc.getType() == UnitType.RAT_KING){
                rc.writeSharedArray(5,1);
            }
            if (rc.canBecomeRatKing() && rc.readSharedArray(5) == 1) {
                rc.becomeRatKing();
            }
            KingLocation = ZeroZero.translate(rc.readSharedArray(2), rc.readSharedArray(3));
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.
            turnCount += 1;  // We have now been alive for one more turn!
            forwards = rc.adjacentLocation(rc.getDirection());
            ratSpawn = rc.adjacentLocation(rc.getDirection().opposite()).add(rc.getDirection().opposite());
            if (dontLookForCheeseTimer > 0) {
                dontLookForCheeseTimer -= 1;
            }
            if (dontTurnTimer > 0) {
                dontTurnTimer -= 1;
            }
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                if (isAGuard) {behaviour = MODE.GUARD;}
                if (rc.getType() == UnitType.RAT_KING && rc.canBuildRat(ratSpawn) && (turnCount % 10 == 0) && (rc.getGlobalCheese() - rc.getCurrentRatCost() >= 50)) {
                    rc.buildRat(ratSpawn);
                    System.out.println("I built a rat!");
                }
                if (turnCount % 100 == 0) {
                    System.out.println("Turn " + turnCount + ": I am a " + rc.getType().toString());
                }
                if (behaviour == MODE.GUARD) {
                    rc.setIndicatorString("GUARD");
                    if (rng.nextInt(2) == 1) {
                        switch (rc.readSharedArray(4)) {
                            case 1 -> targetDirection = Direction.NORTH;
                            case 2 -> targetDirection = Direction.NORTHEAST;
                            case 3 -> targetDirection = Direction.EAST;
                            case 4 -> targetDirection = Direction.SOUTHEAST;
                            case 5 -> targetDirection = Direction.SOUTH;
                            case 6 -> targetDirection = Direction.SOUTHWEST;
                            case 7 -> targetDirection = Direction.WEST;
                            case 8 -> targetDirection = Direction.NORTHWEST;
                        }
                    } else if (rc.getLocation().distanceSquaredTo(KingLocation) >= 10 ) {
                        targetDirection = rc.getLocation().directionTo(KingLocation);
                    }
                    if (rc.canMove(targetDirection)) {
                        rc.turn(targetDirection);
                    }
                } else if (behaviour == MODE.HARVEST || behaviour == MODE.STARVING) {
                    if (rc.getType() == UnitType.BABY_RAT) {
                        if (behaviour == MODE.STARVING) {
                            rc.setIndicatorString("STARVING! " + rc.getRawCheese());
                        } else {
                            rc.setIndicatorString("HARVEST " + rc.getRawCheese());
                        }
                    } else {
                        if (behaviour == MODE.STARVING) {
                            rc.setIndicatorString("STARVING! " + rc.getGlobalCheese());
                        } else {
                            rc.setIndicatorString("HARVEST " + rc.getGlobalCheese());
                        }
                    }
                    if (rc.canRemoveCatTrap(forwards)&& rng.nextInt(3) == 1 ) {
                        rc.removeCatTrap(forwards);
                    }
                    if (rc.getType() == UnitType.BABY_RAT && rc.getRawCheese() >= 100 ){
                        Direction targetDirection = rc.getLocation().directionTo(KingLocation);
                        if (rc.canMove(targetDirection) && targetDirection != Direction.CENTER) {
                            rc.turn(targetDirection);
                        } // assume the later turning code will handle the rest
                        if (rc.canTransferCheese(KingLocation, 100)){
                            rc.transferCheese(KingLocation,100);
                        }
                    } else {
                        if (rc.senseMapInfo(rc.getLocation()).getCheeseAmount() != 0) {
                            rc.pickUpCheese(rc.getLocation());
                        } else {
                            System.out.println("No cheese on me.");
                            if (dontLookForCheeseTimer == 0) {
                                MapInfo[] data = rc.senseNearbyMapInfos();
                                for (MapInfo datum : data) {
                                    targetDirection = rc.getLocation().directionTo(datum.getMapLocation());
                                    if (datum.getCheeseAmount() > 0 && (rc.canMove(targetDirection) || rc.getType() == UnitType.BABY_RAT) && (targetDirection != rc.getDirection().opposite())) {
                                        rc.turn(targetDirection);
                                        dontTurnTimer = 10;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else if (behaviour == MODE.FLEE) {
                    rc.setIndicatorString("FLEE: " + idleDelay + " from " + focusID);
                    if (rc.canSenseRobot(focusID)){
                        idleDelay = idleDelayTime;
                        focusLastKnownLocation = rc.senseRobot(focusID).getLocation();
                        Direction targetDirection = rc.getLocation().directionTo(focusLastKnownLocation).opposite();
                        if (rc.canMove(targetDirection)) {
                            rc.turn(targetDirection);
                        } // assume the later turning code will handle the rest
                        rc.setIndicatorDot(focusLastKnownLocation,255,0,0);
                    } else {
                        idleDelay -= 1;
                        Direction targetDirection = rc.getLocation().directionTo(focusLastKnownLocation).opposite();
                        if (rc.canMove(targetDirection)) {
                            rc.turn(targetDirection);
                        } // assume the later turning code will handle the
                        rc.setIndicatorDot(focusLastKnownLocation,255,0,0);
                        if (idleDelay <= 0) {
                            behaviour = MODE.HARVEST;
                            focusID = 0;
                            focusLastKnownLocation = ZeroZero;
                        }
                    }
                } else if (behaviour == MODE.HUNT) {
                    rc.setIndicatorString("HUNT: " + idleDelay);
                    if (rc.canSenseRobot(focusID)){
                        idleDelay = idleDelayTime;
                        focusLastKnownLocation = rc.senseRobot(focusID).getLocation();
                        Direction targetDirection = rc.getLocation().directionTo(focusLastKnownLocation);
                        if (rc.canMove(targetDirection)) {
                            rc.turn(targetDirection);
                        } // assume the later turning code will handle the rest
                        rc.setIndicatorDot(focusLastKnownLocation,0,255,0);
                    } else {
                        idleDelay -= 1;
                        Direction targetDirection = rc.getLocation().directionTo(focusLastKnownLocation);
                        if (targetDirection == Direction.CENTER) {
                            behaviour = MODE.HARVEST;
                            focusID = 0;
                            focusLastKnownLocation = ZeroZero;
                        } else {
                            if (rc.canMove(targetDirection)) {
                                rc.turn(targetDirection);
                            } // assume the later turning code will handle the rest
                        }
                        rc.setIndicatorDot(focusLastKnownLocation,0,255,0);
                        if (idleDelay <= 0) {
                            behaviour = MODE.HARVEST;
                            focusID = 0;
                            focusLastKnownLocation = ZeroZero;
                        }
                    }
                } else if (behaviour == MODE.SEARCH) { // the SEARCH mode
                    rc.setIndicatorString("SEARCH: " + idleDelay);
                    if (rc.canSenseRobot(focusID)) {
                        idleDelay = idleDelayTime;
                        focusLastKnownLocation = rc.senseRobot(focusID).getLocation();
                        Direction targetDirection = rc.getLocation().directionTo(focusLastKnownLocation);
                        if (rc.canMove(targetDirection)) {
                            rc.turn(targetDirection);
                        }
                        rc.setIndicatorDot(focusLastKnownLocation, 0, 255, 0);
                    } else if (rc.readSharedArray(0) != 0 || rc.readSharedArray(1) != 0) {
                        idleDelay = idleDelayTime;
                        focusLastKnownLocation = ZeroZero.translate(rc.readSharedArray(0),rc.readSharedArray(1));
                        Direction targetDirection = rc.getLocation().directionTo(focusLastKnownLocation);
                        if (targetDirection == Direction.CENTER) {
                            behaviour = MODE.HARVEST;
                            focusID = 0;
                            focusLastKnownLocation = ZeroZero;
                        } else {
                            if (rc.canMove(targetDirection)) {
                                rc.turn(targetDirection);
                            } // assume the later turning code will handle the rest
                        }
                        rc.setIndicatorDot(focusLastKnownLocation, 0, 255, 0);
                    } else {
                        idleDelay -= 1;
                        focusLastKnownLocation = ZeroZero.translate(rc.readSharedArray(0),rc.readSharedArray(1));
                        Direction targetDirection = rc.getLocation().directionTo(focusLastKnownLocation);
                        if (targetDirection == Direction.CENTER) {
                            behaviour = MODE.HARVEST;
                            focusID = 0;
                            focusLastKnownLocation = ZeroZero;
                        } else {
                            if (rc.canMove(targetDirection)) {
                                rc.turn(targetDirection);
                            } // assume the later turning code will handle the rest
                        }
                        rc.setIndicatorDot(focusLastKnownLocation,0,255,0);
                        if (idleDelay <= 0) {
                            behaviour = MODE.HARVEST;
                            focusLastKnownLocation = ZeroZero;
                        }
                    }
                }
                if (behaviour == MODE.GUARD) {
                    robots = rc.senseNearbyRobots(5);
                } else {
                    robots = rc.senseNearbyRobots();
                }
                if (rc.getGlobalCheese() <= 500 && behaviour != MODE.GUARD) {
                    behaviour = MODE.STARVING;
                }
                if (behaviour != MODE.STARVING) {
                    if (idleDelay <= 0 && behaviour != MODE.GUARD){ // just in case
                        behaviour = MODE.HARVEST;
                    }
                    if (behaviour != MODE.GUARD){
                        for (RobotInfo robot : robots) {
                            if (robot.getType() == UnitType.CAT) {
                                if (focusID == 0 || (focusLastKnownLocation.distanceSquaredTo(rc.getLocation())) < robot.getLocation().distanceSquaredTo(rc.getLocation())) {
                                    focusID = robot.getID();
                                    if (rc.getType() == UnitType.RAT_KING) {
                                        behaviour = MODE.FLEE;
                                    } else if (rc.getID() % 2 == 0 || rc.getGlobalCheese() >= 2000 || behaviour == MODE.SEARCH) { //only some hunt, all hunt if theres enought cheese
                                        behaviour = MODE.HUNT;
                                    }
                                }
                            }
                        }
                    } else {
                        for (RobotInfo robot : robots) {
                            break;
                        }
                    }
                } else {
                    if (rc.getGlobalCheese() >= 1000 && behaviour != MODE.GUARD) {
                        behaviour = MODE.HARVEST;
                        }
                }
                if (rc.canMoveForward() && (rc.getType()!= UnitType.RAT_KING || rng.nextInt(5) == 1 || behaviour != MODE.HARVEST)) {
                    System.out.println("Turn " + turnCount + ": Trying to move " + rc.getDirection());
                    rc.moveForward();
                    stuckTimer = 0;
                } else {
                    if (rc.canRemoveDirt(forwards)) {
                        System.out.println("Can't move. Digging...");
                        rc.removeDirt(forwards);
                    } else {
                        System.out.println("Can't move. Turning...");
                        stuckTimer += 1;
                        if (dontLookForCheeseTimer == 0 && stuckTimer == 5) {
                            dontLookForCheeseTimer = 30;
                            stuckTimer = 0;
                        }
                        if (rc.canTurn() && rc.isMovementReady() && (dontTurnTimer == 0)) {
                            if (rng.nextBoolean()) {
                                if (rc.canMove(rc.getDirection().rotateRight())) {
                                    rc.turn(rc.getDirection().rotateRight());
                                } else {
                                    rc.turn(rc.getDirection().rotateLeft());
                                }
                            } else {
                                if (rc.canMove(rc.getDirection().rotateLeft())) {
                                    rc.turn(rc.getDirection().rotateLeft());
                                } else {
                                    rc.turn(rc.getDirection().rotateRight());
                                }
                            }
                        }
                    }
                }
                if (rc.getType() == UnitType.RAT_KING) {
                    rc.writeSharedArray(0,focusLastKnownLocation.x);
                    rc.writeSharedArray(1,focusLastKnownLocation.y);
                    rc.writeSharedArray(2,rc.getLocation().x);
                    rc.writeSharedArray(3,rc.getLocation().y);
                    // which way am i going??
                    switch (rc.getDirection()) {
                        case NORTH -> rc.writeSharedArray(4,1);
                        case NORTHEAST ->  rc.writeSharedArray(4,2);
                        case EAST -> rc.writeSharedArray(4,3);
                        case SOUTHEAST -> rc.writeSharedArray(4,4);
                        case SOUTH ->   rc.writeSharedArray(4,5);
                        case SOUTHWEST -> rc.writeSharedArray(4,6);
                        case WEST -> rc.writeSharedArray(4,7);
                        case NORTHWEST -> rc.writeSharedArray(4,8);
                    }
                }
                if (rc.getType() == UnitType.BABY_RAT && (rc.getID() % 2 == 0 || rc.getGlobalCheese() >= 2000) && (behaviour != MODE.HUNT && behaviour != MODE.GUARD) && (rc.readSharedArray(0) != 0 || rc.readSharedArray(1) != 0)){
                    behaviour = MODE.SEARCH;
                }
                if (rc.getType() == UnitType.BABY_RAT && behaviour == MODE.HUNT ) {
                    if (rc.canPlaceCatTrap(forwards) && rc.getBackstabbingTeam() == null) {
                        rc.placeCatTrap(forwards);
                    } else if (rc.canPlaceRatTrap(forwards) && rc.getBackstabbingTeam() != null){
                        rc.placeRatTrap(forwards);
                    }
                }
                if (rc.getType() == UnitType.BABY_RAT && rc.canAttack(forwards) && (rc.getBackstabbingTeam() != null || rc.senseRobotAtLocation(forwards).getType() == UnitType.CAT || behaviour == MODE.GUARD)) {
                    rc.attack(forwards);
                }
            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                System.out.println("End of turn");
                // Signify we've done everything we want to do, thereby endxing our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }
}
