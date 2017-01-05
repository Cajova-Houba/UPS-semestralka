package org.valesz.ups.main.game;

import org.valesz.ups.common.Constraits;

/**
 * Class containing informations about a player.
 *
 * @author Zdenek Vales
 */
public class Player {

    public static final int FIRST_PLAYER_INIT_POS = 1;
    public static final int SECOND_PLAYER_INIT_POS = 2;

    private String nick;

    /**
     * Player's stones.
     */
    private int[] stones;

    public Player() {
        stones = new int[Constraits.MAX_NUMBER_OF_STONES];
    }

    /**
     * Creates a new player object and generates a new set of stones.
     * @param nick
     * @param initPosition
     */
    public Player(String nick, int initPosition) {
        this(nick);
        generateNewStones(initPosition);
    }

    public Player(String nick) {
        this(nick, new int[Constraits.MAX_NUMBER_OF_STONES]);
    }

    public Player(String nick, int[] stones) {
        this.nick = nick;
        this.stones = stones;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public int[] getStones() {
        return stones;
    }

    public void setStones(int[] stones) {
        this.stones = stones;
    }

    /**
     * Returns the state word of the player.
     * @return
     */
    public byte[] getStateWord() {
        byte[] stateWord = new byte[Constraits.MAX_NUMBER_OF_STONES];

        for (int i = 0; i < Constraits.MAX_NUMBER_OF_STONES; i++) {
            stateWord[i] = (byte)(stones[i]);
        }

        return stateWord;
    }

    /**
     * Generates a new set of stones.
     * @param initPos The position of the first stone (1 or 2).
     */
    public void generateNewStones(int initPos) {
        int pos = initPos;
        if (initPos != 1 && initPos != 2) {
            pos = 2;
        }

        if (stones == null) {
            stones = new int[Constraits.MAX_NUMBER_OF_STONES];
        }

        for (int i = 0; i < 5; i++) {
            stones[i] = pos;
            pos += 2;
        }

    }
}
