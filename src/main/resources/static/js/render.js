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
let errorMessage =
    $('<div class="error-message" id="error-message">\n' +
    '    <div class="error-message--appeared">\n' +
    '        <div class="error-message-box">\n' +
    '            <div class="error-message-text"><img src="img/error_logo.png" alt=""><br><span id="error-message-text">Oops, mistake</span></div>\n' +
    '            <button class="button error-message-button" onclick="closeErrorMessage(); return false;">OK</button>\n' +
    '        </div>\n' +
    '    </div>\n' +
    '</div>')
let gameButtons = $('#game-buttons-block')

function renderAuthAndCreateConnectScreen() {
    let cookies = parseCookie()

    function validateInviteCode(URLInviteCode) {
        return URLInviteCode.length === 5
    }

    if (cookies['no-thanks-name'] === undefined || cookies['no-thanks-token'] === undefined) {
        renderAuthScreen()
    } else {
        let URLInviteCode = location.pathname.substring(1);
        if (validateInviteCode(URLInviteCode)) {
            connectGame(URLInviteCode)
        } else {
            renderCreateAndConnectScreen(cookies['no-thanks-name'])
        }
    }
}

function renderLobbyScreen(isCreator, players, lobbyInviteCode, params) {
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
    $('#max-players-count').html(params.maxPlayerNumber)
}

function addPlayerToLobbyList(player) {
    let playerClass = player.number === myNumber ? "players-li current-player" : "players-li"
    lobbyPlayersList.append($(
        `<li class="${playerClass}" id="lobby-player-li-${player.number}">
            <div class="player-ava-block"></div>
            <span class="nickname\">${player.name}</span>
        </li>`))
}

function deletePlayerFromLobby(player, newNumber) {
    updatePlayerCountInLobby(newNumber)
    $(`#lobby-player-li-${player.number}`).remove()
}

function updatePlayerCountInLobby(newNumber) {
    $('#players-count').html(newNumber)
}


function playerDisconnected(player) {
    $(`#lobby-player-li-${player.number}`).addClass('disconnected-lobby-player')
    $(`#other-player-block-${player.number}`).addClass('disconnected-game-player')
}

function playerReconnected(player) {
    $(`#lobby-player-li-${player.number}`).removeClass('disconnected-lobby-player')
    $(`#other-player-block-${player.number}`).removeClass('disconnected-game-player')
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

function renderGameScreen(playersList, currentCard, activePlayerNumber, remainingNumberCard, currentCardCoin = 0) {
    createAndConnectScreen.hide()
    authScreen.hide()
    lobbyScreen.hide()
    gameScreen.show()
    $('#result-screen').hide()

    gamePlayersList.html('')
    let index = playersList.findIndex(player => player.number === myNumber)
    let rightPlyerList = playersList.slice(index).concat(playersList.slice(0, index))
    rightPlyerList.forEach(player => renderSingleGamePlayer(player, activePlayerNumber))
    currentCardBlock.html(currentCard)
    currentCardCoinsBlock.html(currentCardCoin)
    renderPlayButtons(activePlayerNumber === myNumber)
    updateRemainingNumberCards(remainingNumberCard)
}

function renderPlayButtons(isCurrent, enoughCoins) {
    if (isCurrent) {
        gameButtons.show()
        if (enoughCoins !== undefined) {
            putCoinButton.prop("disabled", !enoughCoins)
        }
    } else {
        gameButtons.hide()
    }
}

function renderSingleGamePlayer(player, activePlayerNumber) {
    if (player.number === myNumber) {
        updatePersonalInfo(player.coins, player.cards, activePlayerNumber === myNumber)
    } else {
        let otherPlayerClass = activePlayerNumber === player.number ? "player-card turn" : "player-card"
        let otherPlayerCardsBlockId = `other-player-card-block-${player.number}`
        let otherPlayerBlockId = `other-player-block-${player.number}`
        let otherPlayerDiv = $(
            `<div class="${otherPlayerClass}" id="${otherPlayerBlockId}">
                <div class="nickname-player-card-block">
                    <div class="player-ava-block"></div>
                    <span class="nickname">${player.name}</span>
                </div>
                <div class="status-player-card-block">
                    <div class="cards-card-block" id="${otherPlayerCardsBlockId}">
                        ${renderCards(player.cards)}
                    </div>
                </div>
            </div>`)
        gamePlayersList.append(otherPlayerDiv)
    }
}

function updatePersonalInfo(coins, cards, isCurrentPlayer) {
    currentPlayerCoins.html(coins)
    currentPlayerCards.html(renderCards(cards))
    renderPlayButtons(isCurrentPlayer, coins > 0)
}

function updateCardsForPlayer(playerNumber, cards) {
    $(`#other-player-card-block-${playerNumber}`).html(renderCards(cards))
}

function updateRemainingNumberCards(number) {
    $('#current-card-left-coins').html(number)
}

function renderCards(cards) {
    return groupCards(cards).map(value => {
        let degree = ''
        if (value.length > 1) {
            degree = `<span class="card-item-degree" style="color: ${getColor(value[1])}">.${value[1]}</span>`
        }
        return `<span class="card-item" style="color: ${getColor(value[0])}">${value[0]}${degree}</span>`
    }).join('')
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

function renderEndRoundScreen(results) {
    createAndConnectScreen.hide()
    authScreen.hide()
    lobbyScreen.hide()
    gameScreen.hide()
    $('#result-table').html('')
    $('#result-screen').show()
    Object.values(results).forEach(addResultRow)
}

function addResultRow(playerResultValue) {
    let roundScores = ""
    playerResultValue.rounds.forEach(roundScore => roundScores+=("<td>"+roundScore+"</td>"))
    $('#result-table').append("<tr>\n" +
        "                    <td>"+playerResultValue['playerName']+"</td>\n" +
                            roundScores +
        "                    <td>"+playerResultValue['totalScore']+"</td>\n" +
        "                </tr>")
}

function groupCards(cards) {
    if (cards === undefined || cards.length === 0) {
        return []
    }
    cards.sort(function(a, b) {
        return a - b;
    })
    let lastCard = cards[0]
    let response = []
    let dif = 1
    cards.slice(1).forEach(element => {
        if (element === lastCard + dif) {
            dif++
        } else {
            if (dif === 1) {
                response.push([lastCard])
            } else {
                response.push([lastCard, (lastCard + dif - 1)])
            }
            dif = 1
            lastCard = element
        }
    })
    if (dif > 1) {
        response.push([lastCard, (lastCard + dif - 1)])
    } else {
        response.push([cards[cards.length - 1]])
    }
    return response
}

function getColor(number) {
    if (number < 3) {
        number = 3
    }
    if (number > 35) {
        number = 35
    }
    let dif = (number - 3) * 15
    let blue = 255 - Math.min(dif, 255)
    let red = Math.max(0, dif - 255)
    return 'rgb(' + red + ',255,' + blue + ')'
}

function setCurrentTurnPlayer(newCurrentPlayerNumber) {
    $( ".player-card.turn").removeClass('turn')
    if (newCurrentPlayerNumber !== myNumber) {
        $(`#other-player-block-${newCurrentPlayerNumber}`).addClass('turn')
    }
}