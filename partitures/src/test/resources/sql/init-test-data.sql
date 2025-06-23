-- Limpia y prepara datos iniciales
TRUNCATE TABLE sheets CASCADE;
TRUNCATE TABLE users CASCADE;

-- Inserta datos de prueba
INSERT INTO users(id, name, email, password, created_at, updated_at)
VALUES (1, 'Test User', 'test@example.com', 'password', NOW(), NOW());

INSERT INTO sheets(id, title, description, pdf_reference, is_public, owner_id, created_at, updated_at)
VALUES (1, 'Test Sheet', 'Description', 'test.pdf', true, 1, NOW(), NOW());