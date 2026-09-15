const { test, expect } = require('@playwright/test')

const applicationUrl = process.env.E2E_BASE_URL || 'http://127.0.0.1:8080'

async function register(page, name) {
    await page.goto(applicationUrl)
    await page.locator('#regName').fill(name)
    await page.getByRole('button', { name: 'OK' }).click()
    await expect(page.getByRole('button', { name: 'Create Game' })).toBeVisible()
}

async function finishRound(pages) {
    for (let turn = 0; turn < 40; turn++) {
        if (await pages[0].locator('#result-screen').isVisible()) {
            break
        }

        const activePage = await pages[0].locator('#take-card').isVisible() ? pages[0] : pages[1]
        const cardsBefore = await activePage.locator('#current-card-left-coins').textContent()
        await activePage.locator('#take-card').click()
        await expect.poll(async () => {
            if (await activePage.locator('#result-screen').isVisible()) {
                return 'round-ended'
            }
            return activePage.locator('#current-card-left-coins').textContent()
        }).not.toBe(cardsBefore)
    }

    await expect(pages[0].locator('#result-screen')).toBeVisible()
    await expect(pages[1].locator('#result-screen')).toBeVisible()
}

test('round controls, score reset, next round, and in-game results stay in sync', async ({ browser }) => {
    test.setTimeout(60_000)
    const creatorContext = await browser.newContext()
    const guestContext = await browser.newContext()
    const creator = await creatorContext.newPage()
    const guest = await guestContext.newPage()

    try {
        await register(creator, 'RoundCreator')
        await creator.getByRole('button', { name: 'Create Game' }).click()
        const inviteCodeBlock = creator.locator('#invite-code')
        await expect(inviteCodeBlock).toHaveText(/^[A-Z]{5}$/)
        const inviteCode = await inviteCodeBlock.innerText()

        await register(guest, 'RoundGuest')
        await guest.locator('#inviteCode').fill(inviteCode)
        await guest.getByRole('button', { name: 'Connect' }).click()

        await expect(creator.locator('#lobby-round-number')).toHaveText('1')
        await expect(creator.locator('#reset-score-button')).toBeDisabled()
        const lobbyButtonFontSize = await creator.locator('#start-game-button').evaluate(element => getComputedStyle(element).fontSize)
        await expect(creator.locator('#reset-score-button')).toHaveCSS('font-size', lobbyButtonFontSize)
        await expect(guest.locator('#reset-score-button')).toBeHidden()
        await expect(creator.locator('#lobby-results-table')).toHaveText('No completed rounds yet')
        await expect(creator.locator('#removed-cards-block')).toBeHidden()

        await creator.locator('#start-game-button').click()
        await expect(creator.locator('#game-round-number')).toHaveText('1')
        await expect(guest.locator('#game-round-number')).toHaveText('1')
        await expect(creator.locator('#abort-round-button')).toBeVisible()
        await expect(guest.locator('#abort-round-button')).toBeHidden()
        await expect(creator.locator('#removed-cards-block')).toBeHidden()

        const activePage = await creator.locator('#take-card').isVisible() ? creator : guest
        const cardsBeforeAbort = await activePage.locator('#current-card-left-coins').textContent()
        await activePage.locator('#take-card').click()
        await expect(activePage.locator('#current-card-left-coins')).not.toHaveText(cardsBeforeAbort)

        creator.once('dialog', dialog => dialog.accept())
        await creator.locator('#abort-round-button').click()
        await expect(creator.locator('#lobby-screen')).toBeVisible()
        await expect(guest.locator('#lobby-screen')).toBeVisible()
        await expect(creator.locator('#lobby-round-number')).toHaveText('1')
        await expect(guest.locator('#lobby-round-number')).toHaveText('1')
        await expect(creator.locator('#lobby-results-table')).toHaveText('No completed rounds yet')
        await expect(creator.locator('#start-game-button')).toHaveText('Start Game')

        await creator.locator('#start-game-button').click()
        await expect(creator.locator('#game-round-number')).toHaveText('1')
        await expect(guest.locator('#game-round-number')).toHaveText('1')

        await creator.locator('#show-results-button').click()
        await expect(creator.locator('#completed-rounds-label')).toHaveText('After 0 completed rounds')
        await expect(creator.locator('#game-results-table')).toHaveText('No completed rounds yet')
        const popupTitleFontSize = await creator.locator('#game-results-block .results-title').evaluate(element => getComputedStyle(element).fontSize)
        const popupTableFontSize = await creator.locator('#game-results-table').evaluate(element => getComputedStyle(element).fontSize)
        await expect(creator.locator('#game-results-block .scroll-wrapper')).toHaveCSS('overflow', 'auto')
        await creator.getByRole('button', { name: 'Close' }).click()

        await finishRound([creator, guest])
        await expect(creator.locator('#result-round-number')).toHaveText('1')
        await expect(creator.locator('#result-table thead')).toHaveText(/PlayerR1Total/)
        await expect(creator.locator('#result-table tbody tr')).toHaveCount(2)
        await expect(creator.locator('#result-screen .results-title')).toHaveCSS('font-size', popupTitleFontSize)
        await expect(creator.locator('#result-table')).toHaveCSS('font-size', popupTableFontSize)
        await expect(creator.locator('#result-screen .scroll-wrapper')).toHaveCSS('overflow', 'auto')
        await expect(creator.locator('#removed-cards-block')).toBeVisible()
        const removedCards = (await creator.locator('#removed-cards-list .removed-card').allTextContents()).map(Number)
        expect(removedCards).toHaveLength(9)
        expect(removedCards).toEqual([...removedCards].sort((a, b) => a - b))
        expect(new Set(removedCards).size).toBe(9)
        await expect(creator.locator('#start-next-round-button')).toBeVisible()
        const resultButtonFontSize = await creator.locator('#start-next-round-button').evaluate(element => getComputedStyle(element).fontSize)
        await expect(creator.getByRole('button', { name: 'Return To Lobby' })).toHaveCSS('font-size', resultButtonFontSize)
        await expect(guest.locator('#start-next-round-button')).toBeHidden()

        await creator.getByRole('button', { name: 'Return To Lobby' }).click()
        await expect(creator.locator('#lobby-round-number')).toHaveText('2')
        await expect(creator.locator('#reset-score-button')).toBeEnabled()
        await expect(creator.locator('#lobby-results-table thead')).toHaveText(/PlayerR1Total/)
        await expect(creator.locator('#lobby-results-table tbody tr')).toHaveCount(2)

        creator.once('dialog', dialog => dialog.accept())
        await creator.locator('#reset-score-button').click()
        await expect(creator.locator('#lobby-round-number')).toHaveText('1')
        await expect(guest.locator('#lobby-round-number')).toHaveText('1')
        await expect(creator.locator('#reset-score-button')).toBeDisabled()
        await expect(creator.locator('#lobby-results-table')).toHaveText('No completed rounds yet')

        await creator.locator('#start-game-button').click()
        await expect(creator.locator('#game-round-number')).toHaveText('1')
        await finishRound([creator, guest])

        await creator.locator('#start-next-round-button').click()
        await expect(creator.locator('#game-screen')).toBeVisible()
        await expect(guest.locator('#game-screen')).toBeVisible()
        await expect(creator.locator('#game-round-number')).toHaveText('2')
        await expect(guest.locator('#game-round-number')).toHaveText('2')
        await expect(creator.locator('#removed-cards-block')).toBeHidden()

        await creator.locator('#show-results-button').click()
        await expect(creator.locator('#completed-rounds-label')).toHaveText('After 1 completed round')
        await expect(creator.locator('#game-results-table thead')).toHaveText(/PlayerR1Total/)
        await expect(creator.locator('#game-results-table tbody tr')).toHaveCount(2)
    } finally {
        await creatorContext.close()
        await guestContext.close()
    }
})
