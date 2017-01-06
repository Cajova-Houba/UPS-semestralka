package org.valesz.ups.main.game;

import org.valesz.ups.common.error.NotMyTurnException;

import java.util.Random;

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
     * 1 or 2 - current turn.
     */
    private int turn;

    /**
     * Player's value. It's possible to throw only once per turn.
     * Default value is -1.
     *
     */
    private int thrownValue;

    private static Game instance;

    /**
     * True if the player already moved a stone in this turn.
     */
    private boolean alreadyMoved;



    public static Game getInstance() {
        if(instance == null) {
            instance = new Game();
        }

        return instance;
    }

    private Game() {
        winner = false;
        state = GameState.NOT_STARTED;
        turn = 1;
        thrownValue = -1;
        alreadyMoved = false;
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
        return turn == myPlayer;
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
        if (firstPlayer.equals(me)) {
            myPlayer = 1;
        } else {
            myPlayer = 2;
        }

        this.turn = 1;

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
        turn = turn == 1 ? 2 : 1;
        thrownValue = -1;
        alreadyMoved = false;
    }

    /**
     * Throws the sticks and returns thrown value. If the sticks had been already thrown,
     * actual value is returned.
     * @return
     */
    public int throwSticks() {
        if(thrownValue == -1) {
            Random random = new Random();
            thrownValue = random.nextInt(5) + 1;
        }

        return thrownValue;
    }

    /**
     * Moves a current player's turn from one field to another.
     * If the turn isn't possible, false is returned. Otherwise, true
     * is returned and alreadyMoved is set to true.
     *
     * @param fromField
     * @param toFiled
     * @return
     */
    public boolean moveStone(int fromField, int toFiled) {
        
    }




    /**
     * Returns true if the state is RUNNING.
     * @return
     */
    public boolean isRunning() {
        return state == GameState.RUNNING;
    }

    /**
     * Returns true if the player which controls this client begins the game.
     * @return
     */
    public boolean amIFirst() {
        return myPlayer == 1;
    }

    /**
     * Returns a nick of player who currently turns.
     * @return
     */
    public String getCurrentPlayer() {
        return turn == 1 ? firstPlayer.getNick() : secondPlayer.getNick();
    }

    /**
     * Returns the number (1 or 2) of the current player.
     * @return
     */
    public int getCurrentPlayerNum() {
        return turn;
    }

    public int getMyPlayer() {
        return myPlayer;
    }

    public int getThrownValue() {
        return thrownValue;
    }

    /**
     * Returns true if the sticks had been already thrown in this turn.
     * @return
     */
    public boolean alreadyThrown() {
        return thrownValue != -1;
    }

    /**
     * Returns true if a player already moved a stone in this turn.
     * @return
     */
    public boolean isAlreadyMoved() {
        return alreadyMoved;
    }



}
