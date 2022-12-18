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
let errorMessage = $('<div class="error-message" id="error-message">\n' +
    '    <div class="error-message--appeared">\n' +
    '        <div class="error-message-box">\n' +
    '            <div class="error-message-text"><img src="img/error_logo.png" alt=""><br><span id="error-message-text">Oops, mistake</span></div>\n' +
    '            <button class="button" onclick="closeErrorMessage(); return false;">OK</button>\n' +
    '        </div>\n' +
    '    </div>\n' +
    '</div>')
let gameButtons = $('#game-buttons-block')

let playerNumbersToNames = new Map

function renderAuthAndCreateConnectScreen() {
    let cookies = parseCookie()
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
    $('#players-count').html(players.length)
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

function renderGameScreen(playersList, currentCard, activePlayerNumber) {
    createAndConnectScreen.hide()
    authScreen.hide()
    lobbyScreen.hide()
    gameScreen.show()

    gamePlayersList.html('')
    playersList.forEach(player => renderSingleGamePlayer(player, activePlayerNumber))

    currentCardBlock.html(currentCard)
    currentCardCoinsBlock.html('0')
    renderPlayButtons(activePlayerNumber)

}

function renderPlayButtons(currentPlayerNumber) {
    if (currentPlayerNumber === myNumber) {
        gameButtons.show()
    } else {
        gameButtons.hide()
    }
}

function renderSingleGamePlayer(player, activePlayerNumber) {
    let playerCards = player.cards.toString()
    if (player.number === myNumber) {
        currentPlayerCoins.html(player.coins)
        currentPlayerCards.html(playerCards)
    } else {
        let otherPlayerClass = activePlayerNumber === player.number ? "\"player-card turn\"" : "\"player-card\""
        let otherPlayerDiv = $("<div class="+otherPlayerClass+">\n" +
            "                 <div class=\"nickname-player-card-block\">\n" +
            "                     <div class=\"player-ava-block\"></div>\n" +
            "                     <span class=\"nickname\">"+player.name+"</span>\n" +
            "                 </div>\n" +
            "                 <div class=\"status-player-card-block\">\n" +
            "                     <div class=\"cards-card-block\"><span>"+playerCards+"</span></div>\n" +
            "                 </div>\n" +
            "             </div>")
        gamePlayersList.append(otherPlayerDiv)
    }
}

function updatePersonalInfo(coins, cards, isCurrentPlayer) {
    currentPlayerCoins.html(coins)
    currentPlayerCards.html(cards.toString())
    if (isCurrentPlayer) {
        gameButtons.show()
    } else {
        gameButtons.hide()
    }
}

function closeErrorMessage(){
    errorMessage.remove();
}

function showErrorMessage(){
    $('body').append(errorMessage);
    let errorMessageText = $('#error-message-text');
    if (arguments.length === 0)
        errorMessageText.text("Oops, mistake");
    if (arguments.length === 1)
        errorMessageText.text(arguments[0]);
}

