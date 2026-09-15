const { defineConfig, devices } = require('@playwright/test')
const path = require('path')

const applicationUrl = process.env.E2E_BASE_URL || 'http://127.0.0.1:8080'
const applicationPort = new URL(applicationUrl).port || '80'
const serverArgs = applicationPort === '8080' ? '' : ` --args=--server.port=${applicationPort}`
const projectRoot = path.resolve(__dirname, '..')
const startApplication = process.platform === 'win32'
    ? `.\\gradlew.bat bootRun${serverArgs}`
    : `./gradlew bootRun${serverArgs}`

module.exports = defineConfig({
    testDir: '.',
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
        cwd: projectRoot,
        url: applicationUrl,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000
    }
})
