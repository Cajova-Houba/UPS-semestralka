package org.valesz.ups.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.valesz.ups.common.error.Error;
import org.valesz.ups.common.error.ErrorMessages;
import org.valesz.ups.common.error.MaxAttemptsReached;
import org.valesz.ups.common.message.received.*;
import org.valesz.ups.main.MainApp;
import org.valesz.ups.model.game.Game;
import org.valesz.ups.network.AbstractReceiver;
import org.valesz.ups.network.TcpClient;
import org.valesz.ups.ui.Board;
import org.valesz.ups.ui.MainPane;
import org.valesz.ups.ui.Stone;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Game controller. Starts game, makes turns etc.
 *
 * @author Zdenek Vales
 */
public class GameController {

    private static final Logger logger = LogManager.getLogger(GameController.class);

    /**
     * Max time for turn = 2 minutes.
     */
    public static final int MAX_TURN_TIME = 2*60;

    private MainPane view;
    private Board boardView;
    private TcpClient tcpClient;
    private Timer timer;
    private int timeCntr;
    private boolean timerPassed;
    private LoginController loginController;

    public GameController(TcpClient tcpClient) {

        this.tcpClient = tcpClient;
    }

    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
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
    public void waitForStartGame() {
        view.disableButtons();
        view.addLogMessage("Čekám na dalšího hráče...\n");
        tcpClient.waitForStartGame(
                event -> {
                    // response
                    AbstractReceivedMessage response = tcpClient.getPreStartReceiverService().getValue();
                    StartGameReceivedMessage startGame = ReceivedMessageTypeResolver.isStartGame(response);
                    if (startGame == null) {
                        // wrong response
                        logger.error("Wrong message received. Expected START_GAME, received: "+response);
                        tcpClient.disconnect();
                        MainApp.viewController.displayLoginPane();
                    } else {
                        // ok
                        logger.debug("Start game received.");
                        Game.getInstance().startGame(startGame.getFirstNickname(),startGame.getSecondNickname());
                        logger.info("The game has started");

                        // update graphical components
                        view.startGame();
                        displayStartGameInfo();

                        // if this is the second player, wait for start turn message
                        if(!Game.getInstance().isMyTurn()) {
                            view.disableButtons();
                            waitForNewTurn();
                        } else {
                            newTurn(true,
                                    Game.getInstance().getFirstPlayer().getStones(),
                                    Game.getInstance().getSecondPlayer().getStones());
                        }
                    }
                },

                // fail
                event -> {
                    Throwable ex = tcpClient.getPreStartReceiverService().getException();
                    String msg = ex == null ? "no exception" : ex.getMessage();
                    if(ex instanceof SocketTimeoutException) {
                        handleFailure("Server stopped responding and is probably dead.", "Server přestal odpovídat.");
                    } else if (ex instanceof MaxAttemptsReached){
                        handleFailure("Maximum number of attempts to receive start game message reached.", "Maximální počet pokusů na start hry dosažen.");
                    } else {
                        handleFailure("Error while waiting for start game message: "+msg, ErrorMessages.COMMUNICATION_BREAKDOWN);
                    }
                }
        );
    }

    public void displayStartGameInfo() {
        view.addLogMessage("NOVÁ HRA\n");
        view.addLogMessage("Na každý tah máte 2 minuty, pak bude automaticky přepnut.\n");
        view.addLogMessage("Pokud pade 4, nebo 5, musíte házet znovu.\n");
        view.addLogMessage("Můžete táhnout dopředu i dozadu, ale nemusíte táhnout vůbec.\n");
        view.addLogMessage("Začíná hráč s bílými kameny. Cílem hry, je dostat všechny kameny z hracího pole.\n");
        view.addLogMessage("Pokud je na poli kámen druhého hráče, můžete se s ním vyměnit. Pokud má ale hráč\n");
        view.addLogMessage("dva a více kamenů za sebou, výměna není možná.\n\n");
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

            view.addLogMessage("Kámen posunut z "+fromField+" na "+toField+".\n");

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

            if(Game.getInstance().canThrowAgain()) {
                view.addLogMessage("Hozeno: "+thrown+". Házej znovu.\n");
            } else {
                view.addLogMessage("Hozeno: "+thrown+".\n");
            }
        }

        if(Game.getInstance().canThrowAgain()) {
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
        if(Game.getInstance().canThrowAgain() && !timerPassed) {
            logger.error("Cannot end turn if player can throw again!");
            view.addLogMessage("Hráč musí házet znovu!\n");
            return;
        }

        stopTimer();
        view.resetTimerText();
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
        try {
            tcpClient.sendEndTurnMessage(
                    Game.getInstance().getFirstPlayer().getStones(),
                    Game.getInstance().getSecondPlayer().getStones());
        } catch (IOException e) {
            logger.error("Exception while sending the end turn: "+e.getMessage());
            tcpClient.disconnect();
            MainApp.viewController.displayLoginPane();
            displayErroMessageLoginPane("Chyba při odesílání tahové zprávy na server.");
            return;
        }

        // wait for end turn confirm
        tcpClient.waitForTurnConfirm(
                event -> {
                    AbstractReceivedMessage message = tcpClient.getPostStartReceiverService().getValue();
                    OkReceivedMessage ok = ReceivedMessageTypeResolver.isOk(message);
                    EndGameReceivedMessage endGame = ReceivedMessageTypeResolver.isEndGame(message);
                    ErrorReceivedMessage err = ReceivedMessageTypeResolver.isError(message);

                    if (err != null) {
                        logger.debug("Error while validating turn: "+err.getContent().toString());
                        view.addLogMessage("Server neuznal tvůj tah a považuje ho za propadlý.\n");
                        waitForNewTurn();
                    } else if (endGame != null) {
                        endGame(endGame.getContent());
                    } else {
                        logger.trace("Turn validation ok.");
                        waitForNewTurn();
                    }
                },

                event -> {
                    // failure
                    Throwable ex = tcpClient.getPreStartReceiverService().getException();
                    String msg = ex == null ? "no exception" : ex.getMessage();
                    if(ex instanceof SocketTimeoutException) {
                        handleFailure("Server stopped responding and is probably dead.", "Server přestal odpovídat.");
                    } else if (ex instanceof MaxAttemptsReached){
                        handleFailure("Maximum number of attempts to receive end turn confirmation reached.", "Maximální počet pokusů na potvrzení tahu dosažen.");
                    } else {
                        handleFailure("Error while waiting for end turn confirm message: "+msg, ErrorMessages.COMMUNICATION_BREAKDOWN);
                    }
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
        tcpClient.waitForMyTurn(
                event -> {
                    // response
                    AbstractReceivedMessage response = tcpClient.getPostStartReceiverService().getValue();
                    StartTurnReceivedMessage startTurn = ReceivedMessageTypeResolver.isStartTurn(response);
                    EndGameReceivedMessage endGame = ReceivedMessageTypeResolver.isEndGame(response);

                    if( endGame != null) {
                        endGame(endGame.getContent());
                    } else if (startTurn != null) {
                        newTurn(false,startTurn.getFirstPlayerStones(), startTurn.getSecondPlayerStones());
                    } else {
                        // wrong response
                        logger.error("Wrong message received. Expected START_TURN, received: "+response);
                    }
                },
                event -> {
                    // failure
                    Throwable ex = tcpClient.getPreStartReceiverService().getException();
                    String msg = ex == null ? "no exception" : ex.getMessage();
                    if(ex instanceof SocketTimeoutException) {
                        handleFailure("Server stopped responding and is probably dead.", "Server přestal odpovídat.");
                    } else if (ex instanceof MaxAttemptsReached){
                        handleFailure("Maximum number of attempts to receive start turn message reached.", "Maximální počet pokusů na nový tah dosažen.");
                    } else {
                        handleFailure("Error while waiting for start turn message: "+msg, ErrorMessages.COMMUNICATION_BREAKDOWN);
                    }
                }
        );
    }

    /**
     * Starts the new turn with updated stones.
     * Also starts the turn timer.
     * Also start the thread for receiving messages from server.
     * @param firstTurn If true, Game.newTurn() won't be called (and the turn won't be switched).
     * @param firstPlayerStones
     * @param secondPlayerStones
     */
    public void newTurn(boolean firstTurn, int[] firstPlayerStones, int[] secondPlayerStones) {
        logger.debug("Starting new turn.");
        if(!firstTurn) {
            Game.getInstance().newTurn(firstPlayerStones, secondPlayerStones);
        }
        view.newTurn();
        boardView.updateStones(Game.getInstance().getFirstPlayer().getStones(),
                Game.getInstance().getSecondPlayer().getStones());
        view.focusOnThrowButton();
        tcpClient.handleWhileTurn(
                event -> {
                    // if the end game is received
                    AbstractReceivedMessage message = tcpClient.getPostStartReceiverService().getValue();
                    EndGameReceivedMessage endGame = ReceivedMessageTypeResolver.isEndGame(message);
                    if(endGame != null) {
                        endGame(endGame.getContent());
                    }
                },
                event -> {
                Throwable ex = tcpClient.getPostStartReceiverService().getException();
                String msg = ex == null ? "no exception" : ex.getMessage();
                handleFailure("Exception while player making his turn: "+msg, ErrorMessages.COMMUNICATION_BREAKDOWN);
        });
        startTimer();
    }

    public void startTimer() {
        timeCntr = 0;
        timerPassed = false;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                turnTimerAction();
            }
            // tick every second
        }, 1000, 1000);
    }

    public void stopTimer() {
        if(timer == null) {
            return;
        }
        timer.cancel();
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
        logger.debug("End of the game, winner is: "+winner);
        stopTimer();
        Game.getInstance().resetGame();
        tcpClient.disconnect();
        view.displayEndGameDialog(winner);
    }

    /**
     * Turn must be done in 2 minutes, after that
     * it's automatically ended.
     */
    public void turnTimerAction() {
        timeCntr++;
        view.updateTimerText(timeCntr);
        if(timeCntr == MAX_TURN_TIME) {
            timerPassed = true;
            logger.debug("Time for turn expired.");
            this.endTurn();
        }
    }

    public void displayErroMessageLoginPane(String message) {
        if(loginController != null) {
            loginController.displayErrorMessage(message);
        }
    }

    /**
     * Immediately stops the game, switches to login pane and show error message.
     * @param logMessage
     * @param displayError
     */
    private void handleFailure(String logMessage, String displayError) {
        logger.error(logMessage);
        stopTimer();
        tcpClient.disconnect();
        MainApp.viewController.displayLoginPane();
        displayErroMessageLoginPane(displayError);
    }
}
