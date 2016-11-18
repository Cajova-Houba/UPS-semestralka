package org.valesz.ups.main.game;

import org.valesz.ups.common.error.NotMyTurnException;

/**
 * Class implementing the game itself. It contains information about players and about the game state.
 *
 * @author Zdenek Vales
 */
public class Game {

    /**
     * The player which controls this client.
     */
    private Player mainPlayer;

    /**
     * The second player.
     */
    private Player secondPlayer;

    /**
     * True if the winner is me, false if the opponent has won.
     */
    private boolean winner;

    /**
     * Current state of the game.
     */
    private GameState state;

    /**
     * True if it's my turn now.
     */
    private boolean myTurn;

    public Game() {
        winner = false;
        state = GameState.NOT_STARTED;
        myTurn = false;
    }

    public Player getMainPlayer() {
        return mainPlayer;
    }

    public void setMainPlayer(Player mainPlayer) {
        this.mainPlayer = mainPlayer;
    }

    public Player getSecondPlayer() {
        return secondPlayer;
    }

    public void setSecondPlayer(Player secondPlayer) {
        this.secondPlayer = secondPlayer;
    }

    public boolean isWinner() {
        return winner;
    }

    public GameState getState() {
        return state;
    }

    public boolean isMyTurn() {
        return myTurn;
    }

    /**
     * Ends my turn and updates the positions of mainPlayer.
     * If the myTurn is false, NotMyTurnException will be thrown.
     *
     * @param newPositions
     */
    public void endTurn(int[] newPositions) throws NotMyTurnException {
        if (!isMyTurn()) {
            throw new NotMyTurnException();
        }

        mainPlayer.setStones(newPositions);
        myTurn = false;
    }
}
