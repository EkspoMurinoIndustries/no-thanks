const { test, expect } = require('@playwright/test')

test('card artwork has consistent dimensions and readable fallbacks', async ({ page }) => {
    await page.goto('/')
    await page.evaluate(() => {
        $('#auth-screen, #create-and-connect-game-screen').hide()
        $('#game-screen').show()
    })
    for (const width of [394, 1280]) {
        await page.setViewportSize({ width, height: 900 })
        let expectedBox
        for (const number of [3, 35, 55]) {
            await page.evaluate(number => renderCurrentCard(number), number)
            const artwork = page.locator('#current-card img')
            await expect(artwork).toHaveAttribute('src', `img/cards/${number}.png`)
            await expect(artwork).toHaveAttribute('alt', `Card ${number}`)
            await expect.poll(() => artwork.evaluate(img => img.complete && img.naturalWidth > 0)).toBe(true)
            await expect(page.locator('.current-card-fallback')).toBeHidden()
            const box = await artwork.boundingBox()
            expect(box.width / box.height).toBeCloseTo(2 / 3)
            expect(box.x).toBeGreaterThanOrEqual(0)
            expect(box.x + box.width).toBeLessThanOrEqual(width)
            if (expectedBox) {
                expect(box.width).toBe(expectedBox.width)
                expect(box.height).toBe(expectedBox.height)
            }
            expectedBox = box
        }
    }
    for (const number of [1, 2]) {
        await page.evaluate(number => renderCurrentCard(number), number)
        await expect(page.locator('#current-card img')).toHaveCount(0)
        await expect(page.locator('.current-card-fallback')).toHaveText(String(number))
    }
    await page.route('**/img/cards/40.png', route => route.abort())
    await page.evaluate(() => renderCurrentCard(40))
    await expect(page.locator('#current-card img')).toHaveCount(0)
    await expect(page.locator('.current-card-fallback')).toBeVisible()
    await expect(page.locator('.current-card-fallback')).toHaveText('40')
})
