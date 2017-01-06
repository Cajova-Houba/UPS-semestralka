package org.valesz.ups.model.game;

/**
 * Possible states of the game.
 *
 * @author Zdenek Vales
 */
public enum GameState {

    /**
     * Game haven't started yet.
     */
    NOT_STARTED,

    /**
     * User is logged to server and is waiting for opponent.
     */
    WAITING_FOR_OPPONENT,

    /**
     * The game is running.
     */
    RUNNING,

    /**
     * The game has ended.
     */
    ENDED

}
