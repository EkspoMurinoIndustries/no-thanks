let mainBlock = $('#main')
let authBlock = $('#auth')
let infoBlock = $('#player-info')
let lobbyBlock = $('#lobby')
let startGameButton = $('#start-game-button')
let playersList = $('#players-list');

renderMainWindow()

function auth() {
    const xhttp = new XMLHttpRequest();
    let playerName = $('#regName')[0].value
    if (playerName === undefined || playerName === '') {
        console.log("Empty name")
        return
    }
    xhttp.open("POST", "api/authorization", true);
    xhttp.setRequestHeader('Content-type', 'application/json; charset=UTF-8')
    xhttp.onload = function() {
        renderMainWindow()
    }
    xhttp.withCredentials = true
    let postObj = {
        name: playerName
    }
    xhttp.send(JSON.stringify(postObj));
}

function createGame() {
    const xhttp = new XMLHttpRequest();
    xhttp.open("POST", "api/game/create", true);
    xhttp.setRequestHeader('Content-type', 'application/json; charset=UTF-8')
    xhttp.onload = function() {
        console.log(xhttp.response)
        let inviteCode = JSON.parse(xhttp.response)['inviteCode']
        connectGameWithInviteCode(inviteCode)
    }
    xhttp.withCredentials = true
    let postObj = {}
    xhttp.send(JSON.stringify(postObj));
}

function connectGame() {
    let inviteCodeValue = $('#inviteCode').val()
    connectGameWithInviteCode(inviteCodeValue)
}

function connectGameWithInviteCode(inviteCodyValue) {
    const xhttp = new XMLHttpRequest();
    xhttp.open("POST", "api/game/connect", true);
    xhttp.setRequestHeader('Content-type', 'application/json; charset=UTF-8')
    xhttp.onload = function() {
        console.log(xhttp.response)
        let response = JSON.parse(xhttp.response)
        if (xhttp.status === 200 && response['status'] === 'SUCCESS') {
            let gameId = response['gameId']
            let isCreator = response['isCreator']
            let allPlayers = response['allPlayers']
            subscribe(gameId)
            renderLobby(isCreator, allPlayers)
        }
    }
    xhttp.withCredentials = true
    let postObj = {
        inviteCode: inviteCodyValue
    }
    xhttp.send(JSON.stringify(postObj));
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
    let playerLi = document.createElement('li');
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