const { test, expect } = require('@playwright/test')

async function register(page, name) {
    await page.goto('/')
    await page.locator('#regName').fill(name)
    await page.getByRole('button', { name: 'OK', exact: true }).click()
    await expect(page.locator('#create-and-connect-game-screen')).toBeVisible()
}

test('host and guest recover the same lobby and active round after a dropped connection', async ({ browser }) => {
    test.setTimeout(60_000)
    const contexts = await Promise.all([browser.newContext(), browser.newContext()])
    const [host, guest] = await Promise.all(contexts.map(context => context.newPage()))
    try {
        await register(host, 'ReconnectHost')
        await host.getByRole('button', { name: 'Create Game' }).click()
        await expect(host.locator('#invite-code')).toHaveText(/^[A-Z]{5}$/)
        const invite = await host.locator('#invite-code').innerText()
        await register(guest, 'ReconnectGuest')
        await guest.locator('#inviteCode').fill(invite)
        await guest.getByRole('button', { name: 'Connect', exact: true }).click()
        await expect(host.locator('#players-count')).toHaveText('2')
        await host.locator('#settings-button').click()
        await host.locator('#settings-max-card').fill('55')
        await host.getByRole('button', { name: 'Save settings' }).click()
        await expect(host.locator('#settings-menu')).toBeHidden()

        for (const index of [1, 0]) {
            const offline = index === 0 ? host : guest
            const observer = index === 0 ? guest : host
            await contexts[index].setOffline(true)
            await offline.evaluate(() => sock.close())
            await expect(observer.locator(`#lobby-player-li-${index}`)).toHaveClass(/disconnected-lobby-player/)
            await expect(observer.locator('#players-count')).toHaveText('2')
            await contexts[index].setOffline(false)
            await offline.evaluate(() => window.dispatchEvent(new Event('online')))
            await expect(offline.locator('#connection-status')).toBeHidden({ timeout: 15000 })
            await expect(observer.locator(`#lobby-player-li-${index}`)).not.toHaveClass(/disconnected-lobby-player/)
            await expect(offline.locator('#invite-code')).toHaveText(invite)
            await expect(offline.locator('#players-list > li')).toHaveCount(2)
        }
        await host.locator('#settings-button').click()
        await expect(host.locator('#settings-max-card')).toHaveValue('55')
        await host.getByRole('button', { name: 'Cancel', exact: true }).click()
        await host.locator('#start-game-button').click()
        await expect(host.locator('#game-screen')).toBeVisible()
        const card = await host.locator('#current-card img').getAttribute('src')
        for (let i = 0; i < 2; i++) {
            await contexts[i].setOffline(true)
            await [host, guest][i].evaluate(() => sock.close())
        }
        for (let i = 0; i < 2; i++) {
            await contexts[i].setOffline(false)
            await [host, guest][i].evaluate(() => window.dispatchEvent(new Event('online')))
        }
        for (const page of [host, guest]) {
            await expect(page.locator('#connection-status')).toBeHidden({ timeout: 15000 })
            await expect(page.locator('#game-screen')).toBeVisible()
            await expect(page.locator('#current-card img')).toHaveAttribute('src', card)
            await expect(page.locator('#game-round-number')).toHaveText('1')
        }
        // Simulate foregrounding a suspended tab whose socket still looks connected.
        await host.evaluate(() => {
            backgroundedAt = Date.now() - 2000
            document.dispatchEvent(new Event('visibilitychange'))
        })
        await expect(host.locator('#connection-status')).toBeHidden({ timeout: 15000 })
        await expect(host.locator('#current-card img')).toHaveAttribute('src', card)
    } finally {
        await Promise.all(contexts.map(context => context.close()))
    }
})

test('an expired or unknown invite returns to the join screen with an error', async ({ page }) => {
    await register(page, 'MissingLobby')
    await page.locator('#inviteCode').fill('XXXXX')
    await page.getByRole('button', { name: 'Connect', exact: true }).click()
    await expect(page.locator('#error-message-text')).toHaveText('Invite does not exist')
    await expect(page.locator('#connection-status')).toBeHidden()
    await expect(page.locator('#create-and-connect-game-screen')).toBeVisible()
    await expect(page).toHaveURL(/\/$/)
})
