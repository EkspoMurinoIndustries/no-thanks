const { test, expect } = require('@playwright/test')

const applicationUrl = 'http://127.0.0.1:8080'

async function register(page, name) {
    await page.goto(applicationUrl)
    await expect(page.locator('#auth-screen')).toBeVisible()
    await page.locator('#regName').fill(name)
    await page.getByRole('button', { name: 'OK' }).click()
    await expect(page.getByRole('button', { name: 'Create Game' })).toBeVisible()
    await expect(page.locator('.player-greeting')).toHaveText(`Hello, ${name}.`)
}

test('two players keep and edit their names in the lobby and game', async ({ browser }) => {
    const creatorContext = await browser.newContext({
        viewport: { width: 1280, height: 800 }
    })
    const guestContext = await browser.newContext({
        viewport: { width: 360, height: 800 }
    })

    const creator = await creatorContext.newPage()
    const guest = await guestContext.newPage()

    try {
        await register(creator, 'CreatorCase')
        await creator.getByRole('button', { name: 'Create Game' }).click()
        const inviteCodeBlock = creator.locator('#invite-code')
        await expect(inviteCodeBlock).toHaveText(/^[A-Z]{5}$/)
        const inviteCode = await inviteCodeBlock.innerText()

        await register(guest, 'GuestCase')
        await guest.locator('#inviteCode').fill(inviteCode)
        await guest.getByRole('button', { name: 'Connect' }).click()

        await expect(creator.locator('#players-count')).toHaveText('2')
        await expect(guest.locator('#players-count')).toHaveText('2')

        const creatorLobbyName = creator.locator('#players-list .current-player .nickname')
        await expect(creatorLobbyName).toHaveText('CreatorCase')
        await creatorLobbyName.click()

        const creatorNameDialog = creator.locator('#new-name-block')
        const creatorNameInput = creatorNameDialog.locator('#newName')
        await expect(creatorNameInput).toHaveValue('CreatorCase')
        await creatorNameInput.fill('   ')
        await creatorNameInput.press('Enter')

        await expect(creator.locator('#error-message-text')).toHaveText('Name cannot be blank')
        await expect(creatorNameDialog).toBeVisible()
        await expect(creatorLobbyName).toHaveText('CreatorCase')

        await creator.locator('#error-message').getByRole('button', { name: 'OK' }).click()
        await creatorNameInput.fill('CreatorLobby')
        await creatorNameInput.press('Enter')

        await expect(creatorLobbyName).toHaveText('CreatorLobby')
        await expect(guest.locator('#players-list .nickname').filter({ hasText: 'CreatorLobby' })).toBeVisible()

        await creator.getByRole('button', { name: 'Start Game' }).click()
        await expect(creator.locator('#game-screen')).toBeVisible()
        await expect(guest.locator('#game-screen')).toBeVisible()
        await expect(creator.locator('#current-player-name')).toHaveText('CreatorLobby')
        await expect(guest.locator('#current-player-name')).toHaveText('GuestCase')

        await guest.locator('#current-player-name').click()
        const guestNameDialog = guest.locator('#new-name-block')
        const guestNameInput = guestNameDialog.locator('#newName')
        await expect(guestNameInput).toHaveValue('GuestCase')
        await guestNameInput.fill('GuestInGame')
        await guestNameInput.press('Enter')

        await expect(guest.locator('#current-player-name')).toHaveText('GuestInGame')
        await expect(creator.locator('#game-players-block .nickname')).toHaveText('GuestInGame')

        const guestPageWidth = await guest.evaluate(() => document.documentElement.scrollWidth)
        expect(guestPageWidth).toBeLessThanOrEqual(360)
    } finally {
        await creatorContext.close()
        await guestContext.close()
    }
})
