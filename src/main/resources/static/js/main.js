let mainBlock = $('#main')
let authBlock = $('#auth')
let infoBlock = $('#player-info')
let lobbyBlock = $('#lobby')
let startGameButton = $('#start-game-button')
let playersList = $('#players-list')

let contentTypeHeader = {'Content-Type': 'application/json; charset=UTF-8'}

renderMainWindow()

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
            renderMainWindow()
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
            if (xhr.status === 200 && data.status === 'SUCCESS') {
                subscribe(data.gameId)
                renderLobby(data.isCreator, data.allPlayers)
            }
        },
        xhrFields: {
            withCredentials: true
        }
    });
}

function subscribe(gameId) {
    let sock = new SockJS("/no-thanks");
    let client = Stomp.over(sock);
    client.connect({}, () => {
        client.subscribe("/lobby/" + gameId, payload => {
            console.log(payload)
            let newPlayer = JSON.parse(payload.body)['newPlayerName']
            addPLayerToList(newPlayer)
        });
    });
}

function renderMainWindow() {
    let cookies = parseCookie()
    if (cookies['no-thanks-name'] === undefined || cookies['no-thanks-token'] === undefined) {
        authWindow()
    } else {
        mainWindow(cookies['no-thanks-name'])
    }
}

function renderLobby(isCreator, players) {
    lobbyBlock.show()
    mainBlock.hide()
    authBlock.hide()
    if (isCreator) {
        startGameButton.show()
    } else {
        startGameButton.hide()
    }
    playersList.html('')
    players.forEach(addPLayerToList)
}

function addPLayerToList(playerName) {
    let playerLi = $("<li/>")
    playerLi.append(document.createTextNode(playerName));
    playersList.append(playerLi);
}


function authWindow() {
    mainBlock.hide()
    infoBlock.hide()
    authBlock.show()
    lobbyBlock.hide()
}

function mainWindow(name) {
    mainBlock.show()
    infoBlock.show()
    authBlock.hide()
    lobbyBlock.hide()
    $('#player-name').html(name)
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