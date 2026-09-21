let contentTypeHeader = {'Content-Type': 'application/json; charset=UTF-8'}

let sock
let stompClient
let activeGameId
let myNumber
let amCreator = false
let currentRound = 0
let currentResults = {}
let currentParams = {}

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

function connectAndSend(message) {
    let cookies = parseCookie()
    let name = cookies['no-thanks-name']
    if (isBlank(name)) {
        showErrorMessage("Name cannot be blank")
        return
    }
    message.name = name
    if (sock !== undefined && stompClient !== undefined && !stompClient.connected) {
        sock = new SockJS("/no-thanks");
        stompClient = Stomp.over(sock);
    }
    if (sock === undefined) {
        sock = new SockJS("/no-thanks");
    }
    if (stompClient === undefined) {
        stompClient = Stomp.over(sock);
    }
    if (stompClient.connected) {
        stompClient.send('/app/lobby/input/connect', {}, JSON.stringify(message))
    } else {
        stompClient.connect({}, () => {
            stompClient.subscribe('/players/lobby/info', payload => {
                processDirectInfoMessage(JSON.parse(payload.body))
            });
            stompClient.send('/app/lobby/input/connect', {}, JSON.stringify(message))
        }, function(message) {
            if (message.startsWith("Whoops! Lost connection to")) {
                renderAuthAndCreateConnectScreen()
                showErrorMessage("You have been disconnected")
            }
        });
    }
}

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
        currentCardBlock.html(message['newCardNumber'])
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
    if (message['type'] === "UserConnectedMessage") {
        window.history.pushState({},"", message['inviteCode']);
        myNumber = message['playerNumber']
        activeGameId = message['gameId']
        amCreator = message['isCreator']
        updateRoundAndResults(message['round'], message['result'])
        stompClient.subscribe('/players/lobby/' + activeGameId + '/player', payload => {
            processDirectMessage(JSON.parse(payload.body))
        });
        stompClient.subscribe("/lobby/" + activeGameId, payload => {
            processTopicMessage(JSON.parse(payload.body))
        });
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
    $('#settings-min-card').val(currentParams.minCard)
    $('#settings-max-card').val(currentParams.maxCard)
    $('#settings-removed-cards').val(currentParams.removedCards)
    $('#settings-default-tokens').prop('checked', currentParams.useDefaultTokens)
    $('#settings-tokens').val(currentParams.useDefaultTokens ? '' : currentParams.initialCoinsCount)
        .prop('required', !currentParams.useDefaultTokens)
    updateSettingsCardRange()
    $('#settings-menu').show()
    $('#settings-min-card').trigger('focus')
}

function closeSettings() {
    $('#settings-menu').hide()
}

function updateSettingsCardRange() {
    const maxCard = Number($('#settings-max-card').val())
    const minCard = Number($('#settings-min-card').val())
    $('#settings-min-card').attr('max', maxCard)
    const count = maxCard - minCard + 1
    $('#settings-removed-cards').attr('max', Math.max(0, count - 1))
    $('#settings-card-range').text(`Cards ${minCard}–${maxCard} (${count} cards before removal).`)
}

$('#settings-min-card, #settings-max-card').on('input', updateSettingsCardRange)

$('#settings-tokens').on('input', function () {
    $('#settings-default-tokens').prop('checked', false)
    $(this).prop('required', true)
})

function toggleDefaultTokens() {
    const automatic = $('#settings-default-tokens').prop('checked')
    $('#settings-tokens').prop('required', !automatic)
    if (automatic) $('#settings-tokens').val('')
}

function resetSettings() {
    if (!amCreator || !lobbyScreen.is(':visible')) return
    stompClient.send('/app/lobby/input/' + activeGameId + '/round', {},
        JSON.stringify({newParams: {resetToDefaults: true}}))
}

function saveSettings() {
    if (!amCreator || !lobbyScreen.is(':visible')) return
    updateSettingsCardRange()
    if (!$('#settings-menu')[0].reportValidity()) return
    stompClient.send('/app/lobby/input/' + activeGameId + '/round', {}, JSON.stringify({newParams: {
        minCard: Number($('#settings-min-card').val()),
        maxCard: Number($('#settings-max-card').val()),
        removedCards: Number($('#settings-removed-cards').val()),
        useDefaultTokens: $('#settings-default-tokens').prop('checked'),
        defaultCoinsCount: $('#settings-default-tokens').prop('checked') ? null : Number($('#settings-tokens').val())
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
