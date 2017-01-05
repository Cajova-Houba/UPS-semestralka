package org.valesz.ups.main.game;

import org.valesz.ups.common.error.NotMyTurnException;

/**
 * Class implementing the game itself. It contains information about players and about the game state.
 *
 * @author Zdenek Vales
 */
public class Game {

    /**
     * Nickname of players which controls this client.
     * Used as temporal storage.
     */
    private String me;

    /**
     * Either 1 or 2, if the first or second player controls this client.
     */
    private int myPlayer;

    /**
     * The player which starts the game.
     */
    private Player firstPlayer;

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

    private static Game instance;

    public static Game getInstance() {
        if(instance == null) {
            instance = new Game();
        }

        return instance;
    }

    private Game() {
        winner = false;
        state = GameState.NOT_STARTED;
        myTurn = false;
    }

    public Player getFirstPlayer() {
        return firstPlayer;
    }

    public void setFirstPlayer(Player firstPlayer) {
        this.firstPlayer = firstPlayer;
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
     * Changes the state to WAITING_FOR_OPPONENT
     *
     * @param me Nick of the player which controls this client.
     *
     */
    public void waitingForOpponent(String me) {
        state = GameState.WAITING_FOR_OPPONENT;
        this.me = me;
    }

    /**
     * Starts the new game.
     * Sets the state to RUNNING.
     * Initializes both player objects.
     *
     *
     * @param firstPlayer The player who starts the game.
     * @param secondPlayer
     */
    public void startGame(String firstPlayer, String secondPlayer) {
        if (firstPlayer == me) {
            myPlayer = 1;
        } else {
            myPlayer = 2;
        }

        this.firstPlayer = new Player(firstPlayer,Player.FIRST_PLAYER_INIT_POS);
        this.secondPlayer = new Player(secondPlayer,Player.SECOND_PLAYER_INIT_POS);

        this.state = GameState.RUNNING;
    }

    /**
     * Ends my turn and updates the positions of firstPlayer.
     * If the myTurn is false, NotMyTurnException will be thrown.
     *
     * @param newPositions
     */
    public void endTurn(int[] newPositions) throws NotMyTurnException {
        if (!isMyTurn()) {
            throw new NotMyTurnException();
        }

        firstPlayer.setStones(newPositions);
        myTurn = false;
    }

    /**
     * Returns true if the state is RUNNING.
     * @return
     */
    public boolean isRunning() {
        return state == GameState.RUNNING;
    }
}
