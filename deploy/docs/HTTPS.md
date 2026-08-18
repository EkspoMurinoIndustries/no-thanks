# NGINX and HTTPS

Install NGINX and Certbot on the server:

```bash
apt update
apt install nginx snapd
snap install --classic certbot
ln -s /snap/bin/certbot /usr/bin/certbot
```

Update the domain in `../nginx/no-thanks.conf`, then copy the supplied files:

```bash
scp deploy/nginx/default.conf deploy/nginx/no-thanks.conf root@<server-ip>:/etc/nginx/sites-available/
```

Enable them on the server:

```bash
rm -f /etc/nginx/sites-enabled/default
ln -s /etc/nginx/sites-available/default.conf /etc/nginx/sites-enabled/default.conf
ln -s /etc/nginx/sites-available/no-thanks.conf /etc/nginx/sites-enabled/no-thanks.conf
mkdir -p /etc/nginx/ssl
openssl req -nodes -new -x509 -subj '/CN=localhost' \
  -keyout /etc/nginx/ssl/default.key \
  -out /etc/nginx/ssl/default.crt
nginx -t
systemctl reload nginx
```

After DNS points to the server and the configured domain is correct:

```bash
certbot --nginx
```
