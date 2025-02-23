# Stage 1: Build Stage
FROM composer:2 AS builder
WORKDIR /app

# Ensure necessary files are available
COPY composer.json composer.lock ./
RUN composer install --no-dev --prefer-dist --no-scripts --no-interaction

# Copy application source
COPY . . 
RUN composer dump-autoload --optimize

# Stage 2: Production Stage
FROM php:8.2-fpm-alpine

# Install required dependencies and PHP extensions
RUN apk add --no-cache \
    bash \
    nginx \
    supervisor \
    zip unzip \
    libpng-dev \
    libjpeg-turbo-dev \
    freetype-dev \
    libzip-dev \
    oniguruma-dev \
    && docker-php-ext-configure gd --with-freetype --with-jpeg \
    && docker-php-ext-install pdo_mysql mbstring zip gd

# Set working directory
WORKDIR /var/www

# Copy optimized application files from the builder stage
COPY --from=builder /app /var/www

# Set permissions
RUN chown -R www-data:www-data /var/www

# Expose ports
EXPOSE 9000

# Run supervisord for process management
CMD ["php-fpm"]
