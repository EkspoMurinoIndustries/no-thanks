package org.expo.nothanks.service

import org.expo.nothanks.model.event.output.UserConnectedMessage
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class NotificationService(
    val simpMessagingTemplate: SimpMessagingTemplate,
    val gameService: GameService
) {

    fun connectionNotify(gameId: UUID, playerName: String) {
        simpMessagingTemplate.convertAndSend(
            "/lobby/${gameId}",
            UserConnectedMessage(
                newPlayerName = playerName,
                allPlayers = gameService.getPlayersNames(gameId)
            )
        )
    }

}