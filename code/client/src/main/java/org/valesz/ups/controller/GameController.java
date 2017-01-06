package org.valesz.ups.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.message.received.AbstractReceivedMessage;
import org.valesz.ups.common.message.received.ReceivedMessageTypeResolver;
import org.valesz.ups.common.message.received.StartGameReceivedMessage;
import org.valesz.ups.common.message.received.StartTurnReceivedMessage;
import org.valesz.ups.model.game.Game;
import org.valesz.ups.network.TcpClient;
import org.valesz.ups.ui.Board;
import org.valesz.ups.ui.MainPane;

/**
 * Game controller. Starts game, makes turns etc.
 *
 * @author Zdenek Vales
 */
public class GameController {

    private static final Logger logger = LogManager.getLogger(GameController.class);

    private MainPane view;
    private Board boardView;
    private TcpClient tcpClient;

    public GameController(MainPane view, Board boardView, TcpClient tcpClient) {
        this.view = view;
        this.boardView = boardView;
        this.tcpClient = tcpClient;
    }

    /**
     * Waits for the message from server indicating start of game.
     */
    public void waitForStartGame() {
        tcpClient.getResponse(
                event -> {
                    // response
                    AbstractReceivedMessage response = tcpClient.getReceivingService().getValue();
                    StartGameReceivedMessage startGame = ReceivedMessageTypeResolver.isStartGame(response);
                    if (startGame == null) {
                        // wrong response
                        logger.error("Wrong message received. Expected START_GAME, received: "+response);
                    } else {
                        Game.getInstance().startGame(startGame.getFirstNickname(),startGame.getSecondNickname());
                        logger.info("The game has started");

                        //update graphical components
                        view.startGame();
                        boardView.placeStones(Game.getInstance().getFirstPlayer().getStones(),
                                              Game.getInstance().getSecondPlayer().getStones());
                    }
                },
                event -> {
                    // failure
                    String error = tcpClient.getReceivingService().getException().getMessage();
                    logger.debug("Error while receiving response: "+error);
                }
        );
    }

    /**
     * Tries to move the stone of the current player on the fromField to toFiled.
     * If the stone is moved, true is returned and board view is updated.
     *
     * @param fromField
     * @param toField
     * @return
     */
    public boolean move(int fromField, int toField) {

        // check if the sticks were already thrown
        if(!Game.getInstance().alreadyThrown()) {
            logger.warn("Sticks not thrown yet!");
            return false;
        }

        // check that the player hasn't moved in this turn yet
        if(Game.getInstance().isAlreadyMoved()) {
           logger.warn("Player already moved his stone!");
           return false;
        }

        // check that the move will be equal to the thrown value
        if(!Game.getInstance().isMoveLengthOk(fromField, toField)) {
            logger.warn("Wrong move!");
            return false;
        }

        // move stones and update board
        if(Game.getInstance().moveStone(fromField, toField)) {
            boardView.updateStones(Game.getInstance().getFirstPlayer().getStones(),
                    Game.getInstance().getSecondPlayer().getStones());
            return true;
        } else {
            logger.warn("Turn from "+fromField+" to "+toField+" not possible.");
        }

        return false;
    }

    /**
     * Throws sticks and returns the value.
     * @return
     */
    public int throwSticks() {
        return Game.getInstance().throwSticks();
    }

    /**
     * Ends the turn and waits for START_TURN message
     */
    public void endTurn() {
        Game.getInstance().endTurn();
        view.disableButtons();

        // wait for START_TURN message
        tcpClient.getResponse(
                event -> {
                    // response
                    AbstractReceivedMessage response = tcpClient.getReceivingService().getValue();
                    StartTurnReceivedMessage startTurn = ReceivedMessageTypeResolver.isStartTurn(response);
                    if (startTurn == null) {
                        // wrong response
                        logger.error("Wrong message received. Expected START_TURN, received: "+response);
                    } else {
                        newTurn(startTurn.getFirstPlayerStones(), startTurn.getSecondPlayerStones());
                    }
                },
                event -> {
                    // failure
                    String error = tcpClient.getReceivingService().getException().getMessage();
                    logger.debug("Error while receiving response: "+error);
                }
        );
    }

    /**
     * Starts the new turn with updated stones.
     * @param firstPlayerStones
     * @param secondPlayerStones
     */
    public void newTurn(int[] firstPlayerStones, int[] secondPlayerStones) {
        Game.getInstance().newTurn(firstPlayerStones, secondPlayerStones);
        view.enableButtons();
        boardView.updateStones(Game.getInstance().getFirstPlayer().getStones(),
                Game.getInstance().getSecondPlayer().getStones());
    }
}
