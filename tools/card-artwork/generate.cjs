const fs = require('node:fs/promises')
const path = require('node:path')
const sharp = require('sharp')

const root = path.resolve(__dirname, '../..')
const source = path.join(root, 'src/main/resources/static/img/cards')
const destination = path.join(root, 'build/generated/card-artwork/static/img/cards')

async function generate() {
    const files = (await fs.readdir(source)).filter(name => /^\d+\.png$/.test(name)).sort()
    if (files.length === 0) throw new Error('No card artwork found')
    await fs.mkdir(destination, { recursive: true })
    // Only remove generated WebP files, including variants of deleted source cards.
    for (const name of await fs.readdir(destination)) {
        if (/^\d+\.webp$/.test(name)) await fs.unlink(path.join(destination, name))
    }
    let sourceBytes = 0
    let outputBytes = 0
    for (const name of files) {
        const input = path.join(source, name)
        const output = path.join(destination, name.replace(/\.png$/, '.webp'))
        const result = await sharp(input)
            .resize({ width: 488, height: 732, fit: 'inside', withoutEnlargement: true })
            .webp({ quality: 85, effort: 4 })
            .toFile(output)
        sourceBytes += (await fs.stat(input)).size
        outputBytes += result.size
    }
    console.log(`${files.length} cards: ${(sourceBytes / 1e6).toFixed(2)} MB -> ${(outputBytes / 1e6).toFixed(2)} MB (${(100 * (1 - outputBytes / sourceBytes)).toFixed(1)}% smaller)`)
}

generate().catch(error => {
    console.error(error)
    process.exitCode = 1
})
