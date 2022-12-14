let createAndConnectScreen = $('#create-and-connect-game-screen')

let authScreen = $('#auth-screen')

let lobbyScreen = $('#lobby-screen')
let lobbyPlayersList = $('#players-list')
let startGameButton = $('#start-game-button')

let gameScreen = $('#game-screen')
let gamePlayersList = $('#game-players-block')
let currentPlayerCoins = $('#coins-count')
let currentPlayerCards = $('#player-cards')
let currentCardBlock = $('#current-card')
let currentCardCoinsBlock = $('#current-card-coins')
let putCoinButton = $('#put-coin')
let takeCardButton = $('#take-card')
let errorMessage = $('#error-message')

let playerNumbersToNames = new Map

function renderAuthAndCreateConnectScreen() {
    let cookies = parseCookie()
    errorMessage.hide();
    if (cookies['no-thanks-name'] === undefined || cookies['no-thanks-token'] === undefined) {
        renderAuthScreen()
    } else {
        renderCreateAndConnectScreen(cookies['no-thanks-name'])
    }
}

function renderLobbyScreen(isCreator, players, lobbyInviteCode) {
    $('#invite-code').html(lobbyInviteCode)
    lobbyScreen.show()
    createAndConnectScreen.hide()
    authScreen.hide()
    gameScreen.hide()
    if (isCreator) {
        startGameButton.show()
    } else {
        startGameButton.hide()
    }
    lobbyPlayersList.html('')
    players.forEach(addPlayerToLobbyList)
}

function addPlayerToLobbyList(player) {
    let playerClass = player.number === myNumber ? "\"players-li current-player\"" : "\"players-li\""
    lobbyPlayersList.append($("<li class="+playerClass+">\n" +
        "<div class=\"player-ava-block\"></div>\n" +
        "<span class=\"nickname\">"+player.name+"</span>\n" +
        "</li>"))
}


function renderAuthScreen() {
    createAndConnectScreen.hide()
    authScreen.show()
    lobbyScreen.hide()
    gameScreen.hide()
}

function renderCreateAndConnectScreen(name) {
    createAndConnectScreen.show()
    authScreen.hide()
    lobbyScreen.hide()
    gameScreen.hide()
    $('#player-name').html(name)
}

function renderGameScreen(playersList, currentCard, currentPlayerNumber) {
    createAndConnectScreen.hide()
    authScreen.hide()
    lobbyScreen.hide()
    gameScreen.show()

    gamePlayersList.html('')
    playersList.forEach(renderSingleGamePlayer)

    currentCardBlock.html(currentCard)
    currentCardCoinsBlock.html('0')
    renderPlayButtons(currentPlayerNumber)

}

function renderPlayButtons(currentPlayerNumber) {
    if (currentPlayerNumber === myNumber) {
        putCoinButton.show()
        takeCardButton.show()
    } else {
        putCoinButton.hide()
        takeCardButton.hide()
    }
}

function renderSingleGamePlayer(item) {
    let playerClass = item.number === myNumber ? "\"player-card turn\"" : "\"player-card\""
    let playerCards = item.cards.toString()
    let playerLi = $("<div class="+playerClass+">\n" +
        "                    <div class=\"nickname-player-card-block\">\n" +
        "                        <div class=\"player-ava-block\"></div>\n" +
        "                        <span class=\"nickname\">"+item.name+"</span>\n" +
        "                    </div>\n" +
        "                    <div class=\"status-player-card-block\">\n" +
        "                        <div class=\"cards-card-block\"><span>"+playerCards+"</span></div>\n" +
        "                    </div>\n" +
        "                </div>")
    gamePlayersList.append(playerLi);
    if (item.number === myNumber) {
        currentPlayerCoins.html(item.coins)
        currentPlayerCards.html(playerCards)
    }
}
function closeErrorMessage(){
    errorMessage.hide();
}
function showErrorMessage(){
    errorMessage.show();
}