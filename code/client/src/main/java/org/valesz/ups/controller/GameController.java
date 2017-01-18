package org.valesz.ups.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.message.received.*;
import org.valesz.ups.main.MainApp;
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

    public GameController(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    public void setView(MainPane view) {
        this.view = view;
    }

    public void setBoardView(Board boardView) {
        this.boardView = boardView;
    }

    /**
     * Waits for the message from server indicating start of game.
     */
    //TODO: test sending the ok message / timeout in server
    public void waitForStartGame() {
        tcpClient.getResponse(
                event -> {
                    // response
                    AbstractReceivedMessage response = tcpClient.getReceivingService().getValue();
                    StartGameReceivedMessage startGame = ReceivedMessageTypeResolver.isStartGame(response);
                    if (startGame == null) {
                        // wrong response
                        logger.error("Wrong message received. Expected START_GAME, received: "+response);
                        tcpClient.disconnect();
                        MainApp.viewController.displayLoginPane();
                    } else {
                        // send ok
                        tcpClient.sendOkMessage(event1 -> {},
                                                event1 -> {
                                                    logger.error("Error while sending OK message to server.");
                                                    tcpClient.disconnect();
                                                    MainApp.viewController.displayLoginPane();
                                                });

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
                    tcpClient.disconnect();
                    MainApp.viewController.displayLoginPane();
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

            if(Game.getInstance().currentPlayerOnLastField()) {
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
        if(Game.getInstance().alreadyThrown() && !Game.getInstance().canThrowAgain()) {
            thrown = Game.getInstance().throwSticks();
        } else {
            thrown = Game.getInstance().throwSticks();
            logger.debug("Thrown: "+thrown+"\n");
            view.addLogMessage("Hozeno: "+thrown+"\n");
        }

        if(Game.getInstance().canThrowAgain()) {
            view.addLogMessage("Házej znovu.\n");
            view.disableEndTurnButton();
            view.focusOnThrowButton();
        } else {
            view.enableEndTurnButton();
            view.focusOnEndTurnButton();
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
//                Game.WINNER,
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
     * The waiting for player message can also be received,
     * in that case, continue waiting for the new turn message.
     * End game message with the winner can also be received at this point.
     */
    public void waitForNewTurn() {
        logger.debug("Waiting for my turn.");
        // wait for START_TURN message
        tcpClient.getResponse(
                event -> {
                    // response
                    AbstractReceivedMessage response = tcpClient.getReceivingService().getValue();
                    StartTurnReceivedMessage startTurn = ReceivedMessageTypeResolver.isStartTurn(response);
                    EndGameReceivedMessage endGame = ReceivedMessageTypeResolver.isEndGame(response);
                    WaitingForPlayerReceivedMessage waitingForPlayer = ReceivedMessageTypeResolver.isWaitingForPlayer(response);

                    if( endGame != null) {
                        logger.debug("End of the game, winner is: "+endGame.getContent());
                        endGame(endGame.getContent());
                    } else if (waitingForPlayer != null) {
                        logger.debug("The game is waiting for player "+waitingForPlayer.getContent()+" to reconnect.");
                        waitForNewTurn();
                    } else if (startTurn != null) {
                        newTurn(startTurn.getFirstPlayerStones(), startTurn.getSecondPlayerStones());
                    } else {
                        // wrong response
                        logger.error("Wrong message received. Expected START_TURN, received: "+response);
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
        view.focusOnThrowButton();
    }

    /**
     * If the currently selected pawn is on the field 30, pawn can leave the board.
     */
    public void leaveBoard() {
        int currentPlayer = Game.getInstance().getCurrentPlayerNum();

        if(!Game.getInstance().isMyTurn()) {
            logger.warn("Not my turn.");
            view.addLogMessage("Nejsem na tahu!\n");
            return;
        }

        if(Game.getInstance().isAlreadyMoved()) {
            logger.warn("Already moved.");
            view.addLogMessage("V tomto tahu už jsem hrál.\n");
            return;
        }

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
        view.addLogMessage("Kámen opustil hrací plochu.\n");
        logger.debug("Pawn "+stone+" has leaved the game board");
        boardView.deselect();
        boardView.updateStones(Game.getInstance().getFirstPlayer().getStones(),
                               Game.getInstance().getSecondPlayer().getStones());
        view.hideLeaveButton();
    }

    /**
     * Ends the game, disconnects the tcpClient.
     */
    public void endGame(String winner) {
        Game.getInstance().resetGame();
        tcpClient.disconnect();
        view.displayEndGameDialog(winner);
    }
}
