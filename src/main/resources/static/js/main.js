let contentTypeHeader = {'Content-Type': 'application/json; charset=UTF-8'}

let sock
let stompClient
let activeGameId
let myNumber

renderAuthAndCreateConnectScreen()

function auth() {
    let playerName = $('#regName')[0].value
    if (playerName === undefined || playerName === '') {
        console.log("Empty name")
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
    if (name === undefined) {
        showErrorMessage("Name is undefined")
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

function connectGame() {
    let inviteCodeValue = $('#inviteCode').val().toUpperCase()
    connectAndSend({inviteCode: inviteCodeValue})
}

function processTopicMessage(message) {
    if (message['type'] === "LobbyConnectedMessage") {
        addPlayerToLobbyList(message['newPlayer'])
        $('#players-count').html(message['allPlayers'].length)
    }
    if (message['type'] === "PlayerLeftMessage") {
        deletePlayerFromLobby(message['player'])
    }
    if (message['type'] === "PlayerDisconnectedMessage") {
        playerDisconnected(message['player'])
    }
    if (message['type'] === "PlayerReconnectedMessage") {
        playerReconnected(message['player'])
    }
    if (message['type'] === "RoundStartedMessage") {
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
        renderEndRoundScreen(message.result)
    }
}

function processDirectMessage(message) {
    if (message['type'] === "ErrorMessage") {
        showErrorMessage(message['message']);
    }
    if (message['type'] === "PlayerPersonalInfoMessage") {
        updatePersonalInfo(message.coins, message.cards, message['isCurrentPlayer']);
    }
}

function processDirectInfoMessage(message) {
    if (message['type'] === "UserConnectedMessage") {
        myNumber = message['playerNumber']
        activeGameId = message['gameId']
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
    lobbyScreen.show()
    createAndConnectScreen.hide()
    authScreen.hide()
    gameScreen.hide()
    $('#result-screen').hide()
}
