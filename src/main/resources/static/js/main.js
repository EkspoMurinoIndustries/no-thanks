const settingsMinCardInput = $('#settings-min-card')
const settingsMaxCardInput = $('#settings-max-card')
const settingsRemovedCardsInput = $('#settings-removed-cards')
const settingsDefaultTokensInput = $('#settings-default-tokens')
const settingsTokensInput = $('#settings-tokens')
const settingsMenu = $('#settings-menu')
const settingsCardRange = $('#settings-card-range')

let contentTypeHeader = {'Content-Type': 'application/json; charset=UTF-8'}

let sock
let stompClient
let activeGameId
let myNumber
let amCreator = false
let currentRound = 0
let currentResults = {}
let currentParams = {}
let desiredConnection = null
let connectionPending = false
let connectionGeneration = 0
let reconnectTimer
let connectionTimeout
let gameSubscriptions = []
let backgroundedAt = null

renderAuthAndCreateConnectScreen()

function auth() {
    let playerName = $('#regName')[0].value
    if (isBlank(playerName)) {
        showErrorMessage("Name cannot be blank")
        return
    }
    $.post({
        url: 'api/authorization',
        headers: contentTypeHeader,
        dataType: 'json',
        data: JSON.stringify({name: playerName}),
        success: function () {
            renderAuthAndCreateConnectScreen()
        },
        xhrFields: {
            withCredentials: true
        }
    });
}

function setConnectionStatus(message = '') {
    $('#connection-status').text(message).toggle(Boolean(message))
    $('.background').prop('inert', Boolean(message))
}

function stopConnection() {
    connectionGeneration++
    clearTimeout(reconnectTimer)
    clearTimeout(connectionTimeout)
    connectionPending = false
    gameSubscriptions = []
    const previousSocket = sock
    sock = undefined
    stompClient = undefined
    if (previousSocket) previousSocket.close()
}

function unavailableLobby(message) {
    desiredConnection = null
    stopConnection()
    activeGameId = undefined
    amCreator = false
    currentRound = 0
    currentResults = {}
    closeAvatarEditor()
    closeGameResults()
    closeSettings()
    $('#result-screen').hide()
    window.history.replaceState({}, '', '/')
    setConnectionStatus()
    renderAuthAndCreateConnectScreen()
    showErrorMessage(message)
}

function connectAndSend(message) {
    const name = parseCookie()['no-thanks-name']
    if (isBlank(name)) {
        showErrorMessage('Name cannot be blank')
        return
    }
    desiredConnection = {...message, name, avatar: savedAvatar()}
    if (connectionPending) return
    if (stompClient && stompClient.connected) {
        connectionPending = true
        setConnectionStatus('Connecting to lobby...')
        connectionTimeout = setTimeout(() => {
            stopConnection()
            openConnection()
        }, 3000)
        stompClient.send('/app/lobby/input/connect', {}, JSON.stringify(desiredConnection))
        return
    }
    openConnection()
}

function openConnection() {
    if (!desiredConnection || connectionPending || document.hidden) return
    stopConnection()
    connectionPending = true
    desiredConnection = {...desiredConnection, name: parseCookie()['no-thanks-name'], avatar: savedAvatar()}
    const generation = connectionGeneration
    const socket = new SockJS('/no-thanks')
    const client = Stomp.over(socket)
    sock = socket
    stompClient = client
    setConnectionStatus(activeGameId ? 'Reconnecting to your game...' : 'Connecting to lobby...')
    const retry = () => {
        if (generation !== connectionGeneration) return
        stopConnection()
        closeAvatarEditor()
        setConnectionStatus('Connection lost. Reconnecting...')
        reconnectTimer = setTimeout(openConnection, 2000)
    }
    connectionTimeout = setTimeout(retry, 10000)
    client.connect({}, () => {
        if (generation !== connectionGeneration) return
        client.subscribe('/players/lobby/info', payload => {
            if (generation === connectionGeneration) processDirectInfoMessage(JSON.parse(payload.body))
        })
        client.send('/app/lobby/input/connect', {}, JSON.stringify(desiredConnection))
    }, retry)
}

document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        backgroundedAt = Date.now()
        return
    }
    if (desiredConnection && backgroundedAt !== null && Date.now() - backgroundedAt > 1000) {
        // Refresh state first; replace a stale socket if it cannot return a snapshot.
        connectAndSend(desiredConnection)
    } else if (desiredConnection && (!stompClient || !stompClient.connected)) {
        openConnection()
    }
    backgroundedAt = null
})
window.addEventListener('online', () => {
    if (desiredConnection && (!stompClient || !stompClient.connected)) openConnection()
})

function createGame() {
    connectAndSend({createGame: true})
}

function connectGame(inviteCode = undefined) {
    if (inviteCode === undefined) {
        inviteCode = $('#inviteCode').val().toUpperCase()
    }
    connectAndSend({inviteCode: inviteCode})
}

function processTopicMessage(message) {
    if (message.type === 'LobbyClosedMessage') {
        unavailableLobby('The lobby closed because the host did not reconnect in time.')
        return
    }
    if (message.type === 'PlayerAvatarChangedMessage') {
        updateAvatar(message.playerNumber, message.avatar)
        if (message.playerNumber === myNumber) {
            rememberAvatar(message.avatar)
            closeAvatarEditor()
        }
    }
    if (message['type'] === 'ParamsChangedMessage') {
        currentParams = message.newParams
        closeSettings()
    }
    if (message['type'] === "LobbyConnectedMessage") {
        addPlayerToLobbyList(message['newPlayer'])
        updatePlayerCountInLobby(message['allPlayers'].length)
        updateResultsFromPlayers(message['allPlayers'])
    }
    if (message['type'] === "PlayerLeftMessage") {
        deletePlayerFromLobby(message['player'], message['remainingPlayersNumber'])
        removePlayerFromResults(message['player']['number'])
    }
    if (message['type'] === "PlayerDisconnectedMessage") {
        playerDisconnected(message['player'])
    }
    if (message['type'] === "PlayerReconnectedMessage") {
        playerReconnected(message['player'])
    }
    if (message['type'] === "RoundStartedMessage") {
        updateRoundAndResults(message['round'], message['result'])
        renderGameScreen(message.players, message['currentCard'], message['currentPlayerNumber'], message['remainingNumberCards'])
    }
    if (message['type'] === "TakeCardMessage") {
        if (message['playerNumber'] !== myNumber) {
            updateCardsForPlayer(message['playerNumber'],  message['allPlayerCards'])
        }
        updateRemainingNumberCards(message['remainingNumberCards'])
        currentCardCoinsBlock.html('0')
        renderCurrentCard(message['newCardNumber'])
    }
    if (message['type'] === "PutCoinMessage") {
        currentCardCoinsBlock.html(message['currentCardCoins'])
        setCurrentTurnPlayer(message['newCurrentPlayerNumber'])
    }
    if (message['type'] === "EndRoundMessage") {
        updateRoundAndResults(message['round'], message['result'])
        renderEndRoundScreen(message.result, message['removedCards'])
    }
    if (message['type'] === "RoundAbortedMessage") {
        updateRoundAndResults(message['round'], message['result'])
        returnToLobby()
    }
    if (message['type'] === "ScoreResetMessage") {
        updateRoundAndResults(message['round'], message['result'])
    }
    if (message['type'] === "PlayerNameChangedMessage") {
        console.log(message)
        renderNewName(message)
    }

}

function processDirectMessage(message) {
    if (message['type'] === "ErrorMessage") {
        if (avatarEditor[0].open) {
            avatarError(message.message)
            return
        }
        showErrorMessage(message['message']);
    }
    if (message['type'] === "PlayerPersonalInfoMessage") {
        updatePersonalInfo(message.coins, message.cards, message['isCurrentPlayer']);
    }
    if (message['type'] === "PersonalPlayerNameChangedMessage") {
        console.log(message)
        Cookies.set('no-thanks-name', message['newName'])
    }
}

function processDirectInfoMessage(message) {
    if (message.type === 'ErrorMessage') {
        unavailableLobby(message.message)
        return
    }
    if (message['type'] === "UserConnectedMessage") {
        clearTimeout(connectionTimeout)
        clearTimeout(reconnectTimer)
        connectionPending = false
        desiredConnection = {inviteCode: message.inviteCode, name: parseCookie()['no-thanks-name'], avatar: savedAvatar()}
        gameSubscriptions.forEach(subscription => subscription.unsubscribe())
        gameSubscriptions = []
        window.history.replaceState({}, '', message.inviteCode)
        myNumber = message['playerNumber']
        activeGameId = message['gameId']
        amCreator = message['isCreator']
        updateRoundAndResults(message['round'], message['result'])
        gameSubscriptions.push(stompClient.subscribe('/players/lobby/' + activeGameId + '/player', payload => {
            processDirectMessage(JSON.parse(payload.body))
        }))
        gameSubscriptions.push(stompClient.subscribe('/lobby/' + activeGameId, payload => {
            processTopicMessage(JSON.parse(payload.body))
        }))
        setConnectionStatus()
        $('#result-screen').hide()
        renderLobbyScreen(message['isCreator'], message['players'], message['inviteCode'], message['params'])
        if (message['isStarted'] === true) {
            let game = message['gameStatus']
            renderGameScreen(game.players, game['currentCard'], game['currentPlayerNumber'], game['remainingNumberCards'], game['currentCardCoin'])
        }
    }
}

function startGame() {
    stompClient.send('/app/lobby/input/' + activeGameId + '/round', {}, JSON.stringify({wantToStart: true}))
}

function openSettings() {
    if (!amCreator || !lobbyScreen.is(':visible')) return
    settingsMinCardInput.val(currentParams.minCard)
    settingsMaxCardInput.val(currentParams.maxCard)
    settingsRemovedCardsInput.val(currentParams.removedCards)
    settingsDefaultTokensInput.prop('checked', currentParams.useDefaultTokens)
    settingsTokensInput.val(currentParams.useDefaultTokens ? '' : currentParams.initialCoinsCount)
        .prop('required', !currentParams.useDefaultTokens)
    updateSettingsCardRange()
    settingsMenu.show()
    settingsMinCardInput.trigger('focus')
}

function closeSettings() {
    settingsMenu.hide()
}

function updateSettingsCardRange() {
    const maxCard = Number(settingsMaxCardInput.val())
    const minCard = Number(settingsMinCardInput.val())
    settingsMinCardInput.attr('max', maxCard)
    const count = maxCard - minCard + 1
    settingsRemovedCardsInput.attr('max', Math.max(0, count - 1))
    settingsCardRange.text(`Cards ${minCard}–${maxCard} (${count} cards before removal).`)
}

settingsMinCardInput.add(settingsMaxCardInput).on('input', updateSettingsCardRange)

settingsTokensInput.on('input', function () {
    settingsDefaultTokensInput.prop('checked', false)
    $(this).prop('required', true)
})

function toggleDefaultTokens() {
    const automatic = settingsDefaultTokensInput.prop('checked')
    settingsTokensInput.prop('required', !automatic)
    if (automatic) settingsTokensInput.val('')
}

function resetSettings() {
    if (!amCreator || !lobbyScreen.is(':visible')) return
    stompClient.send('/app/lobby/input/' + activeGameId + '/round', {},
        JSON.stringify({newParams: {resetToDefaults: true}}))
}

function saveSettings() {
    if (!amCreator || !lobbyScreen.is(':visible')) return
    updateSettingsCardRange()
    if (!settingsMenu[0].reportValidity()) return
    stompClient.send('/app/lobby/input/' + activeGameId + '/round', {}, JSON.stringify({newParams: {
        minCard: Number(settingsMinCardInput.val()),
        maxCard: Number(settingsMaxCardInput.val()),
        removedCards: Number(settingsRemovedCardsInput.val()),
        useDefaultTokens: settingsDefaultTokensInput.prop('checked'),
        defaultCoinsCount: settingsDefaultTokensInput.prop('checked') ? null : Number(settingsTokensInput.val())
    }}))
}

function resetScore() {
    if (window.confirm('Reset every player\'s score and start again from round 1?')) {
        stompClient.send('/app/lobby/input/' + activeGameId + '/round', {}, JSON.stringify({scoreReset: true}))
    }
}

function abortRound() {
    if (window.confirm('End this round and return everyone to the lobby? The in-progress score will be discarded.')) {
        stompClient.send('/app/lobby/input/' + activeGameId + '/round', {}, JSON.stringify({wantToAbort: true}))
    }
}

function putCoin() {
    stompClient.send('/app/lobby/input/' + activeGameId + '/turn', {}, JSON.stringify({action: 'putCoin'}))
}

function takeCard() {
    stompClient.send('/app/lobby/input/' + activeGameId + '/turn', {}, JSON.stringify({action: 'takeCard'}))
}

function parseCookie() {
    if (document.cookie === '') {
        return {}
    }
    return document.cookie
        .split(';')
        .map(v => v.split('='))
        .reduce((acc, v) => {
            acc[decodeURIComponent(v[0].trim())] = decodeURIComponent(v[1].trim());
            return acc;
        }, {});
}

function returnToLobby() {
    closeGameResults()
    lobbyScreen.show()
    createAndConnectScreen.hide()
    authScreen.hide()
    gameScreen.hide()
    $('#result-screen').hide()
}

function changeName() {
    let newName = $('#newName')[0].value
    if (isBlank(newName)) {
        showErrorMessage("Name cannot be blank")
        return
    }
    stompClient.send('/app/lobby/input/name', {}, JSON.stringify({newName: newName}))
    $('#new-name-block').remove()
}
