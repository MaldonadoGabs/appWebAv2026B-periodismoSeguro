-- HU-01: catalogo inicial de categorias. Se ejecuta en cada arranque (H2 y PostgreSQL)
-- y es idempotente: cada fila solo se inserta si su id no existe todavia.

INSERT INTO categorias_noticias (id, nombre, orden)
SELECT 'politica', 'Política', 1 WHERE NOT EXISTS
    (SELECT 1 FROM categorias_noticias WHERE id = 'politica');
INSERT INTO categorias_noticias (id, nombre, orden)
SELECT 'economia', 'Economía', 2 WHERE NOT EXISTS
    (SELECT 1 FROM categorias_noticias WHERE id = 'economia');
INSERT INTO categorias_noticias (id, nombre, orden)
SELECT 'derechos-humanos', 'Derechos Humanos', 3 WHERE NOT EXISTS
    (SELECT 1 FROM categorias_noticias WHERE id = 'derechos-humanos');
INSERT INTO categorias_noticias (id, nombre, orden)
SELECT 'sociedad', 'Sociedad', 4 WHERE NOT EXISTS
    (SELECT 1 FROM categorias_noticias WHERE id = 'sociedad');
INSERT INTO categorias_noticias (id, nombre, orden)
SELECT 'seguridad-justicia', 'Seguridad y Justicia', 5 WHERE NOT EXISTS
    (SELECT 1 FROM categorias_noticias WHERE id = 'seguridad-justicia');
INSERT INTO categorias_noticias (id, nombre, orden)
SELECT 'medio-ambiente', 'Medio Ambiente', 6 WHERE NOT EXISTS
    (SELECT 1 FROM categorias_noticias WHERE id = 'medio-ambiente');
INSERT INTO categorias_noticias (id, nombre, orden)
SELECT 'cultura', 'Cultura', 7 WHERE NOT EXISTS
    (SELECT 1 FROM categorias_noticias WHERE id = 'cultura');
INSERT INTO categorias_noticias (id, nombre, orden)
SELECT 'internacional', 'Internacional', 8 WHERE NOT EXISTS
    (SELECT 1 FROM categorias_noticias WHERE id = 'internacional');
INSERT INTO categorias_noticias (id, nombre, orden)
SELECT 'otros', 'Otros', 9 WHERE NOT EXISTS
    (SELECT 1 FROM categorias_noticias WHERE id = 'otros');

-- Las noticias creadas antes de esta HU no tenian categoria: ninguna queda sin clasificar.
UPDATE noticias SET categoria = 'otros' WHERE categoria IS NULL;
