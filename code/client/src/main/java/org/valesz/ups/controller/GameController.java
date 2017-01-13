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
import org.valesz.ups.ui.Stone;

import java.util.Arrays;

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

                        // update graphical components
                        view.startGame();
                        boardView.placeStones(Game.getInstance().getFirstPlayer().getStones(),
                                              Game.getInstance().getSecondPlayer().getStones());

                        // if this is the second player, wait for start turn message
                        if(!Game.getInstance().isMyTurn()) {
                            view.disableButtons();
                            waitForNewTurn();
                        }
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

            if(toField == 30) {
                view.showLeaveButton();
            } else {
                view.hideLeaveButton();
            }
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
        int thrown;
        if(Game.getInstance().alreadyThrown()) {
            thrown = Game.getInstance().throwSticks();
        } else {
            thrown = Game.getInstance().throwSticks();
            logger.debug("Thrown: "+thrown+"\n");
            view.addLogMessage("Hozeno: "+thrown+"\n");
        }

        if(Game.getInstance().canThrowAgain()) {
            view.disableEndTurnButton();
        } else {
            view.enableEndTurnButton();
        }

        return thrown;
    }

    /**
     * Ends the turn and waits for START_TURN message
     */
    public void endTurn() {
        if(Game.getInstance().canThrowAgain()) {
            logger.error("Cannot end turn if player can throw again!");
            view.addLogMessage("Hráč musí házet znovu!\n");
            return;
        }
        Game.getInstance().endTurn();
        view.disableButtons();
        sendEndTurnMessage(1);
        logger.debug("Ending turn with player 1 stones: "+ Arrays.toString(Game.getInstance().getFirstPlayer().getStones())+
                        ", player 2 stones: "+Arrays.toString(Game.getInstance().getSecondPlayer().getStones())+".");
    }

    /**
     * Separation of sending a message from the main endTurn() method
     */
    private void sendEndTurnMessage(int numOfAttempts) {
        final int noa = numOfAttempts+1;
        if(noa > 5) {
            logger.error("Maximum number of attempts reached.");
            return;
        }
        // send end turn message
        tcpClient.sendEndturn(
                Game.getInstance().getFirstPlayer().getStones(),
                Game.getInstance().getSecondPlayer().getStones(),
                event -> {
                    logger.trace("End turn message sent.");
                    waitForNewTurn();
                },
                event -> {
                    String error = tcpClient.getReceivingService().getException().getMessage();
                    logger.debug("Error while sending end turn message, trying again: "+error);
                    sendEndTurnMessage(noa);
                }
        );
    }


    /**
     * Waits until new turn message is received.
     */
    public void waitForNewTurn() {
        logger.debug("Waiting for my turn.");
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
        logger.debug("Starting new turn.");
        Game.getInstance().newTurn(firstPlayerStones, secondPlayerStones);
        view.newTurn();
        boardView.updateStones(Game.getInstance().getFirstPlayer().getStones(),
                Game.getInstance().getSecondPlayer().getStones());
    }

    /**
     * If the currently selected pawn is on the field 30, pawn can leave the board.
     */
    public void leaveBoard() {
        int currentPlayer = Game.getInstance().getCurrentPlayerNum();
        Stone stone = boardView.getSelected();
        if(stone == null) {
            logger.warn("No stone selected.");
            return;
        }

        if(stone.getPlayer() != currentPlayer) {
            logger.warn("Selected pawn doesn't belong to the current player!");
            return;
        }

        if(stone.getField() != Game.LAST_FIELD) {
            logger.warn("Selected pawn isn't on the last field!");
            return;
        }

        Game.getInstance().leaveBoard();
        view.addLogMessage("Kámen opustil hrací plochu.");
        logger.debug("Pawn "+stone+" has leaved the game board");
        boardView.deselect();
        boardView.updateStones(Game.getInstance().getFirstPlayer().getStones(),
                               Game.getInstance().getSecondPlayer().getStones());
    }
}
