# ---- Etapa 1: build del sitio estático con Astro
FROM node:24-alpine AS build
WORKDIR /app

# Vacías = el navegador llama a /api/... en el mismo origen y nginx (API Gateway)
# deriva cada ruta al backend correspondiente.
ARG PUBLIC_MANAGEMENT_API_URL=""
ARG PUBLIC_AUDIT_CORE_API_URL=""
ENV PUBLIC_MANAGEMENT_API_URL=${PUBLIC_MANAGEMENT_API_URL} \
    PUBLIC_AUDIT_CORE_API_URL=${PUBLIC_AUDIT_CORE_API_URL}

COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- Etapa 2: nginx sirve el sitio y enruta /api/ a los servicios
FROM nginx:stable-alpine
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
