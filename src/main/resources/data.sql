-- HU-16: catalogo inicial de categorias.
-- Se ejecuta en cada arranque (spring.sql.init.mode=always), por eso cada INSERT es idempotente:
-- INSERT ... SELECT ... WHERE NOT EXISTS funciona igual en H2 y en PostgreSQL.
-- defer-datasource-initialization=true garantiza que Hibernate ya creo/actualizo las tablas.

INSERT INTO categorias (slug, nombre, orden)
SELECT 'politica', 'Política', 1
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE slug = 'politica');

INSERT INTO categorias (slug, nombre, orden)
SELECT 'economia', 'Economía', 2
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE slug = 'economia');

INSERT INTO categorias (slug, nombre, orden)
SELECT 'derechos-humanos', 'Derechos Humanos', 3
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE slug = 'derechos-humanos');

INSERT INTO categorias (slug, nombre, orden)
SELECT 'sociedad', 'Sociedad', 4
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE slug = 'sociedad');

INSERT INTO categorias (slug, nombre, orden)
SELECT 'seguridad-justicia', 'Seguridad y Justicia', 5
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE slug = 'seguridad-justicia');

INSERT INTO categorias (slug, nombre, orden)
SELECT 'medio-ambiente', 'Medio Ambiente', 6
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE slug = 'medio-ambiente');

INSERT INTO categorias (slug, nombre, orden)
SELECT 'cultura', 'Cultura', 7
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE slug = 'cultura');

INSERT INTO categorias (slug, nombre, orden)
SELECT 'internacional', 'Internacional', 8
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE slug = 'internacional');

INSERT INTO categorias (slug, nombre, orden)
SELECT 'otros', 'Otros', 9
WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE slug = 'otros');

-- Las noticias creadas antes de HU-16 no tienen categoria; todas pasan a "Otros".
-- Desde esta historia, crear una noticia exige una categoria del catalogo.
UPDATE noticias SET categoria = 'otros' WHERE categoria IS NULL;
