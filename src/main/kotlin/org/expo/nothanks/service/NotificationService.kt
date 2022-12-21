package org.expo.nothanks.service

import mu.KLogging
import org.expo.nothanks.model.event.output.*
import org.expo.nothanks.model.lobby.Lobby
import org.expo.nothanks.utils.*
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class NotificationService(
    val simpMessagingTemplate: SimpMessagingTemplate
) {

    fun scoreReset(lobby: Lobby) {
        messageToTopic(lobby, ScoreResetMessage())
    }

    fun paramsChanged(lobby: Lobby) {
        messageToTopic(lobby, ParamsChangedMessage(
            newParams = lobby.params
        ))
    }

    fun lobbyClosed(gameId: UUID) {
        messageToTopic(gameId, LobbyClosedMessage())
    }

    fun playerDisconnected(lobby: Lobby, playerId: UUID) {
        val playerInLobby = lobby.getPlayerInLobby(playerId)
        messageToTopic(lobby, PlayerDisconnectedMessage(
            playerName = playerInLobby.name,
            playerNumber = playerInLobby.number,
            isGameStarted = lobby.isGameStarted()
        ))
    }

    fun lobbyConnection(lobby: Lobby, playerId: UUID) {
        messageToTopic(lobby, LobbyConnectedMessage(
            newPlayer = lobby.getPlayerInLobby(playerId),
            allPlayers = lobby.getPlayersInLobby()
        ))
    }

    fun gameConnection(lobby: Lobby, playerId: UUID) {
        val playerInLobby = lobby.getPlayerInLobby(playerId)
        messageToTopic(lobby, PlayerReconnectedMessage(
            playerName = playerInLobby.name,
            playerNumber = playerInLobby.number,
            canContinue = lobby.getGame().isValid()
        ))
    }

    fun gameStarted(lobby: Lobby) {
        messageToTopic(lobby, RoundStartedMessage(
            currentPlayerNumber = lobby.getGame().currentPlayerNumber(),
            players = lobby.getPlayersInGame(),
            currentCard = lobby.getGame().currentCard(),
            leftNumberCards = lobby.getGame().leftNumberCards()
        ))
    }

    fun putCoin(lobby: Lobby) {
        messageToTopic(lobby, PutCoinMessage(
            newCurrentPlayerNumber = lobby.getGame().currentPlayerNumber(),
            playerNumber = lobby.getGame().previousPlayer().number,
            currentCardCoins = lobby.getGame().currentCardCoins
        ))
    }

    fun takeCard(lobby: Lobby) {
        messageToTopic(lobby, TakeCardMessage(
            playerNumber = lobby.getGame().currentPlayerNumber(),
            takenCard = lobby.getGame().previousCard(),
            newCardNumber = lobby.getGame().currentCardNumber(),
            allPlayerCards = lobby.getGame().currentPlayer().cards,
            leftNumberCards = lobby.getGame().leftNumberCards()
        ))
    }

    fun endRound(lobby: Lobby) {
        messageToTopic(lobby, EndRoundMessage(
            result = lobby.getResult()
        ))
    }

    fun updateInfo(lobby: Lobby) {
        updateInfo(lobby, lobby.getGame().currentPlayer().id)
    }

    fun updateInfoForPrevious(lobby: Lobby) {
        updateInfo(lobby, lobby.getGame().previousPlayer().id)
    }

    fun updateInfo(lobby: Lobby, playerId: UUID) {
        val player = lobby.getGame().getPlayer(playerId)
        messageToUser(lobby, playerId, PlayerPersonalInfoMessage(
            coins = player.coins,
            cards = player.cards,
            isCurrentPlayer = lobby.getGame().isPlayerCurrent(playerId)
        ))
    }

    fun sendErrorToUser(gameId: UUID, playerId: UUID, message: String) {
        simpMessagingTemplate.convertAndSendToUser(
            playerId.toString(),
            "/lobby/${gameId}/player",
            ErrorMessage(message)
        )
    }

    private fun messageToTopic(lobby: Lobby, message: OutputMessage) {
        messageToTopic(lobby.gameId, message)
    }

    private fun messageToTopic(gameId: UUID, message: OutputMessage) {
        simpMessagingTemplate.convertAndSend("/lobby/$gameId", message)
    }

    private fun messageToUser(lobby: Lobby, playerId: UUID, message: OutputMessage) {
        simpMessagingTemplate.convertAndSendToUser(
            playerId.toString(),
            "/lobby/${lobby.gameId}/player",
            message
        )
    }

    private companion object : KLogging()

}