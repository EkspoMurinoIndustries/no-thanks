const { test, expect } = require('@playwright/test')

test('players crop their own avatar and share it across lobby game and reload', async ({ browser }, testInfo) => {
    test.setTimeout(60_000)
    const contexts = await Promise.all([browser.newContext(), browser.newContext()])
    const [host, guest] = await Promise.all(contexts.map(context => context.newPage()))
    try {
        for (const [page, name] of [[host, 'AvatarHost'], [guest, 'AvatarGuest']]) {
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
        await expect(guest.locator('#lobby-player-li-0 button.editable-avatar')).toHaveCount(0)

        const imageData = await host.evaluate(() => {
            const canvas = document.createElement('canvas')
            canvas.width = 400; canvas.height = 200
            const ctx = canvas.getContext('2d')
            ctx.fillStyle = 'red'; ctx.fillRect(0, 0, 200, 200)
            ctx.fillStyle = 'blue'; ctx.fillRect(200, 0, 200, 200)
            return canvas.toDataURL('image/png').split(',')[1]
        })
        const file = { name: 'avatar.png', mimeType: 'image/png', buffer: Buffer.from(imageData, 'base64') }
        const picker = host.waitForEvent('filechooser')
        await host.locator('#lobby-player-li-0 .editable-avatar').click()
        await (await picker).setFiles(file)
        await expect(host.locator('#avatar-editor')).toBeVisible()
        const preview = await host.locator('#avatar-preview').boundingBox()
        await host.mouse.move(preview.x + preview.width / 2, preview.y + preview.height / 2)
        await host.mouse.down()
        await host.mouse.move(preview.x + preview.width / 2 + 30, preview.y + preview.height / 2)
        await host.mouse.up()
        expect(Number(await host.locator('#avatar-x').inputValue())).toBeLessThan(50)
        await host.locator('#avatar-x').fill('0')
        await host.getByRole('button', { name: 'Save avatar' }).click()
        await expect(host.locator('#avatar-editor')).not.toBeVisible()
        await expect(guest.locator('#lobby-player-li-0 img')).toHaveAttribute('src', /^data:image\/jpeg;base64,/)
        const firstAvatar = await host.locator('#lobby-player-li-0 img').getAttribute('src')
        await expect(guest.locator('#lobby-player-li-0 img')).toHaveAttribute('src', firstAvatar)

        await host.locator('#start-game-button').click()
        await expect(host.locator('#own-avatar img')).toHaveAttribute('src', firstAvatar)
        await expect(guest.locator('#other-player-block-0 img')).toHaveAttribute('src', firstAvatar)
        await host.setViewportSize({ width: 394, height: 850 })
        const gamePicker = host.waitForEvent('filechooser')
        await host.locator('#own-avatar button').click()
        await (await gamePicker).setFiles(file)
        await expect(host.locator('#avatar-editor')).toBeVisible()
        await host.locator('#avatar-zoom').fill('2')
        await host.locator('#avatar-x').fill('100')
        await host.screenshot({ path: testInfo.outputPath('avatar-crop-mobile.png'), fullPage: true })
        await host.getByRole('button', { name: 'Save avatar' }).click()
        await expect(host.locator('#avatar-editor')).not.toBeVisible()
        await expect(host.locator('#own-avatar img')).not.toHaveAttribute('src', firstAvatar)
        const secondAvatar = await host.locator('#own-avatar img').getAttribute('src')
        await expect(guest.locator('#other-player-block-0 img')).toHaveAttribute('src', secondAvatar)
        await host.screenshot({ path: testInfo.outputPath('avatar-game-mobile.png'), fullPage: true })

        await host.reload()
        await expect(host.locator('#game-screen')).toBeVisible()
        await expect(host.locator('#own-avatar img')).toHaveAttribute('src', secondAvatar)
        await host.locator('#avatar-file').setInputFiles(file)
        await expect(host.locator('#avatar-editor')).toBeVisible()
        await host.getByRole('button', { name: 'Cancel', exact: true }).click()
        await expect(host.locator('#own-avatar img')).toHaveAttribute('src', secondAvatar)

        await host.locator('#avatar-file').setInputFiles({name:'bad.png', mimeType:'image/png', buffer:Buffer.from('not an image')})
        await expect(host.locator('#error-message-text')).toHaveText(/could not be opened/)
        await expect(host.locator('#own-avatar img')).toHaveAttribute('src', secondAvatar)
    } finally {
        await Promise.all(contexts.map(context => context.close()))
    }
})
