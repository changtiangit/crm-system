# CRM Linux deployment

## 1. Frontend

Copy the **contents** of `Vue3 前端/dist` to `/opt/crm/frontend`. The file `/opt/crm/frontend/index.html` must exist.

## 2. Nginx

Install `nginx-crm.conf` as `/etc/nginx/sites-available/crm`, enable it, and disable the default site:

```bash
sudo cp nginx-crm.conf /etc/nginx/sites-available/crm
sudo ln -sfn /etc/nginx/sites-available/crm /etc/nginx/sites-enabled/crm
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

The API proxy intentionally has no trailing slash on `proxy_pass`. This preserves `/api` in the path expected by Spring Boot.

## 3. Backend

The application defaults to port 8080 and local MySQL `localhost:3306/crm`. If systemd exposes the backend on port 8000, set `SERVER_PORT=8000` (or configure the service accordingly). Set `DB_PASSWORD` to the actual MySQL password. Do not assume the Windows database client and server-local MySQL are the same database instance.

Check the backend service and its database connection:

```bash
sudo systemctl status crm-backend --no-pager
sudo journalctl -u crm-backend -n 100 --no-pager
sudo ss -lntp | grep -E ':8000|:8080'
```

## 4. End-to-end checks

```bash
curl -i http://127.0.0.1:8000/api/auth/health
curl -i http://127.0.0.1:8000/api/business/customers
curl -i http://127.0.0.1/api/auth/health
```

The customer list requires a valid bearer token, so 401 is expected without login. After creating a customer in the UI, query the MySQL instance used by the backend:

```sql
SELECT id, name, industry, level, contact, phone, owner, create_time
FROM crm.crm_customers
ORDER BY id DESC LIMIT 10;
```
