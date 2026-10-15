const { test, expect } = require('@playwright/test')

test('host lobby settings persist and configure the next round', async ({ browser }) => {
    const contexts = await Promise.all([browser.newContext(), browser.newContext()])
    const [host, guest] = await Promise.all(contexts.map(context => context.newPage()))
    try {
        for (const [page, name] of [[host, 'Host'], [guest, 'Guest']]) {
            await page.goto('/')
            await page.locator('#regName').fill(name)
            await page.getByRole('button', { name: 'OK', exact: true }).click()
            await expect(page.locator('#create-and-connect-game-screen')).toBeVisible()
        }
        await host.getByRole('button', { name: 'Create Game' }).click()
        await expect(host.locator('#invite-code')).toHaveText(/^[A-Z]{5}$/)
        await guest.locator('#inviteCode').fill(await host.locator('#invite-code').innerText())
        await guest.getByRole('button', { name: 'Connect', exact: true }).click()
        await expect(guest.locator('#lobby-screen')).toBeVisible()
        await expect(guest.locator('#settings-button')).toBeHidden()
        await host.locator('#settings-button').click()
        await expect(host.locator('#settings-min-card')).toHaveValue('3')
        await expect(host.locator('#settings-removed-cards')).toHaveValue('9')
        await expect(host.locator('#settings-default-tokens')).toBeChecked()
        for (const invalid of ['34', '56']) {
            await host.locator('#settings-max-card').fill(invalid)
            await host.getByRole('button', { name: 'Save settings' }).click()
            await expect(host.locator('#settings-menu')).toBeVisible()
        }
        await host.locator('#settings-max-card').fill('55')
        for (const invalid of ['0', '56']) {
            await host.locator('#settings-min-card').fill(invalid)
            await host.getByRole('button', { name: 'Save settings' }).click()
            await expect(host.locator('#settings-menu')).toBeVisible()
        }
        await host.locator('#settings-min-card').fill('5')
        await expect(host.locator('#settings-card-range')).toHaveText('Cards 5–55 (51 cards before removal).')
        await host.locator('#settings-removed-cards').fill('51')
        await host.getByRole('button', { name: 'Save settings' }).click()
        await expect(host.locator('#settings-menu')).toBeVisible()
        await host.locator('#settings-removed-cards').fill('2')
        await host.locator('#settings-tokens').fill('4')
        await host.getByRole('button', { name: 'Save settings' }).click()
        await expect(host.locator('#settings-menu')).toBeHidden()
        await host.locator('#settings-button').click()
        await expect(host.locator('#settings-tokens')).toHaveValue('4')
        await host.locator('#settings-tokens').fill('9')
        await host.getByRole('button', { name: 'Cancel', exact: true }).click()
        await host.locator('#start-game-button').click()
        for (const page of [host, guest]) {
            await expect(page.locator('#game-screen')).toBeVisible()
            await expect(page.locator('#settings-button')).toBeHidden()
            await expect(page.locator('#settings-menu')).toBeHidden()
            await expect(page.locator('#coins-count')).toHaveText('4')
        }
        host.once('dialog', dialog => dialog.accept())
        await host.locator('#abort-round-button').click()
        await host.locator('#settings-button').click()
        await expect(host.locator('#settings-max-card')).toHaveValue('55')
        await expect(host.locator('#settings-min-card')).toHaveValue('5')
        await expect(host.locator('#settings-removed-cards')).toHaveValue('2')
        await expect(host.locator('#settings-tokens')).toHaveValue('4')
        await expect(host.locator('#settings-default-tokens')).not.toBeChecked()
        await host.getByRole('button', { name: 'Reset to defaults' }).click()
        await expect(host.locator('#settings-menu')).toBeHidden()
        await host.locator('#settings-button').click()
        await expect(host.locator('#settings-max-card')).toHaveValue('35')
        await expect(host.locator('#settings-min-card')).toHaveValue('3')
        await expect(host.locator('#settings-removed-cards')).toHaveValue('9')
        await expect(host.locator('#settings-default-tokens')).toBeChecked()
        // Saving a card change must not turn the automatic token count into an override.
        await host.locator('#settings-max-card').fill('40')
        await host.getByRole('button', { name: 'Save settings' }).click()
        await expect(host.locator('#settings-menu')).toBeHidden()
        await host.locator('#start-game-button').click()
        for (const page of [host, guest]) {
            await expect(page.locator('#coins-count')).toHaveText('11')
        }
    } finally {
        await Promise.all(contexts.map(context => context.close()))
    }
})
