const avatarEditor = $('#avatar-editor')
const avatarFileInput = $('#avatar-file')
const avatarErrorText = $('#avatar-error')
const avatarSaveButton = $('#avatar-save')
const avatarZoomInput = $('#avatar-zoom')
const avatarXInput = $('#avatar-x')
const avatarYInput = $('#avatar-y')
const avatarPreview = $('#avatar-preview')

let avatarImage = null
let avatarDrag = null
let avatarRequest = 0
let avatarOpener = null

function savedAvatar() {
    try { return localStorage.getItem('no-thanks-avatar') } catch (_) { return null }
}

function rememberAvatar(avatar) {
    try {
        if (avatar) localStorage.setItem('no-thanks-avatar', avatar)
        else localStorage.removeItem('no-thanks-avatar')
    } catch (_) { /* Avatars still work for this lobby if browser storage is unavailable. */ }
}

function renderAvatar(player) {
    const own = player.number === myNumber
    const avatar = $(own ? '<button type="button">' : '<div>')
        .addClass('player-ava-block').attr('data-avatar-player', player.number)
    if (own) avatar.addClass('editable-avatar').attr({'aria-label': 'Change avatar', title: 'Change avatar'})
        .on('click', chooseAvatar)
    if (player.avatar) avatar.append($('<img alt="Player avatar">').attr('src', player.avatar))
    return avatar
}

function updateAvatar(number, image) {
    const circles = $(`[data-avatar-player="${number}"]`).empty()
    if (image) circles.append($('<img alt="Player avatar">').attr('src', image))
}

function chooseAvatar() {
    if (!stompClient || !stompClient.connected || !activeGameId) return
    avatarOpener = document.activeElement
    avatarFileInput.val('').trigger('click')
}

function avatarError(message) {
    avatarErrorText.text(message)
    avatarSaveButton.prop('disabled', false)
}

function closeAvatarEditor() {
    avatarEditor[0].close()
}

avatarEditor.on('close', () => {
    avatarImage = null
    avatarDrag = null
    if (avatarOpener && avatarOpener.isConnected) avatarOpener.focus()
})

avatarFileInput.on('change', async function () {
    const file = this.files[0]
    if (!file) return
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 10 * 1024 * 1024) {
        showErrorMessage('Choose a JPEG, PNG, or WebP image smaller than 10 MB')
        return
    }
    const request = ++avatarRequest
    const url = URL.createObjectURL(file)
    try {
        const image = new Image()
        image.src = url
        await image.decode()
        if (request !== avatarRequest || !stompClient.connected) return
        avatarImage = image
        avatarZoomInput.val(1)
        avatarXInput.add(avatarYInput).val(50)
        avatarErrorText.text('')
        avatarSaveButton.prop('disabled', false)
        drawAvatar()
        avatarEditor[0].showModal()
    } catch (_) {
        showErrorMessage('That image could not be opened. Please choose another image.')
    } finally {
        URL.revokeObjectURL(url)
    }
})

function avatarCrop() {
    const side = Math.min(avatarImage.naturalWidth, avatarImage.naturalHeight) / Number(avatarZoomInput.val())
    return {
        side,
        x: (avatarImage.naturalWidth - side) * Number(avatarXInput.val()) / 100,
        y: (avatarImage.naturalHeight - side) * Number(avatarYInput.val()) / 100
    }
}

function drawAvatar(canvas = avatarPreview[0]) {
    if (!avatarImage) return
    const crop = avatarCrop()
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#e7e4ff'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(avatarImage, crop.x, crop.y, crop.side, crop.side, 0, 0, canvas.width, canvas.height)
}

avatarZoomInput.add(avatarXInput).add(avatarYInput).on('input', () => drawAvatar())
avatarPreview.on('pointerdown', function (event) {
    if (!avatarImage) return
    avatarDrag = {x: event.clientX, y: event.clientY, horizontal: Number(avatarXInput.val()), vertical: Number(avatarYInput.val())}
    this.setPointerCapture(event.pointerId)
}).on('pointermove', function (event) {
    if (!avatarDrag || !avatarImage) return
    const crop = avatarCrop()
    const scale = crop.side / this.getBoundingClientRect().width
    const clamp = value => Math.max(0, Math.min(100, value))
    if (avatarImage.naturalWidth > crop.side) avatarXInput.val(clamp(avatarDrag.horizontal - (event.clientX - avatarDrag.x) * scale * 100 / (avatarImage.naturalWidth - crop.side)))
    if (avatarImage.naturalHeight > crop.side) avatarYInput.val(clamp(avatarDrag.vertical - (event.clientY - avatarDrag.y) * scale * 100 / (avatarImage.naturalHeight - crop.side)))
    drawAvatar()
}).on('pointerup pointercancel lostpointercapture', () => { avatarDrag = null })

function saveAvatar() {
    if (!avatarImage) return
    if (!stompClient || !stompClient.connected) {
        avatarError('Reconnect to the game before saving your avatar.')
        return
    }
    const canvas = document.createElement('canvas')
    canvas.width = canvas.height = 96
    drawAvatar(canvas)
    let avatar
    for (const quality of [0.85, 0.7, 0.5, 0.3, 0.1]) {
        avatar = canvas.toDataURL('image/jpeg', quality)
        if (avatar.length <= 8023) break
    }
    if (avatar.length > 8023) {
        avatarError('Please choose a simpler image.')
        return
    }
    avatarSaveButton.prop('disabled', true)
    stompClient.send('/app/lobby/input/avatar', {}, JSON.stringify({avatar}))
}
