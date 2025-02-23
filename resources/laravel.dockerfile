# Stage 1: Build Stage
FROM composer:2 AS builder
WORKDIR /app

# Copy composer files and install dependencies without dev packages
COPY composer.json composer.lock ./
RUN composer install --no-dev --prefer-dist --no-scripts --no-interaction

# Copy the rest of the application code and optimize the autoloader
COPY . .
RUN composer dump-autoload --optimize

# Stage 2: Production Stage
FROM php:7.4-fpm-alpine

# Install system dependencies and PHP extensions required by Laravel
RUN apk add --no-cache \
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

# Ensure the www-data user owns the application files for security
RUN chown -R www-data:www-data /var/www

# Expose port 9000 (PHP-FPM)
EXPOSE 9000

# Start PHP-FPM
CMD ["php-fpm"]
