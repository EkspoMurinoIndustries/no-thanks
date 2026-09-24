let createAndConnectScreen = $('#create-and-connect-game-screen')

let authScreen = $('#auth-screen')

let lobbyScreen = $('#lobby-screen')
let lobbyPlayersList = $('#players-list')
let startGameButton = $('#start-game-button')

let gameScreen = $('#game-screen')
let gamePlayersList = $('#game-players-block')
let currentPlayerName = $('#current-player-name')
let currentPlayerCoins = $('#coins-count')
let currentPlayerCards = $('#player-cards')
let currentCardBlock = $('#current-card')
let currentCardCoinsBlock = $('#current-card-coins')
let putCoinButton = $('#put-coin')
let resetScoreButton = $('#reset-score-button')
let startNextRoundButton = $('#start-next-round-button')
let abortRoundButton = $('#abort-round-button')
let errorMessage =
    $('<div class="error-message" id="error-message">\n' +
    '    <div class="error-message--appeared">\n' +
    '        <div class="error-message-box">\n' +
    '            <div class="error-message-text"><img src="img/error_logo.png" alt=""><br><span id="error-message-text">Oops, mistake</span></div>\n' +
    '            <button class="button error-message-button" onclick="closeErrorMessage(); return false;">OK</button>\n' +
    '        </div>\n' +
    '    </div>\n' +
    '</div>')
let rules =
    $('<div class="error-message" id="rules">\n' +
        '    <div class="error-message--appeared">\n' +
        '        <div class="changename-message-box">\n' +
        '            <div class="rules-message-text"><br><p>' +
        '<h3 style="margin-bottom: 0.2em">Game objective</h3> ' +
        'To score the fewest points; players score points each time they collect a number and subtract points for each counter they hold at the end of the game.<br>' +
        '<h3 style="margin-bottom: 0.2em; margin-top: 0.2em">Game proccess</h3> ' +
        'Players make turns one after another. There is a common stack of numbers in front of players with a top number revealed. ' +
        'By default, the stack contains cards from 3 to 35, <b>with nine random cards removed each round.</b> ' +
        'The host can change the card range, removed card count, and initial tokens in the lobby Settings menu.<br>' +
        'During turn, a number is presented to a player. ' +
        'The player has two options:<br>' +
        '<b>1)</b> Take the card by pressing the <b>"Take Card"</b> button. In this case, the player is scored these penalty points. ' +
        'The number remains in player`s possesion until the end of the round and is public. Any counters collected on the number are also received. ' +
        'After taking the number, the next number in the stack is revealed and the player must make the same choice. <br>' +
        '<b>2)</b> Say <b>"No Thanks!"</b> so you don`t have to take the number by pressing the corresponding button. ' +
        'In this case, a counter must be put on the number and the turn continues to the next player. ' +
        'If you have no counters - the only choice is to take the number.<br>' +
        'The game ends when there are no numbers left in the stack. The host can end a round early; that round is discarded and everyone returns to the lobby.<br>' +
        '<b>Important note: If you take consecutive numbers, only the lowest number in the series will count against you. </b><br>' +
        'The number of counters you have is shown on the left of your collected numbers. ' +
        'The number of counters collected on the number is shown to the right-bottom of it, while the number of ' +
        'remaining numbers in the stack - to the right-top. </p></div>\n' +
        '            <button class="button error-message-button" onclick="closeRules(); return false;">OK</button>\n' +
        '        </div>\n' +
        '    </div>\n' +
        '</div>')
let changeNameBlock =
    $('<div class="error-message" id="new-name-block">\n' +
        '    <div class="error-message--appeared">\n' +
        '        <div class="changename-message-box">\n' +
        '            <form onsubmit="changeName(); return false;">\n' +
        '                <div class="error-message-text"><br><span>New name</span></div>\n' +
        '                <input class="input" type="text" id="newName" name="newName" maxlength="15" autofocus>' +
        '                <button class="button error-message-button" type="submit">OK</button>\n' +
        '            </form>\n' +
        '        </div>\n' +
        '    </div>\n' +
        '</div>')
let gameResultsBlock =
    $('<div class="error-message" id="game-results-block">\n' +
        '    <div class="error-message--appeared">\n' +
        '        <div class="results-message-box">\n' +
        '            <div class="result-block-header"><span class="results-title">Current Results</span></div>\n' +
        '            <div class="completed-rounds-label" id="completed-rounds-label"></div>\n' +
        '            <div class="scroll-wrapper"><table class="result-table" id="game-results-table"></table></div>\n' +
        '            <button class="button error-message-button" onclick="closeGameResults(); return false;">Close</button>\n' +
        '        </div>\n' +
        '    </div>\n' +
        '</div>')
let gameButtons = $('#game-buttons-block')

function isBlank(value) {
    return typeof value !== 'string' || value.trim() === ''
}

function renderAuthAndCreateConnectScreen() {
    let cookies = parseCookie()

    function validateInviteCode(URLInviteCode) {
        return URLInviteCode.length === 5
    }

    if (isBlank(cookies['no-thanks-name']) || cookies['no-thanks-token'] === undefined) {
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
    currentParams = params
    closeSettings()
    $('#settings-button').toggle(isCreator)
    $('#invite-code').html(lobbyInviteCode)
    lobbyScreen.show()
    createAndConnectScreen.hide()
    authScreen.hide()
    gameScreen.hide()
    if (isCreator) {
        startGameButton.show()
        resetScoreButton.show()
    } else {
        startGameButton.hide()
        resetScoreButton.hide()
    }
    lobbyPlayersList.html('')
    players.forEach(addPlayerToLobbyList)
    $('#players-count').html(players.length)
    $('#max-players-count').html(params.maxPlayerNumber)
    updateRoundDisplay()
}

function addPlayerToLobbyList(player) {
    $(`#lobby-player-li-${player.number}`).remove()
    let playerClass = player.number === myNumber ? "players-li current-player" : "players-li"
    let clickable = player.number === myNumber ? "onclick=\"renderChangeNameBlock(); return false;\"" : ""
    let playerNameClass = player.number === myNumber ? "nickname editable-player-name" : "nickname"
    lobbyPlayersList.append($(
        `<li class="${playerClass}" id="lobby-player-li-${player.number}">
            <span class="avatar-slot"></span>
            <span id="lobby-player-li-span-${player.number}" ${clickable} class="${playerNameClass}">${player.name}</span>
        </li>`))
    $(`#lobby-player-li-${player.number} .avatar-slot`).replaceWith(renderAvatar(player))
    if (player.disconnected) playerDisconnected(player)
}
function renderChangeNameBlock() {
    $('body').append(changeNameBlock);
    $('#newName').val(Cookies.get('no-thanks-name') || '').trigger('focus').select()
}

function renderRulesBlock() {
    $('body').append(rules);
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
    updateAvatar(player.number, player.avatar)
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
    $('#player-name').text(name)
}

function renderGameScreen(playersList, currentCard, activePlayerNumber, remainingNumberCard, currentCardCoin = 0) {
    closeSettings()
    createAndConnectScreen.hide()
    authScreen.hide()
    lobbyScreen.hide()
    gameScreen.show()
    $('#result-screen').hide()
    closeGameResults()
    updateRoundDisplay()
    abortRoundButton.toggle(amCreator)

    gamePlayersList.html('')
    let index = playersList.findIndex(player => player.number === myNumber)
    let rightPlyerList = playersList.slice(index).concat(playersList.slice(0, index))
    rightPlyerList.forEach(player => renderSingleGamePlayer(player, activePlayerNumber))
    renderCurrentCard(currentCard)
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

function renderCurrentCard(number) {
    currentCardBlock.empty().attr('aria-label', `Card ${number}`)
    const fallback = $('<span class="current-card-fallback">').text(number)
    currentCardBlock.append(fallback)
    // Custom decks may include 1 or 2, for which there is no artwork.
    if (number < 3 || number > 55) return
    const artwork = $('<img class="current-card-artwork" alt="Current card">')
        .attr({alt: `Card ${number}`, width: 244, height: 366})
        .on('load', () => fallback.hide())
        .on('error', function () { $(this).remove(); fallback.show() })
    artwork.attr('src', `img/cards/${number}.png`)
    currentCardBlock.append(artwork)
}

function renderSingleGamePlayer(player, activePlayerNumber) {
    if (player.number === myNumber) {
        $('#own-avatar').empty().append(renderAvatar(player))
        currentPlayerName.text(player.name)
        updatePersonalInfo(player.coins, player.cards, activePlayerNumber === myNumber)
    } else {
        let otherPlayerClass = activePlayerNumber === player.number ? "player-card turn" : "player-card"
        let otherPlayerCardsBlockId = `other-player-card-block-${player.number}`
        let otherPlayerBlockId = `other-player-block-${player.number}`
        let otherPlayerDiv = $(
            `<div class="${otherPlayerClass}" id="${otherPlayerBlockId}">
                <div class="nickname-player-card-block">
                    <span class="avatar-slot"></span>
                    <span class="nickname" id="game-player-name-${player.number}">${player.name}</span>
                </div>
                <div class="status-player-card-block">
                    <div class="cards-card-block" id="${otherPlayerCardsBlockId}">
                        ${renderCards(player.cards)}
                    </div>
                </div>
            </div>`)
        gamePlayersList.append(otherPlayerDiv)
        otherPlayerDiv.find('.avatar-slot').replaceWith(renderAvatar(player))
        if (player.disconnected) playerDisconnected(player)
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
            degree = `<span class="card-item-degree" style="color: ${getColor(value[1])}">-${value[1]}</span>`
        }
        return `<span class="card-item" style="color: ${getColor(value[0])}">${value[0]}${degree}</span>`
    }).join('')
}

function closeErrorMessage(){
    errorMessage.remove();
}

function closeRules(){
    rules.remove();
}

function closeGameResults(){
    gameResultsBlock.remove()
}

function renderGameResultsBlock() {
    $('body').append(gameResultsBlock)
    let completedRounds = Math.max(currentRound - 1, 0)
    $('#completed-rounds-label').text(completedRounds === 1 ? 'After 1 completed round' : `After ${completedRounds} completed rounds`)
    renderResultsTable($('#game-results-table'), currentResults)
}

function showErrorMessage(){
    $('body').append(errorMessage);
    let errorMessageText = $('#error-message-text');
    if (arguments.length === 0)
        errorMessageText.text("Oops, mistake");
    if (arguments.length === 1)
        errorMessageText.text(arguments[0]);
}

function renderEndRoundScreen(results, removedCards) {
    createAndConnectScreen.hide()
    authScreen.hide()
    lobbyScreen.hide()
    gameScreen.hide()
    $('#result-screen').show()
    startNextRoundButton.toggle(amCreator)
    updateRoundDisplay()
    renderResultsTable($('#result-table'), results)
    renderRemovedCards(removedCards)
}

function renderRemovedCards(removedCards) {
    let removedCardsList = $('#removed-cards-list').empty()
    let sortedRemovedCards = [...(removedCards || [])].sort((a, b) => a - b)
    sortedRemovedCards.forEach((card, index) => {
        if (index > 0) {
            removedCardsList.append(document.createTextNode(', '))
        }
        removedCardsList.append($('<span class="removed-card">').text(card).css('color', getColor(card)))
    })
}

function updateRoundAndResults(round, results) {
    currentRound = round || 0
    currentResults = results || {}
    updateRoundDisplay()
}

function updateRoundDisplay() {
    $('#lobby-round-number').text(currentRound + 1)
    $('#game-round-number').text(Math.max(currentRound, 1))
    $('#result-round-number').text(Math.max(currentRound, 1))
    startGameButton.text(currentRound === 0 ? 'Start Game' : 'Start Next Round')
    resetScoreButton.prop('disabled', !hasCompletedRounds(currentResults))
    renderResultsTable($('#lobby-results-table'), currentResults)
}

function updateResultsFromPlayers(players) {
    currentResults = Object.fromEntries(players.map(player => {
        let rounds = player.score || []
        return [player.number, {
            playerName: player.name,
            rounds: rounds,
            totalScore: rounds.reduce((total, score) => total + score, 0),
            lastRoundScore: rounds.length === 0 ? 0 : rounds[rounds.length - 1]
        }]
    }))
    updateRoundDisplay()
}

function removePlayerFromResults(playerNumber) {
    delete currentResults[playerNumber]
    updateRoundDisplay()
}

function hasCompletedRounds(results) {
    return Object.values(results || {}).some(result => result.rounds && result.rounds.length > 0)
}

function renderResultsTable(table, results) {
    table.empty()
    let sortedResults = Object.values(results || {}).sort((a, b) => a.totalScore - b.totalScore)
    if (sortedResults.length === 0 || !hasCompletedRounds(results)) {
        $('<tbody>').append(
            $('<tr class="empty-results-row">').append($('<td>').text('No completed rounds yet'))
        ).appendTo(table)
        return
    }

    let roundCount = Math.max(...sortedResults.map(result => result.rounds.length))
    let headerRow = $('<tr>').append($('<th>').text('Player'))
    for (let round = 1; round <= roundCount; round++) {
        headerRow.append($('<th>').text(`R${round}`))
    }
    headerRow.append($('<th>').text('Total'))
    $('<thead>').append(headerRow).appendTo(table)

    let tableBody = $('<tbody>')
    sortedResults.forEach(playerResult => {
        let row = $('<tr>').append($('<td>').text(playerResult.playerName))
        for (let roundIndex = 0; roundIndex < roundCount; roundIndex++) {
            let score = playerResult.rounds[roundIndex]
            row.append($('<td>').text(score === undefined ? '\u2014' : score))
        }
        row.append($('<td>').text(playerResult.totalScore))
        tableBody.append(row)
    })
    table.append(tableBody)
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
    if (number > 20) {
        // Ease through yellow and light reddish brown before the dark reference color.
        const stops = [
            [20, [0, 255, 0]],
            [25, [255, 235, 80]],
            [30, [205, 125, 95]],
            [35, [100, 24, 20]],
            [55, [60, 30, 12]]
        ]
        number = Math.min(number, 55)
        const endIndex = stops.findIndex(([value]) => value >= number)
        const [startValue, start] = stops[endIndex - 1]
        const [endValue, end] = stops[endIndex]
        const progress = (number - startValue) / (endValue - startValue)
        const [red, green, blue] = start.map((channel, index) =>
            Math.round(channel + (end[index] - channel) * progress))
        return `rgb(${red},${green},${blue})`
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

function renderNewName(message) {
    $(`#lobby-player-li-span-${message['playerNumber']}`).text(message['newName'])
    if (message['playerNumber'] === myNumber) {
        currentPlayerName.text(message['newName'])
    } else {
        $(`#game-player-name-${message['playerNumber']}`).text(message['newName'])
    }
    let playerResult = currentResults[message['playerNumber']]
    if (playerResult !== undefined) {
        playerResult.playerName = message['newName']
        updateRoundDisplay()
    }
}
