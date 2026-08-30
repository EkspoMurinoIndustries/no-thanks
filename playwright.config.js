const { defineConfig, devices } = require('@playwright/test')

const applicationUrl = 'http://127.0.0.1:8080'
const startApplication = process.platform === 'win32'
    ? '.\\gradlew.bat bootRun'
    : './gradlew bootRun'

module.exports = defineConfig({
    testDir: './e2e',
    fullyParallel: false,
    workers: 1,
    timeout: 30_000,
    expect: {
        timeout: 5_000
    },
    reporter: 'list',
    use: {
        baseURL: applicationUrl,
        screenshot: 'only-on-failure',
        trace: 'retain-on-failure'
    },
    projects: [
        {
            name: 'chromium',
            use: { ...devices['Desktop Chrome'] }
        }
    ],
    webServer: {
        command: startApplication,
        url: applicationUrl,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000
    }
})
