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

function createGame() {
    $.post({
        url: 'api/game/create',
        headers: contentTypeHeader,
        dataType: 'json',
        data: JSON.stringify({}),
        success: function (data) {
            console.log(data)
            connectGameWithInviteCode(data.inviteCode)
        },
        xhrFields: {
            withCredentials: true
        }
    });
}

function connectGame() {
    let inviteCodeValue = $('#inviteCode').val()
    connectGameWithInviteCode(inviteCodeValue)
}

function connectGameWithInviteCode(inviteCodyValue) {
    $.post({
        url: 'api/game/connect',
        headers: contentTypeHeader,
        dataType: 'json',
        data: JSON.stringify({inviteCode: inviteCodyValue}),
        success: function (data, textStatus, xhr) {
            console.log(data)
            if (xhr.status === 200) {
                myNumber = data.playerNumber
                subscribe(data.gameId)
                renderLobbyScreen(data.isCreator, data.allPlayers, inviteCodyValue, data.params)
            }
        },
        xhrFields: {
            withCredentials: true
        }
    });
}

function subscribe(gameId) {
    sock = new SockJS("/no-thanks");
    stompClient = Stomp.over(sock);
    stompClient.connect({}, () => {
        stompClient.subscribe('/players/lobby/' + gameId + '/player', payload => {
            processDirectMessage(JSON.parse(payload.body))
        });
        stompClient.subscribe("/lobby/" + gameId, payload => {
            processTopicMessage(JSON.parse(payload.body))
        });
    });
    activeGameId = gameId
}

function processTopicMessage(message) {
    if (message['type'] === "LobbyConnectedMessage") {
        addPlayerToLobbyList(message['newPlayer'])
        $('#players-count').html(message['allPlayers'].length)
    }
    if (message['type'] === "RoundStartedMessage") {
        renderGameScreen(message.players, message['currentCard'], message['currentPlayerNumber'])
        updateLeftNumberCards(message['leftNumberCards'])
    }
    if (message['type'] === "TakeCardMessage") {
        if (message['playerNumber'] !== myNumber) {
            updateCardsForPlayer(message['playerNumber'],  message['allPlayerCards'])
        }
        updateLeftNumberCards(message['leftNumberCards'])
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
