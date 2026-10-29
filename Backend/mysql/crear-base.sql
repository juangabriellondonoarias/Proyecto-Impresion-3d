-- Ejecuta este script UNA vez en MySQL Workbench, conectado como root.
-- Crea la base de datos y un usuario propio para la aplicación
-- (mejor que usar root: solo tiene permisos sobre esta base).
--
-- Antes de ejecutarlo, cambia ESCRIBE_AQUI_TU_CONTRASEÑA por una contraseña tuya
-- y pon ese usuario y contraseña en Backend/.env (DB_USERNAME=costos3d, DB_PASSWORD=...).
-- Si prefieres usar root, no necesitas este script: basta con poner la contraseña de root en .env.

CREATE DATABASE IF NOT EXISTS costos3d
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'costos3d'@'localhost' IDENTIFIED BY 'ESCRIBE_AQUI_TU_CONTRASEÑA';

GRANT ALL PRIVILEGES ON costos3d.* TO 'costos3d'@'localhost';

FLUSH PRIVILEGES;

-- Las tablas (filament, product, quote, quote_item, settings) las crea la aplicación
-- la primera vez que arranca. No hace falta crearlas a mano.
